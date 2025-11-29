# QR Code Sharing Implementation Plan

## Overview
Add QR code scanning/sharing for both collection folders and decks, allowing users to:
- Export a collection folder as a get-diced.com link with QR code
- Export a deck as a get-diced.com link with QR code (preserving deck structure)
- Scan QR codes to import collections or decks into the app

## Backend API Changes (COMPLETED ✅)

### Files Modified:

1. **`/home/brandon/data/srg_card_search_website/backend/app/models/base.py`**
   - Added `SharedListType` enum (COLLECTION, DECK)
   - Added `list_type` column to `SharedList` model
   - Added `deck_data` JSON column to store deck structure
   - Imported `JSON` from sqlalchemy

2. **`/home/brandon/data/srg_card_search_website/backend/app/schemas/shared_list_schema.py`**
   - Added `SharedListType` enum
   - Added `DeckSlotData` schema for individual deck slots
   - Added `DeckData` schema for deck structure (spectacle_type, slots)
   - Updated `SharedListCreate` to include `list_type` and `deck_data`
   - Updated `SharedListResponse` to return new fields

3. **`/home/brandon/data/srg_card_search_website/backend/app/routers/shared_lists.py`**
   - Updated `create_shared_list` to validate and store deck_data
   - Added validation that deck_data is required when list_type is DECK

4. **`/home/brandon/data/srg_card_search_website/backend/app/backup_shared_lists.py`**
   - Updated backup to include `list_type` and `deck_data`
   - Updated restore to handle new fields with backward compatibility

### Database Schema Changes:

The `shared_lists` table now includes:
```sql
- id: STRING (primary key, UUID)
- name: STRING(255) (optional)
- description: TEXT (optional)
- card_uuids: ARRAY(STRING) (required)
- list_type: ENUM('COLLECTION', 'DECK') (default: 'COLLECTION')
- deck_data: JSON (optional, required when list_type='DECK')
- created_at: TIMESTAMP (auto-generated)
```

### Deck Data JSON Structure:

When `list_type = 'DECK'`, the `deck_data` field contains:
```json
{
  "spectacle_type": "NEWMAN" | "VALIANT",
  "slots": [
    {
      "slot_type": "ENTRANCE" | "COMPETITOR" | "DECK" | "FINISH" | "ALTERNATE",
      "slot_number": 0-30,
      "card_uuid": "uuid-string"
    },
    ...
  ]
}
```

### API Endpoints (existing, now enhanced):

**POST /api/shared-lists**
- Request body:
  ```json
  {
    "name": "My Deck Name",
    "description": "Optional description",
    "card_uuids": ["uuid1", "uuid2", ...],
    "list_type": "DECK" | "COLLECTION",
    "deck_data": {
      "spectacle_type": "NEWMAN",
      "slots": [...]
    }
  }
  ```
- Response:
  ```json
  {
    "id": "generated-uuid",
    "url": "/create-list?shared=generated-uuid",
    "message": "Shareable list created successfully"
  }
  ```

**GET /api/shared-lists/{shared_id}**
- Response includes all fields including `list_type` and `deck_data`

### Deployment Steps for Backend:

1. **On development machine:**
   ```bash
   cd ~/data/srg_card_search_website/helpers
   ./workflow.sh
   ```
   This will:
   - Backup existing shared lists
   - Recreate database with new schema
   - Restore shared lists (as COLLECTION type)
   - Load cards from YAML
   - Generate mobile DB and manifests

2. **Deploy to server:**
   ```bash
   # Copy updated files to server
   rsync -avz ~/data/srg_card_search_website/backend/app/ \
     dondo@get-diced.com:srg_card_search_website/backend/app/

   # SSH to server and run workflow
   ssh dondo@get-diced.com
   cd srg_card_search_website/helpers
   ./workflow.sh

   # Restart FastAPI service
   sudo systemctl restart srg_backend  # (or whatever the service name is)
   ```

3. **No nginx changes needed** - The API routes are already proxied correctly

---

## Android App Changes (TODO)

### 1. Add Dependencies (`app/build.gradle.kts`)

```kotlin
dependencies {
    // ... existing dependencies ...

    // QR Code generation
    implementation("com.google.zxing:core:3.5.2")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // Camera permissions handled by zxing-android-embedded
}
```

### 2. Add Camera Permission (`AndroidManifest.xml`)

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="false" />
```

### 3. New Screens/Components to Create:

#### A. QRCodeScanScreen.kt
- Top-level menu item in bottom navigation
- Camera-based QR scanner using ZXing
- Scan QR codes containing get-diced.com shared list URLs
- Parse URL to extract shared ID
- Fetch shared list from API
- Determine if it's a COLLECTION or DECK
- Show import dialog with name input

#### B. QRCodeDisplayDialog.kt
- Composable dialog that displays a QR code
- Takes a URL as input
- Generates QR code bitmap using ZXing
- Shows the QR code and the URL text
- Copy URL button

#### C. Enhanced DeckEditorScreen.kt
- Replace current "Share" button with "Share as QR Code"
- Generate proper deck_data structure:
  ```kotlin
  {
    "spectacle_type": "NEWMAN",
    "slots": [
      {"slot_type": "ENTRANCE", "slot_number": 0, "card_uuid": "..."},
      ...
    ]
  }
  ```
- POST to `/api/shared-lists` with `list_type: "DECK"`
- Show QRCodeDisplayDialog with the returned URL

#### D. Enhanced FolderDetailScreen.kt
- Add "Share as QR Code" button
- POST to `/api/shared-lists` with `list_type: "COLLECTION"`
- Include all card UUIDs from folder
- Show QRCodeDisplayDialog

### 4. Import Flow Changes:

#### DeckImportDialog.kt (new)
- Shows when scanning a DECK type shared list
- Lets user name the deck
- Select which deck folder to import into
- Parse deck_data and recreate deck structure:
  - Create new Deck with name and spectacle_type
  - Create DeckCard entries for each slot
  - Navigate to deck editor

#### CollectionImportDialog.kt (new)
- Shows when scanning a COLLECTION type shared list
- Lets user name the folder (or select existing folder)
- Add cards to folder with default quantity

### 5. Navigation Updates:

Add "Scan QR" as 5th tab in bottom navigation:
- Collection
- Decks
- Viewer
- Settings
- **Scan QR** (new)

### 6. Data Layer Updates:

No database changes needed - existing Deck and DeckCard tables support this.

Need to add to `GetDicedApi.kt`:
```kotlin
// Enhanced response model
data class SharedListResponse(
    val id: String,
    val name: String?,
    val description: String?,
    val card_uuids: List<String>,
    val list_type: String,
    val deck_data: DeckDataResponse?
)

data class DeckDataResponse(
    val spectacle_type: String,
    val slots: List<DeckSlotDataResponse>
)

data class DeckSlotDataResponse(
    val slot_type: String,
    val slot_number: Int,
    val card_uuid: String
)

// Enhanced create request
data class SharedListCreateRequest(
    val name: String?,
    val description: String?,
    val card_uuids: List<String>,
    val list_type: String,
    val deck_data: DeckDataRequest?
)
```

---

## Implementation Order

1. ✅ Backend API enhancement (COMPLETED)
2. ⏳ Deploy backend changes to server
3. ⏳ Add ZXing dependencies to Android app
4. ⏳ Create QRCodeDisplayDialog component
5. ⏳ Update DeckEditorScreen to share with deck structure
6. ⏳ Update FolderDetailScreen to share collection
7. ⏳ Create QRCodeScanScreen
8. ⏳ Create import dialogs (Deck and Collection)
9. ⏳ Add "Scan QR" to bottom navigation
10. ⏳ Test end-to-end flow

---

## Testing Checklist

- [ ] Deploy backend changes successfully
- [ ] Create a DECK type shared list via API
- [ ] Create a COLLECTION type shared list via API
- [ ] Verify deck_data structure is saved correctly
- [ ] Share a deck from app, verify QR code appears
- [ ] Share a collection from app, verify QR code appears
- [ ] Scan a deck QR code, verify deck structure is preserved
- [ ] Scan a collection QR code, verify cards import correctly
- [ ] Test with existing shared lists (should be type COLLECTION)

---

## Notes

- Existing shared lists will be migrated as COLLECTION type (backward compatible)
- QR codes will encode the full URL: `https://get-diced.com/create-list?shared={id}`
- The mobile app can also accept just the ID if scanning manually typed codes
- Deck sharing preserves: spectacle type, entrance, competitor, deck slots 1-30, finishes, alternates
- Collection sharing is a simple list of cards (like current implementation)

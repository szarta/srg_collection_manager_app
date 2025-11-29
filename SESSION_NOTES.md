# Session Notes - Nov 28, 2025

## What Was Completed This Session ✅

### QR Code Sharing - Backend API Enhancement (COMPLETED)

**Goal:** Enable QR code sharing for both collection folders and decks, preserving deck structure (slots, spectacle type)

**Backend Changes (COMPLETED):**

1. **Enhanced `SharedList` Model** (`models/base.py`)
   - Added `SharedListType` enum (COLLECTION, DECK)
   - Added `list_type` column (defaults to COLLECTION for backward compatibility)
   - Added `deck_data` JSON column to store deck structure
   - Imported JSON from sqlalchemy

2. **Enhanced Schemas** (`schemas/shared_list_schema.py`)
   - Added `SharedListType` enum
   - Added `DeckSlotData` schema for individual deck slots:
     - `slot_type`: ENTRANCE, COMPETITOR, DECK, FINISH, ALTERNATE
     - `slot_number`: 0 for single slots, 1-30 for deck cards
     - `card_uuid`: card identifier
   - Added `DeckData` schema for complete deck structure:
     - `spectacle_type`: NEWMAN or VALIANT
     - `slots`: List of DeckSlotData
   - Updated `SharedListCreate` request schema
   - Updated `SharedListResponse` schema

3. **Enhanced API Router** (`routers/shared_lists.py`)
   - Updated `create_shared_list` endpoint
   - Added validation for deck_data when list_type is DECK
   - Converts Pydantic deck_data to dict for JSON storage
   - Backward compatible with existing COLLECTION lists

4. **Updated Backup/Restore** (`backup_shared_lists.py`)
   - Backup now includes `list_type` and `deck_data` fields
   - Restore handles new fields with backward compatibility
   - Existing shared lists will be restored as COLLECTION type

**Database Schema Changes:**

```sql
shared_lists table:
- id: STRING (primary key, UUID)
- name: STRING(255) (optional)
- description: TEXT (optional)
- card_uuids: ARRAY(STRING) (required)
- list_type: ENUM('COLLECTION', 'DECK') [NEW] (default: 'COLLECTION')
- deck_data: JSON [NEW] (optional, required when list_type='DECK')
- created_at: TIMESTAMP (auto-generated)
```

**Deck Data JSON Structure:**
```json
{
  "spectacle_type": "NEWMAN" | "VALIANT",
  "slots": [
    {
      "slot_type": "ENTRANCE" | "COMPETITOR" | "DECK" | "FINISH" | "ALTERNATE",
      "slot_number": 0-30,
      "card_uuid": "uuid-string"
    }
  ]
}
```

**API Endpoints (Enhanced):**

- **POST /api/shared-lists** - Now accepts `list_type` and `deck_data`
- **GET /api/shared-lists/{id}** - Now returns `list_type` and `deck_data`
- **DELETE /api/shared-lists/{id}** - Unchanged
- **GET /api/shared-lists** - List endpoint unchanged

**Files Modified:**
- `/home/brandon/data/srg_card_search_website/backend/app/models/base.py`
- `/home/brandon/data/srg_card_search_website/backend/app/schemas/shared_list_schema.py`
- `/home/brandon/data/srg_card_search_website/backend/app/routers/shared_lists.py`
- `/home/brandon/data/srg_card_search_website/backend/app/backup_shared_lists.py`

**Files Created:**
- `/home/brandon/data/srg_collection_manager_app/QR_CODE_IMPLEMENTATION.md` - Complete implementation plan
- `/home/brandon/data/srg_collection_manager_app/srg.conf` - Pulled down nginx config (no changes needed)

**Deployment Steps:**

1. **On dev machine:**
   ```bash
   cd ~/data/srg_card_search_website/helpers
   ./workflow.sh  # Backs up, recreates DB, restores, loads cards
   ```

2. **Deploy to server:**
   ```bash
   # Sync files
   rsync -avz ~/data/srg_card_search_website/backend/app/ \
     dondo@get-diced.com:srg_card_search_website/backend/app/

   # SSH and run workflow
   ssh dondo@get-diced.com
   cd srg_card_search_website/helpers
   ./workflow.sh

   # Restart FastAPI service
   sudo systemctl restart srg_backend
   ```

**No nginx changes needed** - `/api/` routes already proxied correctly

---

### Android App - QR Code Export (COMPLETED)

**Goal:** Enable QR code sharing for collection folders and decks from the Android app

**Phase 1: Collection Folder QR Export (COMPLETED)**

1. **Added Dependencies** (`app/build.gradle.kts`)
   - ZXing QR generation: `com.google.zxing:core:3.5.2`

2. **Created Components:**
   - `QRCodeDialog.kt` - Reusable dialog displaying QR code with clickable URL
     - Uses ZXing to generate QR code bitmap
     - Displays shareable URL (underlined, blue, clickable)
     - Opens URL in browser on tap

3. **Enhanced API Models** (`ApiModels.kt`)
   - Updated `SharedListRequest` with `listType` and `deckData` fields
   - Updated `SharedListResponse` to include new fields
   - Added `DeckData` and `DeckSlot` models

4. **Enhanced CollectionViewModel** (`CollectionViewModel.kt`)
   - Added share state: `_shareUrl`, `_isSharing`
   - Added `shareFolderAsQRCode()` function:
     - Fetches all cards in folder
     - Creates `SharedListRequest` with `listType="COLLECTION"`
     - Calls API to generate shareable link
     - Displays QR code dialog

5. **Enhanced FolderDetailScreen** (`FolderDetailScreen.kt`)
   - Added QR code share button (📱) to TopAppBar
   - Shows loading indicator during share
   - Displays QR code dialog with shareable URL

**Phase 2: Deck QR Export with Full Structure (COMPLETED)**

1. **Enhanced DeckViewModel** (`DecksScreen.kt`)
   - Added share state: `_shareUrl`, `_isSharing`
   - Added `shareDeckAsQRCode()` function:
     - Fetches all cards in deck with slot information
     - Builds complete deck structure with:
       - `spectacle_type` (NEWMAN/VALIANT)
       - `slots` array with slot types and numbers:
         - ENTRANCE (slot 0)
         - COMPETITOR (slot 0)
         - DECK (slots 1-30)
         - FINISH
         - ALTERNATE
     - Creates `SharedListRequest` with `listType="DECK"` and full `deckData`
     - Calls API to generate shareable link

2. **Enhanced DeckEditorScreen** (`DeckEditorScreen.kt`)
   - Replaced old Share button with QR code button (📱)
   - Shows loading indicator during share
   - Displays QR code dialog with shareable URL
   - CSV export already functional (Download icon ⬇️)

**Files Created:**
- `app/src/main/kotlin/com/srg/inventory/ui/QRCodeDialog.kt`

**Files Modified:**
- `app/build.gradle.kts` - Added ZXing dependency
- `app/src/main/kotlin/com/srg/inventory/api/ApiModels.kt` - Enhanced shared list models
- `app/src/main/kotlin/com/srg/inventory/ui/CollectionViewModel.kt` - Collection share functionality
- `app/src/main/kotlin/com/srg/inventory/ui/FolderDetailScreen.kt` - QR share button
- `app/src/main/kotlin/com/srg/inventory/ui/DecksScreen.kt` - Deck share functionality
- `app/src/main/kotlin/com/srg/inventory/ui/DeckEditorScreen.kt` - QR share button

**Current State:**
- ✅ Backend API deployed and tested at get-diced.com
- ✅ Collection folders can be shared via QR code
- ✅ Decks can be shared via QR code with full structure preserved
- ✅ QR codes display clickable URLs
- ✅ CSV export works for both collections and decks
- 📱 APK built (176MB) - ready for testing with better cable

**APK Location:** `app/build/outputs/apk/debug/app-debug.apk`

---

### Phase 3: QR Code Scanner Infrastructure (COMPLETED)

**Goal:** Add camera-based QR code scanner as 5th navigation tab

1. **Added Dependencies** (`app/build.gradle.kts`)
   - ZXing Embedded: `com.journeyapps:zxing-android-embedded:4.3.0`
   - Note: Removed redundant `com.google.zxing:core:3.5.2` (already included in embedded library)

2. **Added Permissions** (`AndroidManifest.xml`)
   - Camera permission: `android.permission.CAMERA`
   - Runtime permission request handling

3. **Created QR Scanner Screen** (`QRCodeScanScreen.kt`)
   - Runtime camera permission request
   - ZXing barcode scanner integration with ScanContract
   - User-friendly UI with scan button
   - Permission status indicator
   - Scan options: QR codes only, beep on scan

4. **Updated Navigation**
   - Added `Screen.Scan` route to Navigation.kt
   - Added scanner composable to NavHost
   - Added 5th tab to bottom navigation in MainScreen
   - Bottom nav now: Collection | Decks | Viewer | **Scan** | Settings

**Files Created:**
- `app/src/main/kotlin/com/srg/inventory/ui/QRCodeScanScreen.kt`

**Files Modified:**
- `app/build.gradle.kts` - Added ZXing embedded, removed redundant core library
- `app/src/main/AndroidManifest.xml` - Added camera permission
- `app/src/main/kotlin/com/srg/inventory/ui/Navigation.kt` - Added Scan route and screen
- `app/src/main/kotlin/com/srg/inventory/ui/MainScreen.kt` - Added Scan tab to bottom navigation

**Current State:**
- ✅ Scanner tab visible in bottom navigation
- ✅ Camera permission requested on first use
- ✅ ZXing scanner launches and scans QR codes
- ✅ Scanned URL captured successfully
- ⏸️ Import logic pending (currently just navigates back after scan)

**APK Location:** `app/build/outputs/apk/debug/app-debug.apk` (176MB)

---

## What's Next 📋

### Phase 3: Import Logic (TODO)

**Goal:** Parse scanned QR codes and import collections/decks

1. **URL Parsing:**
   - Extract shared list ID from get-diced.com URL
   - Example: `https://get-diced.com/create-list?shared=UUID` → extract UUID

2. **API Integration:**
   - Fetch shared list from `GET /api/shared-lists/{id}`
   - Parse response to determine type (COLLECTION vs DECK)
   - Extract card UUIDs and deck structure (if applicable)

3. **Import Dialogs:**
   - Create `ImportCollectionDialog.kt`:
     - Choose destination folder (existing or new)
     - Show card count and list name
     - Confirm import
   - Create `ImportDeckDialog.kt`:
     - Choose destination deck folder
     - Show deck name, spectacle type, card count
     - Confirm import with full structure preservation

4. **ViewModel Enhancements:**
   - **CollectionViewModel:**
     - `importCollectionFromSharedList(sharedListId, targetFolderId)`
     - Fetch cards from shared list
     - Add cards to selected folder
   - **DeckViewModel:**
     - `importDeckFromSharedList(sharedListId, targetDeckFolderId)`
     - Fetch deck structure from shared list
     - Create new deck with complete slot structure
     - Preserve spectacle type, entrance, competitor, deck slots, finishes, alternates

5. **UI Flow:**
   - User scans QR code
   - App extracts shared list ID
   - Shows loading indicator while fetching
   - Determines type and shows appropriate import dialog
   - User selects destination
   - Import completes with success message

**Files to Create:**
- `ImportCollectionDialog.kt`
- `ImportDeckDialog.kt`

**Files to Modify:**
- `CollectionViewModel.kt` - Add import function
- `DecksScreen.kt` (DeckViewModel) - Add import function
- `QRCodeScanScreen.kt` - Wire up import logic to onScanResult
- `Navigation.kt` - Pass ViewModels to scanner for import

---

# Session Notes - Nov 20, 2025 (Part 3)

## What Was Completed This Session ✅

### Card Database Sync Implementation (COMPLETED)

**Goal:** Implement hash-based database sync that downloads only when server has updates, preserving user data

**Server Side:**
1. **`generate_db_manifest.py`** - Creates `db_manifest.json` with:
   - SHA-256 hash of database file
   - Card count, finish links count, related cards count
   - File size, version, generated timestamp

2. **API Endpoints in `main.py`**:
   - `GET /api/cards/manifest` - Returns manifest JSON
   - `GET /api/cards/database` - Returns mobile DB file

3. **`workflow.sh` Updated**:
   ```bash
   python3 create_mobile_db.py srg_cards_mobile.db cards.yaml
   python3 generate_db_manifest.py
   python3 generate_image_manifest.py
   ```

**App Side:**
1. **Room Entities** (`Card.kt`):
   - `CardRelatedFinish` - Finish variant relationships (foils, etc.)
   - `CardRelatedCard` - Related card relationships
   - Both with proper indexes for performance

2. **MIGRATION_3_4** (`UserCardDatabase.kt`):
   - Creates `card_related_finishes` and `card_related_cards` tables
   - Database now at version 4

3. **`CardSyncRepository.kt`** - Completely rewritten:
   - Fetches manifest from server
   - Compares SHA-256 hash with stored local hash
   - Downloads entire DB when hash differs
   - Merges card tables while preserving user data (folders, decks)
   - Stores last sync hash in SharedPreferences

4. **`CollectionViewModel.kt`** - Updated sync:
   - Uses new `CardSyncRepository(context)` constructor
   - Shows sync status messages
   - `syncCardsFromWebsite()` uses new approach

5. **`GetDicedApi.kt`** - Added:
   - `getCardsManifest()` endpoint
   - `CardsManifest` data class

**Current Stats:**
- 3923 cards
- 973 related finish links
- 498 related card links
- Database size: ~1.5MB

**Key Feature:** User data (folders, folder_cards, decks, deck_cards) is preserved during sync. Only card data tables are replaced.

**Files Created:**
- `~/data/srg_card_search_website/backend/app/generate_db_manifest.py`

**Files Modified:**
- `~/data/srg_card_search_website/backend/app/main.py` - DB endpoints
- `~/data/srg_card_search_website/backend/app/workflow.sh` - Generate manifests
- `app/src/main/kotlin/com/srg/inventory/data/Card.kt` - Relationship entities
- `app/src/main/kotlin/com/srg/inventory/data/UserCardDatabase.kt` - Migration v3→v4
- `app/src/main/kotlin/com/srg/inventory/api/CardSyncRepository.kt` - Hash-based sync
- `app/src/main/kotlin/com/srg/inventory/api/GetDicedApi.kt` - Manifest endpoint
- `app/src/main/kotlin/com/srg/inventory/ui/CollectionViewModel.kt` - New sync approach
- `app/src/main/assets/cards_initial.db` - Regenerated with new tables

**Sync Commands:**
```bash
# On server/dev machine - regenerate DB and manifests
cd ~/data/srg_card_search_website/backend/app
./workflow.sh

# Or manually:
python3 create_mobile_db.py srg_cards_mobile.db cards.yaml
python3 generate_db_manifest.py

# Update bundled database for app release
cp srg_cards_mobile.db /home/brandon/data/srg_collection_manager_app/app/src/main/assets/cards_initial.db
```

---

# Session Notes - Nov 20, 2025 (Part 2)

## What Was Completed This Session ✅

### Google Play Store Preparation (IN PROGRESS)

**Release Build Configuration (COMPLETED):**
1. **Keystore Generated** (`release.keystore`)
   - Alias: `srg_inventory`
   - Validity: 10,000 days
   - Stored in project root (gitignored)

2. **Signing Config Added** (`app/build.gradle.kts`)
   - Loads credentials from `keystore.properties`
   - Both keystore and properties file gitignored

3. **ProGuard/R8 Rules** (`app/proguard-rules.pro`)
   - Rules for Room, Retrofit, OkHttp, Gson, Coroutines
   - Debug logs (Log.d, Log.v) stripped in release
   - Error logs (Log.e, Log.w) preserved

4. **Minification Enabled**
   - `isMinifyEnabled = true`
   - `isShrinkResources = true`
   - Release APK: 154MB (down from 167MB debug)

5. **Privacy Policy Published**
   - Created `/privacy` page on get-diced.com
   - URL: https://get-diced.com/privacy
   - Covers app and website data practices

6. **Store Descriptions Drafted**
   - Short (77 chars): Card viewer, collection manager, and deck builder for SRG Supershow card game.
   - Full description with features, links, disclaimer

7. **Acknowledgements Added** (`README.md`)
   - Steve Resk, SRG Universe team, wrestlers, community
   - SRGPC.net contributors
   - Claude (Anthropic)

**Files Modified:**
- `app/build.gradle.kts` - Signing config, minification
- `app/proguard-rules.pro` - Library rules, log stripping
- `.gitignore` - Keystore exclusions
- `README.md` - Acknowledgements section

**Files Created:**
- `release.keystore` - Release signing key
- `keystore.properties` - Keystore credentials
- `~/data/srg_card_search_website/frontend/src/pages/PrivacyPolicy.jsx`

**Remaining Tasks:**
- [ ] Test on multiple device sizes
- [ ] Screenshots (phone + tablet)
- [ ] Feature graphic (1024x500)
- [ ] Content rating questionnaire

**Keystore Credentials:**
- Stored in `keystore.properties` (gitignored)
- Keep a secure backup of both `release.keystore` and `keystore.properties`

---

### Image Sync Implementation (COMPLETED)

**Goal:** Allow app to sync new/changed images from server without re-downloading everything

**Server Side:**
1. `generate_image_manifest.py` - Creates `images_manifest.json` with SHA-256 hashes
2. `/api/images/manifest` endpoint in main.py
3. `/images/mobile/{path}` static file serving

**App Side:**
1. `ImageSyncRepository.kt` - Core sync logic
   - Loads bundled manifest from assets
   - Fetches server manifest via API
   - Compares SHA-256 hashes
   - Downloads only new/changed images to internal storage
   - Saves local manifest tracking synced images

2. `ImageUtils.kt` - Updated loading strategy
   - Priority: synced images → bundled assets → placeholder
   - `getSyncedImageFile()` checks internal storage first

3. `CollectionViewModel` - Image sync state
   - `isImageSyncing`, `imageSyncProgress` state flows
   - `syncImages()` function with progress callback

4. `SettingsScreen` - UI for image sync
   - "Images" card with "Sync new images" button
   - Progress indicator showing downloaded/total
   - Status message when complete

**Workflow:**
1. App ships with `images_manifest.json` in assets (3648 images)
2. User taps "Sync new images" in Settings
3. App fetches server manifest, compares hashes
4. Downloads only images where hash differs or missing
5. Saves to internal storage (`synced_images/{first2}/{uuid}.webp`)
6. ImageUtils loads synced images first, then falls back to bundled

**Files Created:**
- `~/data/srg_card_search_website/backend/app/generate_image_manifest.py`
- `~/data/srg_card_search_website/backend/app/convert_to_mobile.py`
- `app/src/main/kotlin/com/srg/inventory/api/ImageSyncRepository.kt`
- `app/src/main/assets/images_manifest.json`

**Files Modified:**
- `~/data/srg_card_search_website/backend/app/main.py` - Added manifest endpoint
- `app/src/main/kotlin/com/srg/inventory/api/GetDicedApi.kt` - Added manifest API
- `app/src/main/kotlin/com/srg/inventory/utils/ImageUtils.kt` - Check synced first
- `app/src/main/kotlin/com/srg/inventory/ui/CollectionViewModel.kt` - Image sync state
- `app/src/main/kotlin/com/srg/inventory/ui/SettingsScreen.kt` - Sync UI
- `bundle_images.sh` - Generate and copy manifest

**Image Sync Commands:**
```bash
# Generate manifest and convert missing images (on dev machine)
cd ~/data/srg_card_search_website/backend/app
python3 generate_image_manifest.py
python3 convert_to_mobile.py

# Upload mobile images to server
rsync -avz ~/data/srg_card_search_website/backend/app/images/mobile/ dondo@get-diced.com:srg_card_search_website/backend/app/images/mobile/

# Bundle for app release
cd /home/brandon/data/srg_collection_manager_app
./bundle_images.sh
```

**Current State:**
- 3648 images synced and bundled (166MB)
- Manifest generated with SHA-256 hashes
- Server endpoints deployed
- App UI complete with sync button

---

# Session Notes - Nov 20, 2025 (Part 1)

## What Was Completed This Session ✅

### Deckbuilding Foundation (COMPLETED)
**Goal:** Implement deckbuilding feature with folders, decks, and card slots

**Data Layer (COMPLETED):**
1. **DeckFolder Entity** (`DeckFolder.kt`)
   - Organizes decks by type: Singles, Tornado, Trios, Tag (defaults)
   - Custom folders supported
   - DAO with all CRUD operations

2. **Deck Entity** (`Deck.kt`)
   - Belongs to a folder
   - Has spectacle type (Newman/Valiant)
   - Tracks created/modified timestamps

3. **DeckCard Entity** (`Deck.kt`)
   - Slot types: ENTRANCE, COMPETITOR, DECK (1-30), FINISH, ALTERNATE
   - Links cards to deck slots

4. **DeckRepository** (`DeckRepository.kt`)
   - All deck operations
   - Folder management
   - Card slot operations (setEntrance, setCompetitor, setDeckCard, addFinish, addAlternate)

5. **Database Migration v2→v3**
   - Added deck_folders, decks, deck_cards tables
   - Type converters for SpectacleType and DeckSlotType enums
   - Default folders created on migration

**UI Screens (COMPLETED):**
1. **DecksScreen** - Shows deck folders (Singles, Tornado, Trios, Tag)
   - Folder icons per type
   - Deck count per folder
   - Create custom folders
   - Delete custom folders

2. **DeckListScreen** - Shows decks within a folder
   - Deck name, card count, spectacle type badge
   - Modified date
   - Create/delete decks
   - Navigate to deck editor

3. **DeckViewModel** - State management for all deck operations

4. **Navigation** - Added DeckFolderDetail and DeckDetail routes

5. **DeckEditorScreen** (`DeckEditorScreen.kt`)
   - Entrance card slot (1)
   - Competitor card slot (1)
   - Deck cards 1-30 (finishes are part of deck, typically 28-30)
   - Alternates section (add multiple)
   - Spectacle type selector (Newman/Valiant) in top bar
   - Card removal functionality
   - Full card picker with search and filtering

6. **Card Picker Integration**
   - Search cards by name within picker dialog
   - Smart filtering by slot type:
     - Entrance → EntranceCard only
     - Competitor → Filtered by folder (Singles→SingleCompetitorCard, Tornado→TornadoCompetitorCard, Trios→TrioCompetitorCard)
     - Deck slots → Filtered by deck_card_number (slot #1 shows only cards with deck_card_number=1, etc.)
     - Alternates → Any card type
   - Tap to select, auto-closes dialog

**What's Working:**
- View deck folders with counts
- Navigate into folders to see decks
- Create/delete decks
- See deck metadata (card count, spectacle type, modified date)
- Full deck editor UI with all slots
- Navigate: Decks tab → Folder → Deck → Editor
- Card picker with smart filtering
- Add/remove cards from any slot
- Change spectacle type (Newman/Valiant)

7. **CSV Export/Import**
   - Export deck to CSV (Slot Type, Slot Number, Card Name)
   - Import deck from CSV file
   - FileProvider for sharing

8. **Shared List API Integration**
   - Share deck to get-diced.com API
   - Copy shareable link to clipboard
   - Import deck from shareable URL
   - Cards imported as alternates

**All deckbuilding features complete!**

---

# Session Notes - Nov 19, 2025 (Part 4)

## What Was Completed This Session ✅

### Folder Sorting (COMPLETED)
**Goal:** Implement custom sort order for cards in folders

**What was done:**
- Added `sortCardsByType()` function in CollectionViewModel
- Sort order: EntranceCard → SingleCompetitorCard → TornadoCompetitorCard → TrioCompetitorCard → MainDeckCard (by deck #) → SpectacleCard (Valiant then Newman) → CrowdMeterCard
- Alphabetical within each type

### CSV Export/Import (COMPLETED)
**Goal:** Allow exporting and importing folder contents via CSV

**What was done:**
1. **CSV Export**
   - Download icon in folder TopAppBar
   - Exports: Name, Quantity, Card Type, Deck #, Attack Type, Play Order, Division
   - Uses FileProvider for Android sharing
   - Added file_paths.xml and AndroidManifest FileProvider config

2. **CSV Import**
   - Upload icon in folder TopAppBar
   - Supports both app format and website format
   - Auto-detects headers and column positions
   - Matches cards by name (case-insensitive)
   - Shows import results with not-found card names
   - Added getCardByName to CardDao, CollectionRepository, CollectionViewModel

### App Icon Fix (COMPLETED)
- Removed mipmap-anydpi-v26 directory (adaptive icons overriding PNGs)
- Copied ic_launcher.png to ic_launcher_round.png for all densities

### UI Improvements (COMPLETED)
- Made card rows clickable (removed magnifying lens icon)
- Clicking card opens detail dialog directly

---

# Session Notes - Nov 19, 2025 (Part 3)

## What Was Completed This Session ✅

### Image Integration (COMPLETED)
**Goal:** Add card images to detail dialogs with mobile-optimized compression

**What was done:**
1. **Mobile-Optimized Images**
   - Created mobile variant at quality 75 (158MB total)
   - Updated `convert_images.py` to produce mobile output
   - Updated `bundle_images.sh` to use mobile images only
   - APK size: 167MB (down from 295MB with fullsize)

2. **Images in Detail Dialogs**
   - CardSearchScreen (Viewer) - Full card details with image
   - AddCardToFolderScreen - Card image when adding to folder
   - FolderDetailScreen - Full card details with image, stats, rules
   - CollectionScreen - Card image in edit dialog

3. **ImageUtils Updated**
   - Changed to always use mobile assets
   - Path: `mobile/{first2}/{uuid}.webp`
   - Simplified loading strategy (no thumbnails)

### UI/UX Improvements (COMPLETED)
1. **Renamed Search to Viewer**
   - Bottom nav now shows "Viewer" instead of "Search"
   - Removed "Add to Collection" button from Viewer
   - Viewer is now purely for browsing and viewing cards

2. **New Edit Quantity Dialog**
   - +/- buttons to increment/decrement quantity
   - Large quantity display
   - Delete button to remove card from folder
   - Cleaner UX than text field

3. **Separate Card Actions in Folders**
   - 🔍 Search icon → View full card details
   - ✏️ Edit icon → Edit quantity dialog
   - Clear separation of viewing vs editing

4. **Full Card Details in Collection**
   - Clicking a card in folder shows CardDetailDialog
   - Shows image, stats (for competitors), rules, errata
   - Same quality as Viewer detail view

---

# Session Notes - Nov 19, 2025 (Part 2)

## What Was Completed This Session ✅

### Navigation Restructure (COMPLETED)
**Goal:** Create clean 4-tab bottom navigation: Collection | Decks | Card Search | Settings

**What was done:**
1. **Updated MainScreen.kt**
   - Added bottom navigation bar with 4 tabs
   - Implemented tab switching logic
   - Hides bottom nav on detail screens (FolderDetail, AddCardToFolder)
   - Material 3 NavigationBar design

2. **Created SettingsScreen.kt**
   - Moved sync functionality from FoldersScreen
   - Shows database statistics (card count, last sync time)
   - Manual sync button with progress indicator
   - Error message handling
   - "About" section with app info
   - Info card explaining bundled database strategy

3. **Created CardSearchScreen.kt**
   - Standalone card browsing (separate from adding to folders)
   - Full search and filter capabilities
   - Card details dialog with "Add to Collection" button
   - Uses same filters as AddCardToFolderScreen
   - Shows deck card number (#) for MainDeck cards

4. **Created DecksScreen.kt**
   - Placeholder for future deckbuilding feature
   - "Coming Soon" message
   - Preview of planned features

5. **Updated FoldersScreen.kt**
   - Removed sync button from top bar
   - Cleaned up UI - now just shows folders and card count

6. **Updated Navigation.kt**
   - Added routes for new screens (Collection, Decks, CardSearch, Settings)
   - Updated navigation structure to support tabs

### Image Loading Infrastructure Setup (IN PROGRESS)
**Goal:** Set up image loading architecture with bundled thumbnails + server fallback

**What was done:**
1. **Added Coil dependency**
   - `io.coil-kt:coil-compose:2.5.0` for async image loading
   - Supports WebP format
   - Built-in caching and memory management

2. **Created ImageUtils.kt**
   - Helper functions for image loading
   - `getAssetPath()` - Get bundled asset path
   - `getImageUrl()` - Get server URL for images
   - `buildCardImageRequest()` - Build Coil request with fallback
   - Supports both thumbnails and fullsize images

3. **Created bundle_images.sh script**
   - Copies thumbnails from website to app assets
   - Uses `IMAGE_SOURCE_DIR` environment variable
   - Default: `~/data/srg_card_search_website/backend/app/images`
   - Preserves directory structure: `thumbnails/{first2}/{uuid}.webp`

4. **Bundled thumbnail images**
   - Copied 3,481 thumbnail images to app/src/main/assets/thumbnails/
   - Total size: 34MB (WebP compressed)
   - Organized by first 2 chars of UUID for efficient lookup
   - Images ready to use offline immediately

5. **Image Loading Strategy Designed**
   ```
   Priority: Bundled assets → Coil cache → Server download → Placeholder
   ```
   - Try bundled assets first (fast, offline)
   - Fall back to cached downloads
   - Download from server if needed
   - Show placeholder if unavailable

### UI Improvements
1. **Deck Card Number Display**
   - MainDeck cards now show `#` followed by deck_card_number
   - Displayed prominently (bold) before attack type and play order
   - Applied to both AddCardToFolderScreen and CardSearchScreen

2. **App Icon Updated**
   - Replaced default icon with get-diced.com favicon
   - Copied dice icon to all mipmap densities (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi)
   - App now has proper branding

### Database Schema Fix
**Issue:** Bundled database had schema mismatch with Room expectations

**Fix:**
1. Updated `create_mobile_db.py` to match Room's exact schema requirements:
   - Removed `DEFAULT` clauses (Room doesn't expect them in bundled DB)
   - Removed indexes (Room creates them automatically)
   - Fixed `is_custom` column in user_cards table

2. Regenerated bundled database with correct schema
3. Tested app launch - no more crashes!

## Current State 📍

### What Works
- ✅ 4-tab bottom navigation (Collection | Decks | Card Search | Settings)
- ✅ App ships with 3,922 cards + 3,481 thumbnail images
- ✅ Folder-based collection system
- ✅ Advanced card search with type-specific filters
- ✅ Standalone card browsing (view cards before adding)
- ✅ Settings screen with sync functionality
- ✅ Deck card number prominently displayed
- ✅ Custom app icon matching website
- ✅ Image loading infrastructure ready (Coil + ImageUtils)
- ✅ Offline-first architecture

### Image Status
- ✅ 3,481 thumbnails bundled (34MB)
- ✅ Coil library integrated
- ✅ ImageUtils helper created
- ✅ Bundle script created
- 🚧 UI integration pending (next session)
- 🔜 Server image sync (background task)

### What Still Needs Work
- 🔜 Integrate images into UI components (card lists, details dialogs)
- 🔜 Image sync from server (background download of missing images)
- 🔜 Sorting system for cards in collection folders
- 🔜 Search within collection folders
- 🔜 Deckbuilding feature

## Next Session Tasks 📋

### Priority 1: Integrate Images into UI
**Goal:** Show card images in all relevant UI components

**Files to modify:**
1. `AddCardToFolderScreen.kt` - Show thumbnails in card list
2. `CardSearchScreen.kt` - Show thumbnails in browse list
3. `FolderDetailScreen.kt` - Show thumbnails in folder contents
4. Update card detail dialogs to show fullsize images

**Implementation approach:**
```kotlin
AsyncImage(
    model = ImageUtils.buildCardImageRequest(context, card.dbUuid, thumbnail = true),
    contentDescription = card.name,
    modifier = Modifier.size(48.dp)
)
```

### Priority 2: Image Sync from Server
**Goal:** Background task to download new/missing images from server

**Implementation plan:**
1. Create `ImageSyncRepository.kt`
   - Check which images are missing locally
   - Download from `https://get-diced.com/images/thumbnails/{first2}/{uuid}.webp`
   - Save to internal storage
   - Track sync progress

2. Add image sync to SettingsScreen
   - "Sync Images" button
   - Progress indicator
   - Show downloaded/total counts

3. Optional: Auto-sync images during card database sync

### Priority 3: Folder Enhancements
**Goal:** Add sorting and search within folders

**Features:**
1. **Sorting options:**
   - By name (A-Z, Z-A)
   - By card type
   - By deck card number (for MainDeck cards)
   - By quantity
   - By date added

2. **Search within folder:**
   - Search bar at top of FolderDetailScreen
   - Filter displayed cards
   - Same search logic as main search

### Priority 4: Deckbuilding
- See PIVOT_PLAN.md for detailed deckbuilding implementation plan

## Important Files Reference 📁

### New Files Created This Session
- **Settings:** `app/src/main/kotlin/com/srg/inventory/ui/SettingsScreen.kt`
- **Card Search:** `app/src/main/kotlin/com/srg/inventory/ui/CardSearchScreen.kt`
- **Decks:** `app/src/main/kotlin/com/srg/inventory/ui/DecksScreen.kt`
- **Image Utils:** `app/src/main/kotlin/com/srg/inventory/utils/ImageUtils.kt`
- **Bundle Script:** `bundle_images.sh`

### Modified Files This Session
- **Navigation:** `app/src/main/kotlin/com/srg/inventory/ui/Navigation.kt`
- **Main Screen:** `app/src/main/kotlin/com/srg/inventory/ui/MainScreen.kt`
- **Folders Screen:** `app/src/main/kotlin/com/srg/inventory/ui/FoldersScreen.kt`
- **Add Card Screen:** `app/src/main/kotlin/com/srg/inventory/ui/AddCardToFolderScreen.kt`
- **Build Config:** `app/build.gradle.kts` (added Coil dependency)
- **DB Script:** `~/data/srg_card_search_website/backend/app/create_mobile_db.py`
- **App Icons:** `app/src/main/res/mipmap-*/ic_launcher.png`

### Assets
- **Bundled Database:** `app/src/main/assets/cards_initial.db` (1.6MB, 3,922 cards)
- **Bundled Images:** `app/src/main/assets/thumbnails/**/*.webp` (34MB, 3,481 images)

## Build & Deploy Commands 🛠️

```bash
# Bundle images (if needed)
IMAGE_SOURCE_DIR=~/data/srg_card_search_website/backend/app/images ./bundle_images.sh

# Clean and build
cd /home/brandon/data/srg_collection_manager_app
./gradlew clean assembleDebug

# Install on device
adb uninstall com.srg.inventory  # If needed
adb install app/build/outputs/apk/debug/app-debug.apk

# View logs
adb logcat | grep -E "srg|SRG|inventory"
```

## Regenerate Bundled Database 🔄

If cards.yaml is updated:
```bash
cd ~/data/srg_card_search_website/backend/app
python3 create_mobile_db.py srg_cards_mobile.db cards.yaml
cp srg_cards_mobile.db /home/brandon/data/srg_collection_manager_app/app/src/main/assets/cards_initial.db
```

## Re-bundle Images 🖼️

If new images are added to website:
```bash
cd /home/brandon/data/srg_collection_manager_app
IMAGE_SOURCE_DIR=~/data/srg_card_search_website/backend/app/images ./bundle_images.sh
```

## Notes for Next Developer 💡

1. **Navigation is restructured** - 4 clean tabs with proper bottom nav
2. **Images are bundled** - 3,481 thumbnails ready to use offline
3. **Coil is configured** - Just need to add AsyncImage components to UI
4. **Image loading strategy** - Bundled → Cached → Server → Placeholder
5. **Settings screen** - Centralized location for sync and configuration
6. **Card Search** - Standalone browsing separate from adding to folders
7. **Deck card numbers** - Displayed prominently for MainDeck cards
8. **App icon updated** - Matches get-diced.com branding

## Technical Decisions Made 📝

### Image Architecture
- **Bundled thumbnails:** 3,481 images (34MB) - acceptable for app size
- **Fullsize on-demand:** Not bundled (259MB too large), download from server when needed
- **Coil library:** Handles caching, async loading, WebP support automatically
- **Asset structure:** Mirrors server structure for consistency

### Navigation Structure
- **Bottom navigation:** Material 3 NavigationBar with icons + labels
- **4 tabs:** Collection, Decks, Card Search, Settings
- **Hide on details:** Bottom nav hidden when viewing folder contents or adding cards
- **State preservation:** Each tab maintains its own navigation stack

### Schema Fixes
- **No DEFAULT clauses** - Room doesn't expect them in pre-packaged databases
- **No indexes** - Room creates them automatically based on entity annotations
- **Exact type matching** - INTEGER vs INTEGER NOT NULL matters

## Testing Done This Session ✅

1. **App launch** - Successfully loads with bundled database and images
2. **Navigation** - All 4 tabs work correctly
3. **Settings screen** - Displays stats and sync works
4. **Card Search** - Filters and search work standalone
5. **Deck card numbers** - Display correctly for MainDeck cards
6. **App icon** - Shows dice icon in launcher

## Known Issues 🐛

None! Everything is working smoothly.

---

## Remaining Features for Future Versions 📋

### High Priority
1. **Image sync from server** - Download new images when cards are synced (currently only bundled images work)
2. **Search within folders** - Filter/search cards in collection folders

### Nice to Have
3. **Deck validation** - Check deck completeness (all 30 slots filled, entrance, competitor)
4. **Folder sorting options** - Sort by name, type, quantity, date added

---

## App Store Release Preparation 🚀

### Version 1.0 Scope
- All current features are ready for initial release
- Incremental feature delivery planned for future versions
- Database migrations ensure user data (collections, decks) preserved on updates

### Technical Tasks
- [ ] Release signing key (keystore)
- [ ] ProGuard/R8 minification rules
- [ ] Version code/name in build.gradle
- [ ] Remove debug logging
- [ ] Test on multiple device sizes

### Store Listing
- [ ] Screenshots (phone + tablet)
- [ ] Feature graphic (1024x500)
- [ ] Short description (80 chars)
- [ ] Full description
- [ ] Privacy policy URL
- [ ] Content rating questionnaire

### Legal/Copyright
- **Source Code License**: MIT License
- **Card Content**: All SRG Supershow card properties are copyright SRG Universe
- **Disclaimer**: This is an unofficial fan project - in no way supported or explicitly endorsed by the game or its creators
- **Permission**: Steve Resk (SRG - Steve Resk Gaming) has given permission for this project

### Database Migration Safety
- Room database migrations (v1→v2→v3) preserve all user data
- Collections (folder_cards) and decks (deck_folders, decks, deck_cards) are never dropped
- App updates will NOT erase saved cards or decks

---

## Session Summary - Nov 20, 2025 (Current)

### What Was Completed
1. **Deckbuilding Feature** - Complete (see top of notes)
2. **Bug Fixes**
   - Filter persistence fix: Filters now clear when switching card types in Viewer
   - Deck editor layout: Deck name on own line, action buttons below
3. **Documentation Updates**
   - SESSION_NOTES.md - Full deckbuilding completion details
   - PIVOT_PLAN.md - Phase 3 marked complete, Session 7 complete
   - README.md - Added deckbuilding features, copyright/disclaimer section
4. **App Store Preparation Planning**
   - Added remaining features list
   - Added release preparation checklist
   - Added legal/copyright notices

### Files Modified This Session
- `app/src/main/kotlin/com/srg/inventory/ui/DeckEditorScreen.kt` - Layout fix
- `app/src/main/kotlin/com/srg/inventory/ui/CollectionViewModel.kt` - Filter clearing
- `SESSION_NOTES.md` - Full update
- `PIVOT_PLAN.md` - Phase 3 complete
- `README.md` - Copyright/disclaimer added

### Next Session: Google Play Store Preparation
- Release build configuration
- ProGuard/R8 rules
- Signing keystore generation
- Privacy policy draft
- Store listing content

## Token Usage This Session 📊
- Session 1 (bundled DB): ~84,000 tokens
- Session 2 (navigation + images): ~88,000 tokens
- Session 3 (deckbuilding + docs): ~85,000 tokens
- Total: ~257,000 tokens across sessions

## Previous Session (Part 1)

See beginning of file for bundled database implementation details.

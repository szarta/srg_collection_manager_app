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

## Token Usage This Session 📊
- Session 1 (bundled DB): ~84,000 tokens
- Session 2 (navigation + images): ~88,000 tokens
- Total: ~172,000 / 200,000 tokens
- Remaining: ~28,000 tokens

## Previous Session (Part 1)

See beginning of file for bundled database implementation details.

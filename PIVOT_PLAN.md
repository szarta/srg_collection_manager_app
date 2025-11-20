# SRG Card Scanner → Deckbuilder Pivot Plan

## Context
Camera scanning is unreliable due to mismatch between phone photos and high-quality reference scans (31-bit pHash difference). Pivoting to focus on card search and deckbuilding, leveraging existing get-diced.com infrastructure.

## Phase 1: Remove Scanning Capability ✂️

### Goals
- Remove camera scanning feature
- Remove OpenCV dependency
- Clean up unused code
- Simplify app architecture

### Tasks
1. **Remove Camera UI** (`app/src/main/kotlin/com/srg/inventory/ui/`)
   - Delete `CameraScanScreen.kt`
   - Remove camera tab from `MainScreen.kt`
   - Update navigation to 2 tabs: Search + Collection

2. **Remove Camera Dependencies** (`app/build.gradle.kts`)
   - Remove CameraX libraries
   - Remove OpenCV dependency
   - Remove camera permissions from manifest

3. **Remove Matching Utilities** (`app/src/main/kotlin/com/srg/inventory/utils/`)
   - Delete `CardMatcher.kt`
   - Delete `ORBFeatures.kt`
   - Delete `PerceptualHash.kt`

4. **Remove ViewModel Scan Logic** (`app/src/main/kotlin/com/srg/inventory/ui/CardViewModel.kt`)
   - Remove `scanCard()` function
   - Remove `ScanState` sealed class
   - Remove image saving logic
   - Keep collection management and search

5. **Remove Hash Database**
   - Remove `card_hashes.db` from assets
   - Remove `CardHash.kt` entity
   - Remove `CardHashDao.kt`
   - Remove `CardHashDatabase.kt`
   - Keep `UserCard` database for collection

6. **Clean Up**
   - Remove `generate_hashes.py` (no longer needed)
   - Update README to reflect new purpose
   - Remove captured images from device

**Estimated Time**: 1-2 hours

---

## Phase 2: Integrate with get-diced.com 🎲 ✅ COMPLETED

### Goals ✅
- ✅ Pull card database from get-diced.com
- ✅ Implement fast, rich card search with filters
- ✅ Cache data for offline use
- ✅ Folder-based collection system
- 🚧 Display card images from get-diced.com (next)

### Research ✅
1. **Inspected get-diced.com codebase**
   - ✅ Documented API endpoints (search, filters, shared lists)
   - ✅ Documented database schema (7 card types)
   - ✅ Documented image storage structure (WebP thumbnails and full-size)
   - ✅ Documented search implementation with filters

### Implementation Tasks

#### 2.1: API Integration ✅ COMPLETED
1. **Created API client** (`app/src/main/kotlin/com/srg/inventory/api/`)
   - ✅ `GetDicedApi.kt` - Retrofit interface with all endpoints
   - ✅ `RetrofitClient.kt` - Singleton API client
   - ✅ `ApiModels.kt` - Request/response models
   - ✅ `CardMapper.kt` - DTO to entity mapper
   - ✅ `CardSyncRepository.kt` - Batch sync operations

2. **API Endpoints Implemented**
   - ✅ `GET /cards` - Fetch cards with pagination and filters
   - ✅ `GET /cards/{uuid}` - Get card by UUID
   - ✅ `GET /cards/slug/{slug}` - Get card by slug
   - ✅ `POST /cards/by-uuids` - Batch fetch by UUIDs
   - ✅ `POST /api/shared-lists` - Create shareable list
   - ✅ `GET /api/shared-lists/{id}` - Get shared list
   - ✅ Image endpoints documented (thumbnails and full-size)

3. **Dependencies Added** ✅
   - ✅ Retrofit 2.9.0 for API calls
   - ✅ Gson 2.10.1 for JSON parsing
   - ✅ OkHttp logging interceptor
   - 🔜 Coil for image loading (next)

#### 2.2: Local Database ✅ COMPLETED
1. **Created Database Entities** (`app/src/main/kotlin/com/srg/inventory/data/`)
   - ✅ `Card.kt` - Full card data from API (supports all 7 card types)
   - ✅ `Folder.kt` - Collection folders (Owned, Wanted, Trade, + custom)
   - ✅ `FolderCard.kt` - Many-to-many junction table
   - ✅ Includes: UUID, name, type, rules, stats, tags, etc.

2. **Updated Database** ✅
   - ✅ `CardDao.kt` - CRUD and search operations
   - ✅ `FolderDao.kt` - Folder management
   - ✅ `FolderCardDao.kt` - Junction table operations
   - ✅ `UserCardDatabase.kt` - Upgraded to v2 with migration
   - ✅ Automatic migration from v1 preserving user data

3. **Caching Strategy** ✅
   - ✅ Manual sync button in UI
   - ✅ Batch download with progress tracking
   - ✅ Offline-first architecture
   - ✅ Last sync timestamp display

#### 2.3: Enhanced Search UI ✅ COMPLETED
1. **Redesigned Collection System** (`app/src/main/kotlin/com/srg/inventory/ui/`)
   - ✅ `FoldersScreen.kt` - Folder list with sync button
   - ✅ `FolderDetailScreen.kt` - Cards in folder
   - ✅ `AddCardToFolderScreen.kt` - Search with advanced filters
   - ✅ `CollectionViewModel.kt` - State management
   - ✅ `Navigation.kt` - Type-safe navigation

2. **Advanced Filters Implemented** ✅
   - ✅ Card Type filter (7 types)
   - ✅ Attack Type filter (Strike, Grapple, Submission)
   - ✅ Play Order filter (Lead, Followup, Finish)
   - ✅ Division filter (for competitors)
   - ✅ Real-time search across name and rules text
   - ✅ Type-specific filters shown dynamically

3. **Image Loading** 🚧 NEXT
   - 🔜 Bundle images in app resources OR
   - 🔜 Use Coil to load from get-diced.com
   - 🔜 Cache images locally
   - 🔜 Placeholder/error images

**Actual Time**: ~6 hours (more comprehensive than planned)

---

## Phase 3: Deckbuilding Feature 🃏 ✅ COMPLETED

### Goals ✅
- ✅ Create/edit/delete decks
- ✅ Add cards to decks with slot-based system
- ✅ Smart card filtering by slot type
- ✅ Export/import decks via CSV and get-diced.com API

### Tasks

#### 3.1: Data Layer ✅ COMPLETED
1. **Created Deck Entities** (`app/src/main/kotlin/com/srg/inventory/data/`)
   - ✅ `DeckFolder.kt` - Organizes decks by type (Singles, Tornado, Trios, Tag, custom)
   - ✅ `Deck.kt` - id, name, folderId, spectacleType, timestamps
   - ✅ `DeckCard.kt` - deckId, cardUuid, slotType (ENTRANCE, COMPETITOR, DECK, ALTERNATE), slotNumber

2. **Created DAOs** ✅
   - ✅ `DeckFolderDao.kt` - Folder CRUD
   - ✅ `DeckDao.kt` - Deck CRUD
   - ✅ `DeckCardDao.kt` - Slot-based card management
   - ✅ Query: Get deck with all cards and details

3. **Repository** ✅
   - ✅ `DeckRepository.kt` - All deck operations
   - ✅ setEntrance, setCompetitor, setDeckCard, addAlternate
   - ✅ removeCardFromDeck
   - ✅ Database migration v2→v3

#### 3.2: Deckbuilding UI ✅ COMPLETED
1. **Deck Folders Screen** ✅
   - ✅ `DecksScreen.kt` - Shows deck folders (Singles, Tornado, Trios, Tag)
   - ✅ Folder icons per type
   - ✅ Deck count per folder
   - ✅ Create/delete custom folders

2. **Deck List Screen** ✅
   - ✅ `DeckListScreen.kt` - Show decks in folder
   - ✅ Deck name, card count, spectacle type badge
   - ✅ Create/delete decks
   - ✅ Navigate to deck editor

3. **Deck Editor Screen** ✅
   - ✅ `DeckEditorScreen.kt` - Full slot-based editor
   - ✅ Entrance card slot (1)
   - ✅ Competitor card slot (1)
   - ✅ Deck cards 1-30 (finishes are 28-30)
   - ✅ Alternates section (unlimited)
   - ✅ Spectacle type selector (Newman/Valiant)
   - ✅ Card picker with smart filtering

4. **Smart Features** ✅
   - ✅ Filter by slot type (Entrance → EntranceCard only)
   - ✅ Filter competitors by folder (Singles → SingleCompetitorCard, etc.)
   - ✅ Filter deck cards by deck_card_number (slot #1 → cards with deck_card_number=1)

#### 3.3: Export/Import ✅ COMPLETED
1. **CSV Export/Import** ✅
   - ✅ Export deck to CSV (Slot Type, Slot Number, Card Name)
   - ✅ Import deck from CSV file
   - ✅ FileProvider for sharing

2. **Shared List API Integration** ✅
   - ✅ Share deck to get-diced.com API (`POST /api/shared-lists`)
   - ✅ Copy shareable link to clipboard
   - ✅ Import deck from shareable URL
   - ✅ Cards imported as alternates

**Actual Time**: ~3 hours

---

## Phase 4: UI/UX Polish 🎨

### Tasks
1. **Update Navigation**
   - Remove "Scan" tab
   - Tabs: Search | Collection | Decks

2. **Improve Card Display**
   - Grid view for search results
   - Large card images
   - Smooth transitions

3. **Material Design**
   - Update color scheme
   - Add animations
   - Improve spacing/typography

4. **Performance**
   - Lazy loading for large lists
   - Image caching
   - Database indexing

**Estimated Time**: 2-3 hours

---

## Implementation Status

### ✅ Session 1: Cleanup (COMPLETED)
1. ✅ Remove camera scanning UI and code
2. ✅ Remove OpenCV and matching utilities
3. ✅ Clean up dependencies and unused files
4. ✅ Test app still runs with simplified architecture

### ✅ Session 2: Get-Diced Integration (COMPLETED)
1. ✅ Inspect get-diced.com codebase and document APIs
2. ✅ Document API endpoints and data models
3. ✅ Implement API client (Retrofit + Gson)
4. ✅ Create CardSyncRepository for batch sync operations
5. ✅ Update database schema to v2 (Folder-based collections)
6. ✅ Implement database migration from v1 to v2
7. ✅ Manual sync with progress tracking
8. 🚧 Implement image loading (next step)

### ✅ Session 3: Enhanced Search (COMPLETED)
1. ✅ Build folder-based collection UI
2. ✅ Add type-specific filters (card type, atk type, play order, division)
3. ✅ Create folder list, folder detail, and add card screens
4. ✅ Implement navigation between screens
5. ✅ Support multi-folder cards with independent quantities

### ✅ Session 4: Navigation & Image Infrastructure (COMPLETED - Nov 19, 2025)
1. ✅ Restructure navigation to 4-tab bottom nav (Collection | Decks | Card Search | Settings)
2. ✅ Create SettingsScreen with sync functionality
3. ✅ Create CardSearchScreen for standalone card browsing
4. ✅ Create DecksScreen placeholder
5. ✅ Update app icon to match get-diced.com branding
6. ✅ Add Coil image loading library
7. ✅ Create ImageUtils helper for image loading
8. ✅ Bundle 3,481 thumbnail images (34MB) with app
9. ✅ Create bundle_images.sh script with IMAGE_SOURCE_DIR support
10. ✅ Show deck card number (#) prominently for MainDeck cards

### ✅ Session 5: Image Integration (COMPLETED - Nov 19, 2025)
1. ✅ Mobile-optimized images (quality 75, 158MB total)
2. ✅ Images in all card detail dialogs
3. ✅ Full card details view (image, stats, rules, errata)
4. ✅ Renamed Search to Viewer (read-only browsing)
5. ✅ Edit quantity dialog with +/- buttons and delete
6. ✅ Separate view and edit actions in folder cards
7. ✅ Updated convert_images.py with mobile output
8. ✅ Updated bundle_images.sh for mobile-only bundling

### ✅ Session 6: Folder Enhancements (COMPLETED - Nov 20, 2025)
1. ✅ Add sorting by card type with custom order (Entrance → Competitor → MainDeck → Spectacle → CrowdMeter)
2. ✅ CSV export functionality (download icon, FileProvider)
3. ✅ CSV import functionality (upload icon, supports app + website format)
4. ✅ App icon fix (removed adaptive icon overrides)
5. ✅ Clickable card rows (removed magnifying lens)
6. ✅ Added getCardByName to data layer for import matching

### ✅ Session 7: Deckbuilding (COMPLETED - Nov 20, 2025)
1. ✅ Created deck data layer (DeckFolder, Deck, DeckCard entities)
2. ✅ Built deck folders screen (Singles, Tornado, Trios, Tag)
3. ✅ Built deck list screen
4. ✅ Built full deck editor with slot-based system
5. ✅ Smart card filtering (by slot type, folder type, deck_card_number)
6. ✅ CSV export/import for decks
7. ✅ Shared list API integration (share to web, import from URL)
8. ✅ Database migration v2→v3

### ✅ Session 8: Card Database Sync (COMPLETED - Nov 20, 2025)
1. ✅ Created `generate_db_manifest.py` - SHA-256 hash and stats
2. ✅ Added `/api/cards/manifest` and `/api/cards/database` endpoints
3. ✅ Updated `workflow.sh` to generate DB and manifests
4. ✅ Created Room entities for `card_related_finishes` and `card_related_cards`
5. ✅ Added MIGRATION_3_4 (database v4)
6. ✅ Rewrote `CardSyncRepository.kt` for hash-based sync
7. ✅ Downloads entire DB when hash differs, merges while preserving user data
8. ✅ Updated CollectionViewModel for new sync approach
9. ✅ Regenerated bundled database with new tables (3923 cards, 973 finishes, 498 related)

### 🔜 Session 9: Polish & Optimization
1. 🖼️ UI/UX polish and animations
2. ⚡ Performance optimizations
3. 🧹 Clean up legacy files

---

## Technical Decisions to Make

### Database
- **Option 1**: Sync full card database from API (better offline, larger size)
- **Option 2**: On-demand loading (smaller, requires network)
- **Recommendation**: Full sync with periodic updates

### Image Strategy
- **Option 1**: Use get-diced.com URLs directly (smaller app, requires network)
- **Option 2**: Download and cache images (better offline)
- **Recommendation**: URL + aggressive caching (Coil handles this)

### API Authentication
- Does get-diced.com require auth?
- Rate limiting concerns?
- Document after inspecting codebase

---

## Questions for Tomorrow

1. **get-diced.com API**:
   - What endpoints exist?
   - Authentication required?
   - Rate limits?
   - CORS for mobile?

2. **Card Data Schema**:
   - What fields are available?
   - How are linked cards represented?
   - Image URL structure?

3. **Deck Export Format**:
   - Does get-diced.com have deck import/export already?
   - What format? (JSON, text list, custom)

4. **Deck Rules**:
   - Are there deck size limits?
   - Card quantity restrictions?
   - Format-specific rules?

---

## Success Metrics

After implementation:
- ✅ Fast card search (< 100ms)
- ✅ Smooth scrolling with images
- ✅ Offline usable (cached data)
- ✅ Deck export/import works
- ✅ Better UX than manual search in current app
- ✅ Deckbuilding is intuitive and fast

---

## Future Enhancements (Post-MVP)

- Deck statistics and analysis
- Card price tracking (if available)
- Deck sharing community features
- Advanced deck validation rules
- Multi-format support
- Card collection value tracking
- Trade/want list management
- Barcode scanning for inventory (if cards have barcodes)

---

## Current File Structure (Updated)

```
app/src/main/kotlin/com/srg/inventory/
├── api/                          ✅ Implemented
│   ├── GetDicedApi.kt           ✅ Retrofit service interface
│   ├── RetrofitClient.kt        ✅ API client singleton
│   ├── ApiModels.kt             ✅ Request/response models
│   ├── CardMapper.kt            ✅ DTO to entity mapper
│   └── CardSyncRepository.kt    ✅ Batch sync operations
├── data/                         ✅ Implemented
│   ├── Card.kt                  ✅ Card entity (from API)
│   ├── CardDao.kt               ✅ Card DAO
│   ├── Folder.kt                ✅ Folder entity
│   ├── FolderDao.kt             ✅ Folder DAO
│   ├── FolderCard.kt            ✅ Junction table
│   ├── FolderCardDao.kt         ✅ Junction DAO
│   ├── CollectionRepository.kt  ✅ Collection operations
│   ├── UserCard.kt              ✅ Legacy (migrated)
│   ├── UserCardDao.kt           ✅ Legacy DAO
│   ├── CardRepository.kt        ✅ Legacy repository
│   └── UserCardDatabase.kt      ✅ Room DB v2
├── ui/                           ✅ Implemented
│   ├── CollectionViewModel.kt   ✅ State management
│   ├── Navigation.kt            ✅ Navigation routes
│   ├── MainScreen.kt            ✅ Updated for folders
│   ├── FoldersScreen.kt         ✅ Folder list
│   ├── FolderDetailScreen.kt    ✅ Cards in folder
│   ├── AddCardToFolderScreen.kt ✅ Search with filters
│   ├── ManualAddScreen.kt       (legacy, unused)
│   ├── CollectionScreen.kt      (legacy, unused)
│   ├── CardViewModel.kt         (legacy, unused)
│   └── theme/                   ✅ Material 3 theme
├── deck/                         🔜 Next phase
│   ├── Deck.kt                  🔜 Deck entity
│   ├── DeckDao.kt               🔜 Deck DAO
│   ├── DeckCard.kt              🔜 Junction table
│   ├── DeckCardDao.kt           🔜 Junction DAO
│   └── DeckRepository.kt        🔜 Deck operations
└── MainActivity.kt               ✅ App entry point
```

---

## Notes

- **Keep**: User collection database and UI (still valuable)
- **Remove**: Everything camera/scanning related
- **Focus**: Make this the best SRG card search + deckbuilding app
- **Leverage**: Existing get-diced.com infrastructure instead of reinventing

---

## Current Status 🚀

### Completed ✅
- **Phase 1**: Camera scanning removal and cleanup
- **Phase 2**: get-diced.com API integration with folder-based collections
- **Enhanced Search**: Type-specific filters and real-time search
- **Database Migration**: v1 to v2 with data preservation
- **Offline-First**: Manual sync with progress tracking
- **Bundled Database**: ✨ NEW - App ships with 3,922 cards pre-loaded (1.6MB database in assets)

### Latest Session: Card Database Sync (Nov 20, 2025)
#### What Was Completed
1. **Server-Side Infrastructure**
   - `generate_db_manifest.py` - SHA-256 hash and stats
   - `/api/cards/manifest` and `/api/cards/database` endpoints
   - Updated `workflow.sh` for DB and manifest generation

2. **App-Side Sync Implementation**
   - Room entities for `card_related_finishes` and `card_related_cards`
   - MIGRATION_3_4 (database now at v4)
   - Rewrote `CardSyncRepository.kt` for hash-based sync
   - Downloads entire DB when hash differs
   - Merges card tables while preserving user data

3. **Database Stats**
   - 3923 cards
   - 973 related finish links
   - 498 related card links

4. **Key Feature**
   - User data (folders, decks) preserved during sync
   - Only card data tables are replaced

### Next Steps 🔜
**High Priority:**
1. **Search within folders** - Filter cards in collection folders
2. **Deck validation** - Check deck completeness and rules

**Future:**
3. **UI Polish** - Animations, transitions, performance
4. **Versioned Bundles** - Server-side versioning for DB + images
5. **Clean up legacy files** - Remove unused ManualAddScreen, CollectionScreen, CardViewModel

### Known Issues 🐛
- Legacy UI files (ManualAddScreen, CollectionScreen, CardViewModel) can be deleted once thoroughly tested

### Architecture Highlights 🏗️
- **Folder-based collections** allow same card in multiple folders with independent quantities
- **7 card types supported** with type-specific metadata and filters
- **Many-to-many relationship** between folders and cards via junction table
- **Offline-first** with bundled database (~3,922 cards) + images (3,481 thumbnails, 34MB)
- **Type-safe navigation** using Compose Navigation with 4-tab bottom nav
- **Reactive UI** using Flow and StateFlow
- **Pre-populated database** using Room's `.createFromAsset()` feature
- **Living database** - New cards added regularly (~10/month), sync keeps app current
- **Image loading** - Coil library with bundled assets → cached downloads → server fallback
- **Standalone card browsing** - View and search cards separately from adding to collection

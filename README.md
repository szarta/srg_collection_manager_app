# SRG Collection Manager

Android app for managing your SRG (Super Ring of Glory) wrestling card collection and building decks.

## Features

### ✅ Implemented
- **Bundled Database** - ✨ App ships with 3,923 cards pre-loaded (ready to use immediately!)
- **Card Database Sync** - Hash-based sync downloads only when server has updates (preserves user data)
- **Bundled Images** - ✨ 3,481 mobile-optimized card images (158MB) bundled with app for offline use
- **Card Images in Detail Views** - Full card images with stats, rules, and errata in detail dialogs
- **4-Tab Navigation** - Clean bottom nav: Collection | Decks | Viewer | Settings
- **Folder-Based Collection Management** - Organize cards in custom folders (Owned, Wanted, Trade, + custom)
- **Card Database Sync** - Sync from get-diced.com to get latest cards and updates (Settings screen)
- **Advanced Card Search** - Search with type-specific filters (attack type, play order, division, etc.)
- **Card Viewer** - Browse and view any card with full details (image, stats, rules)
- **Edit Quantity Dialog** - +/- buttons to adjust quantity, delete option to remove cards
- **Multi-Folder Support** - Same card can exist in multiple folders with independent quantities
- **Offline-First** - Works offline with bundled database + images, sync for latest updates
- **7 Card Types Supported** - MainDeck, SingleCompetitor, TornadoCompetitor, TrioCompetitor, Entrance, Spectacle, CrowdMeter
- **Image Loading** - Coil library with bundled mobile-optimized assets
- **Full Deckbuilding** - ✨ Complete slot-based deck editor with smart filtering
  - Deck folders (Singles, Tornado, Trios, Tag, custom)
  - Entrance, Competitor, Deck cards 1-30, Alternates
  - Smart card filtering by slot type and deck_card_number
  - Spectacle type selector (Newman/Valiant)
- **Deck CSV Export/Import** - Export decks to CSV, import from CSV files
- **Deck Sharing** - Share decks to get-diced.com, import from shareable URLs
- **QR Code Import/Export** - ✨ NEW! Generate QR codes for collections and decks, scan QR codes to import
  - Export collections/decks as QR codes with shareable URLs
  - Scan QR codes from app or get-diced.com articles
  - Full deck structure preservation (slots, spectacle type)
  - Works in both portrait and landscape orientations
  - Dialogs persist through screen rotation

**Note:** New cards are released regularly (~10/month) and the database is actively being updated. The bundled database provides a great starting point, but syncing is recommended to get the latest cards and updates.

### 🔜 Coming Soon
- **Folder Search** - Search within specific collection folders
- **Deck Validation** - Check deck completeness and rules

## Tech Stack

- **Kotlin** - Primary language (100% Kotlin codebase)
- **Jetpack Compose** - Modern UI toolkit with Material 3
- **Room Database** - Local data persistence with migrations
- **Coroutines & Flow** - Reactive async operations
- **Retrofit** - API integration with get-diced.com
- **Gson** - JSON parsing
- **Coil** - Async image loading with caching
- **Navigation Compose** - Type-safe navigation

## Integration with get-diced.com

This app integrates with [get-diced.com](https://get-diced.com) to:
- ✅ Sync comprehensive card database (~3,922 cards, growing monthly)
- ✅ Access all 7 card types with full metadata
- ✅ Search and filter with API-backed queries
- ✅ Load high-quality card images (WebP format, bundled + on-demand)
- ✅ Export/import decks via shared lists API
- ✅ Share decks with unique URLs
- ✅ QR code generation and scanning for collections and decks
- ✅ Scan QR codes from deck articles on get-diced.com

## Project Structure

```
app/src/main/kotlin/com/srg/inventory/
├── api/                          # API integration layer
│   ├── ApiModels.kt             # API response models
│   ├── GetDicedApi.kt           # Retrofit service interface
│   ├── RetrofitClient.kt        # API client singleton
│   ├── CardMapper.kt            # DTO to entity mapper
│   └── CardSyncRepository.kt    # Card sync operations
├── data/                         # Database entities, DAOs, and repositories
│   ├── Folder.kt                # Folder entity
│   ├── FolderDao.kt             # Folder DAO
│   ├── Card.kt                  # Card entity (from API)
│   ├── CardDao.kt               # Card DAO
│   ├── FolderCard.kt            # Junction table for many-to-many
│   ├── FolderCardDao.kt         # Junction table DAO
│   ├── UserCard.kt              # Legacy entity (migrated)
│   ├── UserCardDao.kt           # Legacy DAO
│   ├── UserCardDatabase.kt      # Room database (v2)
│   ├── CollectionRepository.kt  # Collection operations
│   └── CardRepository.kt        # Legacy repository
├── ui/                          # UI components and screens
│   ├── CollectionViewModel.kt   # ViewModel for folder-based collections
│   ├── Navigation.kt            # Navigation routes and NavHost
│   ├── MainScreen.kt            # 4-tab bottom navigation
│   ├── FoldersScreen.kt         # Collection tab - folder list
│   ├── FolderDetailScreen.kt    # Cards in folder screen
│   ├── AddCardToFolderScreen.kt # Card search with filters
│   ├── CardSearchScreen.kt      # Card Search tab - standalone browse
│   ├── DecksScreen.kt           # Decks tab - deck folders + DeckViewModel
│   ├── DeckListScreen.kt        # Decks in folder
│   ├── DeckEditorScreen.kt      # Full deck editor with card picker
│   ├── SettingsScreen.kt        # Settings tab - sync & config
│   ├── ManualAddScreen.kt       # Legacy search screen (unused)
│   ├── CollectionScreen.kt      # Legacy collection screen (unused)
│   ├── CardViewModel.kt         # Legacy ViewModel (unused)
│   └── theme/                   # Material 3 theme
├── utils/                       # Utility functions
│   └── ImageUtils.kt            # Image loading helpers
└── MainActivity.kt              # App entry point
```

## Database Schema (v4)

### Folders Table
```sql
CREATE TABLE folders (
    id TEXT PRIMARY KEY,            -- Folder ID (owned, wanted, trade, or UUID)
    name TEXT NOT NULL,             -- Display name
    is_default INTEGER NOT NULL,    -- 1 for Owned/Wanted/Trade, 0 for custom
    display_order INTEGER NOT NULL, -- Sort order
    created_at INTEGER NOT NULL     -- Timestamp
);
```

### Cards Table (synced from get-diced.com)
```sql
CREATE TABLE cards (
    db_uuid TEXT PRIMARY KEY,       -- Card UUID from API
    name TEXT NOT NULL,             -- Card name
    card_type TEXT NOT NULL,        -- MainDeckCard, SingleCompetitorCard, etc.
    rules_text TEXT,                -- Card rules
    errata_text TEXT,               -- Official errata
    is_banned INTEGER NOT NULL,     -- Banned status
    release_set TEXT,               -- Release set name
    tags TEXT,                      -- Comma-separated tags
    -- Competitor fields (nullable)
    power INTEGER, agility INTEGER, strike INTEGER,
    submission INTEGER, grapple INTEGER, technique INTEGER,
    division TEXT, gender TEXT,
    -- MainDeck fields (nullable)
    deck_card_number INTEGER, atk_type TEXT, play_order TEXT,
    synced_at INTEGER NOT NULL      -- Last sync timestamp
);
```

### Card Relationship Tables
```sql
CREATE TABLE card_related_finishes (
    card_uuid TEXT NOT NULL,        -- FK to cards.db_uuid
    finish_uuid TEXT NOT NULL,      -- FK to cards.db_uuid (foil/variant)
    PRIMARY KEY (card_uuid, finish_uuid)
);

CREATE TABLE card_related_cards (
    card_uuid TEXT NOT NULL,        -- FK to cards.db_uuid
    related_uuid TEXT NOT NULL,     -- FK to cards.db_uuid (linked card)
    PRIMARY KEY (card_uuid, related_uuid)
);
```

### FolderCards Junction Table
```sql
CREATE TABLE folder_cards (
    folder_id TEXT NOT NULL,        -- FK to folders.id
    card_uuid TEXT NOT NULL,        -- FK to cards.db_uuid
    quantity INTEGER NOT NULL,      -- Number of copies
    added_at INTEGER NOT NULL,      -- Timestamp
    PRIMARY KEY (folder_id, card_uuid),
    FOREIGN KEY (folder_id) REFERENCES folders(id) ON DELETE CASCADE,
    FOREIGN KEY (card_uuid) REFERENCES cards(db_uuid) ON DELETE CASCADE
);
```

## Building the App

1. **Prerequisites**:
   - Android Studio Ladybug or later (2024.2.1+)
   - JDK 21
   - Android SDK 34

2. **Bundle Images** (optional, images already bundled):
   ```bash
   IMAGE_SOURCE_DIR=~/data/srg_card_search_website/backend/app/images ./bundle_images.sh
   ```

3. **Build**:
   ```bash
   ./gradlew assembleDebug
   ```

4. **Run**:
   ```bash
   ./gradlew installDebug
   ```

## How to Use

### 1. First Time Setup
   - Launch the app
   - **3,922 cards + 3,481 images are pre-loaded and ready to use immediately!**
   - Navigate to **Settings** tab (bottom right)
   - **Recommended:** Tap **Sync from get-diced.com** to get latest cards
   - New cards are released regularly (~10/month), so periodic syncing keeps your database current

### 2. Organize Your Collection with Folders
   - View default folders: **Owned**, **Wanted**, **Trade**
   - Create custom folders with the **+** button
   - Tap any folder to view its contents
   - Delete custom folders (default folders cannot be deleted)

### 3. Add Cards to Folders
   - Open a folder → Tap **+** button
   - Search by card name or rules text
   - Use filters: Card Type, Attack Type, Play Order, Division
   - Select a card and enter quantity
   - Same card can be in multiple folders with different quantities

### 4. Manage Cards in Folders
   - Tap **🔍 Search** icon to view full card details (image, stats, rules)
   - Tap **✏️ Edit** icon to change quantity (+/- buttons) or remove card
   - Cards remain in database even when removed from all folders

### 5. Build Decks
   - Go to **Decks** tab → Select folder (Singles, Tornado, Trios, Tag)
   - Create a new deck with the **+** button
   - Tap deck to open editor
   - Add cards to slots: Entrance, Competitor, Deck 1-30, Alternates
   - Cards are smart-filtered by slot type (e.g., slot #5 shows only cards with deck_card_number=5)
   - Select spectacle type (Newman/Valiant) in top bar
   - **Export/Import**: Use toolbar icons for CSV export/import
   - **Share**: Share deck to get-diced.com and copy link to clipboard
   - **Import from URL**: Paste a get-diced.com shareable link to import

### 6. QR Code Sharing
   - **Export**: Tap 📱 icon in collection folders or deck editor to generate QR code
   - **Scan**: Go to **Scan** tab → Tap "Start Scanner" → Scan QR code
   - **Import**: After scanning, select destination folder and tap "Import"
   - **From Website**: Scan QR codes from deck articles on get-diced.com
   - QR codes work in both portrait and landscape orientations
   - Import dialogs persist through screen rotation

## Roadmap

See [PIVOT_PLAN.md](PIVOT_PLAN.md) for the full implementation plan.

### Phase 1: ✅ Cleanup (Completed)
- Removed camera scanning functionality
- Simplified architecture to Search + Collection
- Cleaned up OpenCV dependencies

### Phase 2: ✅ get-diced.com Integration (Completed)
- ✅ Retrofit API client implementation
- ✅ Card database sync with batch operations
- ✅ Folder-based collection system
- ✅ Enhanced search with type-specific filters
- ✅ Database migration from v1 to v2
- ✅ Manual sync with progress tracking
- ✅ Bundled database with 3,922 cards (offline-first)
- ✅ 4-tab bottom navigation (Collection | Decks | Viewer | Settings)
- ✅ Image loading with Coil
- ✅ Bundled mobile-optimized images (3,481 cards, 158MB)
- ✅ Card images in all detail dialogs
- ✅ Full card details view (image, stats, rules, errata)
- ✅ Edit quantity dialog with +/- buttons and delete

### Phase 3: ✅ Deckbuilding (Completed)
- ✅ Create/edit/delete decks with slot-based system
- ✅ Deck folders (Singles, Tornado, Trios, Tag)
- ✅ Smart card filtering by slot type and deck_card_number
- ✅ CSV export/import
- ✅ Share to get-diced.com and import from URLs
- ✅ QR code generation for collections and decks
- ✅ QR code scanning with import dialogs
- ✅ Multi-orientation support (portrait/landscape)
- ✅ Configuration change handling (rotation)

### Phase 4: 🔜 Collection Enhancements (Next)
- Folder search (filter cards within folders)
- Deck validation and statistics
- Bulk operations

### Phase 5: 🔜 UI/UX Polish
- Grid view for card browsing
- Improved animations and transitions
- Performance optimizations

## Disclaimer

**This is an unofficial fan project.** It is in no way supported or explicitly endorsed by the game or its creators. This project was created with permission from Steve Resk (SRG - Steve Resk Gaming).

## Acknowledgements

Special thanks to:

- **Steve Resk** - Creator of SRG Supershow, for granting permission for this project
- **SRG Universe team** - Artists, designers, and everyone who brings Supershow to life
- **The wrestlers** - Past, present, and future competitors in the ring
- **The Supershow community** - Players, collectors, and fans who keep the game thriving
- **SRGPC.net** - The original card database (https://www.srgpc.net/) that accelerated this project, and all the contributors who built and maintained it
- **Claude (Anthropic)** - AI pair programming partner for development

## Copyright & License

### Source Code
The source code for this application is licensed under the MIT License. See [LICENSE](LICENSE) file.

### Card Content
All SRG Supershow card names, images, rules text, and related game content are copyright **SRG Universe**. The card data and images are used with permission for this fan project.

### Trademarks
SRG, Supershow, and related marks are trademarks of SRG Universe. This project is not affiliated with or endorsed by SRG Universe.

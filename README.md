# SRG Collection Manager

Android app for managing your SRG (Super Ring of Glory) wrestling card collection and building decks.

## Features

### ✅ Implemented
- **Bundled Database** - ✨ App ships with 3,922 cards pre-loaded (ready to use immediately!)
- **Folder-Based Collection Management** - Organize cards in custom folders (Owned, Wanted, Trade, + custom)
- **Card Database Sync** - Sync from get-diced.com to get latest cards and updates
- **Advanced Card Search** - Search with type-specific filters (attack type, play order, division, etc.)
- **Multi-Folder Support** - Same card can exist in multiple folders with independent quantities
- **Offline-First** - Works offline with bundled database, sync for latest updates
- **7 Card Types Supported** - MainDeck, SingleCompetitor, TornadoCompetitor, TrioCompetitor, Entrance, Spectacle, CrowdMeter

**Note:** New cards are released regularly (~10/month) and the database is actively being updated. The bundled database provides a great starting point, but syncing is recommended to get the latest cards and updates.

### 🚧 In Progress
- **Navigation Restructure** - Moving to 4-tab bottom nav (Collection/Decks/Card Search/Settings)

### 🔜 Coming Soon
- **Versioned Database Bundles** - Download latest versioned DB + images bundle from get-diced.com
- **Deckbuilding** - Build and manage decks with smart suggestions
- **Deck Export/Import** - Share decks via get-diced.com API
- **Shared Lists** - Import collection/deck links from get-diced.com
- **Card Images** - Bundle or download card images

## Tech Stack

- **Kotlin** - Primary language (100% Kotlin codebase)
- **Jetpack Compose** - Modern UI toolkit with Material 3
- **Room Database** - Local data persistence with migrations
- **Coroutines & Flow** - Reactive async operations
- **Retrofit** - API integration with get-diced.com
- **Gson** - JSON parsing
- **Navigation Compose** - Type-safe navigation

## Integration with get-diced.com

This app integrates with [get-diced.com](https://get-diced.com) to:
- ✅ Sync comprehensive card database (~300-500 cards)
- ✅ Access all 7 card types with full metadata
- ✅ Search and filter with API-backed queries
- 🔜 Load high-quality card images (WebP format)
- 🔜 Export/import decks via shared lists API
- 🔜 Share collections and decks with unique URLs

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
│   ├── MainScreen.kt            # App entry point (updated)
│   ├── FoldersScreen.kt         # Folder list screen
│   ├── FolderDetailScreen.kt    # Cards in folder screen
│   ├── AddCardToFolderScreen.kt # Card search with filters
│   ├── ManualAddScreen.kt       # Legacy search screen (unused)
│   ├── CollectionScreen.kt      # Legacy collection screen (unused)
│   ├── CardViewModel.kt         # Legacy ViewModel (unused)
│   └── theme/                   # Material 3 theme
└── MainActivity.kt              # App entry point
```

## Database Schema (v2)

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

2. **Build**:
   ```bash
   ./gradlew assembleDebug
   ```

3. **Run**:
   ```bash
   ./gradlew installDebug
   ```

## How to Use

### 1. First Time Setup
   - Launch the app
   - **3,922 cards are pre-loaded and ready to use immediately!**
   - **Recommended:** Tap the **Refresh** button (top right) to sync latest cards from get-diced.com
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
   - Tap **Edit** icon to change quantity
   - Tap **Delete** icon to remove from folder
   - Cards remain in database even when removed from all folders

### 5. Build Decks *(coming soon)*
   - Create and name your deck
   - Search and add cards
   - Get smart suggestions for linked cards
   - Export/share via get-diced.com

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

### Phase 3: 🔜 Deckbuilding (Next)
- Create/edit/delete decks
- Smart card suggestions (linked cards, related finishes)
- Fast deck building tools
- Export/import via get-diced.com shared lists API
- Deck validation and statistics

### Phase 4: 🔜 UI/UX Polish
- Bundle card images into app
- Grid view for card browsing
- Improved animations and transitions
- Performance optimizations

## License

See [LICENSE](LICENSE) file.

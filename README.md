# SRG Collection Manager

Android app for managing your SRG (Super Ring of Glory) wrestling card collection and building decks.

## Features

- **Card Search** - Search and browse all SRG cards
- **Collection Management** - Track owned and wanted cards with quantities
- **Deckbuilding** *(coming soon)* - Build and manage decks with smart suggestions

## Tech Stack

- **Kotlin** - Primary language
- **Jetpack Compose** - Modern UI toolkit with Material 3
- **Room Database** - Local data persistence
- **Coroutines & Flow** - Reactive async operations
- **Retrofit** *(coming soon)* - API integration
- **Coil** *(coming soon)* - Image loading

## Integration with get-diced.com

This app integrates with [get-diced.com](https://get-diced.com) to:
- Pull comprehensive card database
- Load high-quality card images
- Export/import decks
- Sync across devices

## Project Structure

```
app/src/main/kotlin/com/srg/inventory/
├── data/                    # Database entities, DAOs, and repositories
│   ├── UserCard.kt         # User collection entity
│   ├── UserCardDao.kt      # DAO for user collection
│   ├── UserCardDatabase.kt # User collection database
│   └── CardRepository.kt   # Data repository
├── ui/                     # UI components and screens
│   ├── CardViewModel.kt    # ViewModel for state management
│   ├── MainScreen.kt       # Main navigation screen
│   ├── ManualAddScreen.kt  # Card search screen
│   ├── CollectionScreen.kt # Collection view screen
│   └── theme/              # Material 3 theme
└── MainActivity.kt         # App entry point
```

## User Collection Database

```sql
CREATE TABLE user_cards (
    card_id TEXT PRIMARY KEY,       -- Card identifier
    card_name TEXT NOT NULL,        -- Display name
    quantity_owned INTEGER,         -- Number owned
    quantity_wanted INTEGER,        -- Number wanted
    is_custom BOOLEAN,              -- True if custom entry
    added_timestamp INTEGER         -- When added
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

### 1. Search for Cards
   - Tap the **"Search"** tab
   - Type any part of a card name
   - See real-time suggestions from your collection
   - Add cards to owned or wanted lists

### 2. Manage Collection
   - Tap the **"Collection"** tab
   - Filter by **All** / **Owned** / **Wishlist**
   - Tap a card to edit quantities
   - Set both quantities to 0 to remove

### 3. Build Decks *(coming soon)*
   - Create and name your deck
   - Search and add cards
   - Get smart suggestions for linked cards
   - Export/share via get-diced.com

## Roadmap

See [PIVOT_PLAN.md](PIVOT_PLAN.md) for the full implementation plan.

### Phase 1: ✅ Cleanup
- Removed camera scanning functionality
- Simplified architecture to Search + Collection

### Phase 2: 🚧 get-diced.com Integration (Next)
- API client implementation
- Card database sync
- Image loading from get-diced.com
- Enhanced search with filters

### Phase 3: 🔜 Deckbuilding
- Create/edit/delete decks
- Smart card suggestions (linked cards)
- Fast deck building tools
- Export/import via get-diced.com API

## License

See [LICENSE](LICENSE) file.

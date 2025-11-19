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

## Phase 2: Integrate with get-diced.com 🎲

### Goals
- Pull card database from get-diced.com
- Display card images from get-diced.com
- Implement fast, rich card search
- Cache data for offline use

### Research First
1. **Inspect SRG codebase for get-diced.com**
   - API endpoints
   - Database schema
   - Image storage structure
   - Search implementation

### Implementation Tasks

#### 2.1: API Integration
1. **Create API client** (`app/src/main/kotlin/com/srg/inventory/api/`)
   - `GetDicedApiService.kt` (Retrofit interface)
   - `GetDicedRepository.kt` (data layer)
   - Define data models for API responses

2. **API Endpoints to Implement**
   - `GET /api/cards` - Fetch all cards
   - `GET /api/cards/search?q={query}` - Search cards
   - `GET /api/cards/{id}` - Get card details
   - `GET /api/images/{path}` - Get card images
   - (Document actual endpoints after inspecting codebase)

3. **Add Dependencies** (`app/build.gradle.kts`)
   - Retrofit for API calls
   - Coil for image loading
   - Moshi/Gson for JSON parsing

#### 2.2: Local Database
1. **Create Card Entity** (`app/src/main/kotlin/com/srg/inventory/data/`)
   - `Card.kt` - Full card data from API
   - Include: id, name, type, text, image_url, set, rarity, linked_cards, etc.

2. **Update Database**
   - `CardDao.kt` - CRUD operations
   - `AppDatabase.kt` - Include Card table
   - Migration strategy from old schema

3. **Caching Strategy**
   - Download card database on first launch
   - Periodic sync (daily/weekly)
   - Offline-first with cache

#### 2.3: Enhanced Search UI
1. **Redesign Search Screen** (`app/src/main/kotlin/com/srg/inventory/ui/`)
   - Replace `ManualAddScreen.kt` with `CardSearchScreen.kt`
   - Real-time search with debouncing
   - Filter by: type, set, rarity, color
   - Sort by: name, set, rarity

2. **Card Detail View**
   - `CardDetailScreen.kt` - Full card view
   - Display high-res image from get-diced.com
   - Show all card attributes
   - Show linked cards (clickable)
   - Actions: Add to Collection, Add to Deck

3. **Image Loading**
   - Use Coil to load images from get-diced.com
   - Cache images locally
   - Placeholder/error images

**Estimated Time**: 3-4 hours

---

## Phase 3: Deckbuilding Feature 🃏

### Goals
- Create/edit/delete decks
- Add cards to decks with quantities
- Smart card suggestions
- Export/import decks via get-diced.com API

### Tasks

#### 3.1: Data Layer
1. **Create Deck Entities** (`app/src/main/kotlin/com/srg/inventory/data/`)
   - `Deck.kt` - id, name, description, created_date, modified_date
   - `DeckCard.kt` - deck_id, card_id, quantity, notes
   - Many-to-many relationship

2. **Create DAOs**
   - `DeckDao.kt` - CRUD for decks
   - `DeckCardDao.kt` - Manage deck contents
   - Query: Get deck with all cards

3. **Repository**
   - `DeckRepository.kt` - Business logic
   - Add/remove cards from deck
   - Duplicate deck
   - Validate deck (future: deck rules)

#### 3.2: Deckbuilding UI
1. **Deck List Screen** (`app/src/main/kotlin/com/srg/inventory/ui/deck/`)
   - `DeckListScreen.kt` - Show all decks
   - Create new deck dialog
   - Delete deck confirmation
   - Click to edit deck

2. **Deck Editor Screen**
   - `DeckEditorScreen.kt` - Main deckbuilding interface
   - Top: Deck name, description, card count
   - Search bar to add cards
   - Card list with quantities
   - Remove cards, adjust quantities

3. **Smart Features**
   - Auto-suggest linked cards (from card data)
   - "Add playset" button (add 4 copies)
   - Quick filters: "Show only cards I own"
   - Visual deck breakdown (by type, color, etc.)

#### 3.3: Support Rules for Faster Deck Building
1. **Linked Card Suggestions**
   - When adding a card, suggest linked cards
   - "Players who added X also added Y"
   - Quick-add button for suggestions

2. **Deck Templates**
   - Pre-built starter decks
   - "Clone and customize" feature

3. **Batch Operations**
   - Add multiple cards at once
   - Import from text list

#### 3.4: Export/Import via get-diced.com
1. **Export Deck**
   - `POST /api/decks/export` - Upload deck to get-diced.com
   - Returns shareable link
   - Share via Android share sheet

2. **Import Deck**
   - `GET /api/decks/{share_id}` - Download deck from link
   - Parse and import to local database
   - Handle missing cards gracefully

3. **Deck Sync** (Optional Future)
   - Sync decks across devices
   - Cloud backup

**Estimated Time**: 4-5 hours

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

## Implementation Order (Tomorrow's Session)

### Session 1: Cleanup (Start Here)
1. ✅ Remove camera scanning UI and code
2. ✅ Remove OpenCV and matching utilities
3. ✅ Clean up dependencies and unused files
4. ✅ Test app still runs with simplified architecture

### Session 2: Get-Diced Integration
1. 🔍 Inspect get-diced.com codebase
2. 📝 Document API endpoints and data models
3. 🔌 Implement API client and repository
4. 💾 Update database schema for full card data
5. 🖼️ Implement image loading

### Session 3: Enhanced Search
1. 🔍 Build new search UI
2. 🎯 Add filters and sorting
3. 📄 Create card detail view
4. 🔗 Implement linked card navigation

### Session 4: Deckbuilding
1. 💾 Create deck data layer
2. 📝 Build deck list screen
3. ✏️ Build deck editor
4. 🔗 Implement linked card suggestions
5. 📤 Export/import via API

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

## File Structure After Pivot

```
app/src/main/kotlin/com/srg/inventory/
├── api/
│   ├── GetDicedApiService.kt
│   ├── GetDicedRepository.kt
│   └── models/
│       ├── CardResponse.kt
│       └── DeckResponse.kt
├── data/
│   ├── Card.kt
│   ├── CardDao.kt
│   ├── Deck.kt
│   ├── DeckDao.kt
│   ├── DeckCard.kt
│   ├── DeckCardDao.kt
│   ├── UserCard.kt (keep for collection)
│   └── AppDatabase.kt
├── ui/
│   ├── MainScreen.kt (updated navigation)
│   ├── search/
│   │   ├── CardSearchScreen.kt
│   │   ├── CardDetailScreen.kt
│   │   └── CardSearchViewModel.kt
│   ├── collection/
│   │   └── CollectionScreen.kt (keep)
│   ├── deck/
│   │   ├── DeckListScreen.kt
│   │   ├── DeckEditorScreen.kt
│   │   └── DeckViewModel.kt
│   └── components/
│       ├── CardGridItem.kt
│       └── CardListItem.kt
└── utils/
    └── ImageLoader.kt (Coil wrapper)
```

---

## Notes

- **Keep**: User collection database and UI (still valuable)
- **Remove**: Everything camera/scanning related
- **Focus**: Make this the best SRG card search + deckbuilding app
- **Leverage**: Existing get-diced.com infrastructure instead of reinventing

---

## Ready to Start Tomorrow! 🚀

Begin with **Phase 1: Remove Scanning Capability** to clean up the codebase, then inspect the get-diced.com codebase to understand the API structure before implementing integration.

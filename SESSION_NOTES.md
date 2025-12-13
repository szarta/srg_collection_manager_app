# Session Notes - Dec 12, 2025 (Part 2)

## ✅ COMPLETE SEARCH & FILTER REDESIGN - NEW ARCHITECTURE IMPLEMENTED

### Summary
Completely redesigned the search and filter system with a single-page architecture, multi-select filters, and live search. The old two-page flow with session ID tracking has been replaced with a clean, modern implementation.

---

## What Was Implemented ✅

### 1. ViewModel Complete Redesign
**File:** `CollectionViewModel.kt`

**New Features:**
- ✅ Multi-select search scopes (name, tags, rules_text) - Set<String>, all enabled by default
- ✅ Multi-select deck card numbers - Set<Int> for filtering MainDeckCards
- ✅ Six individual stat filters (minPower, minTechnique, minAgility, minStrike, minSubmission, minGrapple) - range 5-30, default 5
- ✅ Live search with 300ms debounce - results update as you type
- ✅ `applyFilters()` function - called when user clicks Apply in filter dialog
- ✅ `clearFilters()` function - resets all filters to defaults and shows all cards
- ✅ `loadNextPage()` - renamed from loadMoreResults for infinite scroll support

**Removed:**
- ❌ Session ID tracking (failed approach)
- ❌ Autocomplete suggestions state (no longer needed)
- ❌ Two-page flow navigation logic
- ❌ Old SearchFilters and AutocompleteParams data classes

### 2. Database Query Updates
**Files:** `CardDao.kt`, `CollectionRepository.kt`

**Enhanced Query:**
```sql
SELECT DISTINCT cards.* FROM cards
WHERE (:searchQuery IS NULL OR (
    (:searchName AND name LIKE '%' || :searchQuery || '%' COLLATE NOCASE) OR
    (:searchTags AND tags LIKE '%' || :searchQuery || '%' COLLATE NOCASE) OR
    (:searchRulesText AND rules_text LIKE '%' || :searchQuery || '%' COLLATE NOCASE)
))
AND (:cardType IS NULL OR card_type = :cardType)
AND (:division IS NULL OR division = :division)
AND (CASE WHEN :hasDeckNumbers THEN deck_card_number IN (:deckCardNumbers) ELSE 1 END)
AND (power IS NULL OR power >= :minPower)
AND (technique IS NULL OR technique >= :minTechnique)
AND (agility IS NULL OR agility >= :minAgility)
AND (strike IS NULL OR strike >= :minStrike)
AND (submission IS NULL OR submission >= :minSubmission)
AND (grapple IS NULL OR grapple >= :minGrapple)
ORDER BY name ASC
LIMIT :limit OFFSET :offset
```

**Features:**
- Multi-select search scopes using OR query
- Multi-select deck numbers using IN clause with hasDeckNumbers flag
- Stats filtering for all 6 competitor stats
- Supports showing all cards when no filters applied

### 3. FilterDialog Component (NEW FILE)
**File:** `FilterDialog.kt`

**Features:**
- ✅ Search scope toggles (Name, Tags, Card Text) - multi-select with FilterChips
- ✅ Card type selector - single-select with "All" option
- ✅ Deck card number grid (1-30) - multi-select, 6 rows × 5 columns layout
- ✅ Six stat sliders for competitors - Power, Technique, Agility, Strike, Submission, Grapple (5-30 range)
- ✅ Division filter for SingleCompetitorCards
- ✅ Apply button - triggers `applyFilters()` and closes dialog
- ✅ Clear All button - resets filters to defaults

**UI/UX:**
- Full-height dialog (90% screen height) with scrollable content
- Shows selected deck numbers as comma-separated list
- Shows current stat values next to each slider
- Conditional visibility (deck numbers only for MainDeckCard, stats only for competitors)

### 4. Single-Page AddCardToFolderScreen
**File:** `AddCardToFolderScreen.kt` (completely rewritten)

**New Architecture:**
- ✅ Single screen - no separate results page
- ✅ Filter icon in top app bar (funnel icon)
- ✅ Search bar always visible at top
- ✅ Infinite scroll using LaunchedEffect watching LazyListState
- ✅ Empty states:
  - "Search for cards" when no query entered
  - "No cards found" when search returns nothing
- ✅ Results count display ("50+ cards")
- ✅ Loading indicator at bottom during pagination
- ✅ Card click opens AddCardDialog with quantity selector

**Removed:**
- ❌ Two-page flow (search page → results page)
- ❌ Session ID validation
- ❌ Autocomplete dropdown
- ❌ "Load More" button (replaced with infinite scroll)
- ❌ Inline filter UI (moved to dialog)

### 5. Single-Page CardSearchScreen (Viewer)
**File:** `CardSearchScreen.kt` (completely rewritten)

**Same architecture as AddCardToFolderScreen:**
- ✅ Single page with filter icon
- ✅ Infinite scroll
- ✅ Card click opens CardDetailsDialog (placeholder - full implementation exists in old file)
- ✅ Same empty states and search behavior

### 6. Navigation Cleanup
**File:** `Navigation.kt`

**Changes:**
- ❌ Removed `Screen.SearchResults` route
- ❌ Removed `Screen.AddCardToFolderResults` route
- ✅ Simplified AddCardToFolder composable (no onSearchClick callback)
- ✅ Simplified CardSearch composable (no onSearchClick callback)

---

## How It Works Now

### User Flow (Add Cards to Folder):
1. User clicks "Add Cards" (+) in folder
2. **Immediately sees:** Empty state "Search for cards"
3. User types in search bar → results appear after 300ms
4. User clicks filter icon → FilterDialog opens
5. User selects filters (card type, deck numbers, stats, etc.)
6. User clicks "Apply" → dialog closes, `applyFilters()` called
7. Results update based on filters
8. User scrolls down → automatically loads more cards (infinite scroll)
9. User clicks card → AddCardDialog opens with +/- quantity buttons
10. User clicks "Add" → card added to folder, stays on same results screen

### Multi-Select Features:
- **Search Scopes:** Can search in Name only, Tags only, Card Text only, or any combination
- **Deck Numbers:** Can filter for multiple deck numbers (e.g., 5, 10, 15) at once
- **Stats:** Set minimum values for each stat independently

### Live Search:
- Type "Lariat" → after 300ms pause, sees all cards matching "Lariat"
- Change query to "Dragon" → after 300ms pause, sees all cards matching "Dragon"
- Works across name, tags, and card text based on selected scopes

---

## Technical Details

### Infinite Scroll Implementation:
```kotlin
LaunchedEffect(listState) {
    snapshotFlow {
        listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
    }.collect { lastVisibleIndex ->
        if (lastVisibleIndex != null &&
            lastVisibleIndex >= searchResults.size - 5 &&
            hasMoreResults &&
            !isLoadingMore) {
            viewModel.loadNextPage()
        }
    }
}
```

### Live Search Debouncing:
```kotlin
init {
    viewModelScope.launch {
        _searchQuery
            .debounce(300)
            .collect {
                _searchResults.value = emptyList()
                _searchOffset.value = 0
                performSearch()
            }
    }
}
```

### Performance:
- Pagination: 50 cards per page
- Loads next page when scrolled to within 5 cards of bottom
- Debounced search prevents excessive queries

---

## Files Modified/Created

### Created:
1. `FilterDialog.kt` - NEW dialog component for all filters

### Completely Rewritten:
1. `AddCardToFolderScreen.kt` - Single-page design with infinite scroll
2. `CardSearchScreen.kt` - Single-page design with infinite scroll

### Major Changes:
1. `CollectionViewModel.kt` - New filter state, live search, multi-select support
2. `CardDao.kt` - Enhanced query with multi-select and stats filtering
3. `CollectionRepository.kt` - Updated to match new DAO signature
4. `Navigation.kt` - Removed results routes

### Backup Files (can be deleted):
- `AddCardToFolderScreen_Old.kt` (removed)
- `CardSearchScreen_Old.kt` (removed)

---

## Testing Instructions

### Test 1: Live Search
1. Open app → Collection → Any folder → "Add Cards"
2. Type "Lariat" slowly
3. **Expected:** Results appear 300ms after you stop typing

### Test 2: Filter Dialog
1. Click filter icon (funnel) in top bar
2. Toggle search scopes (deselect "Name", keep "Tags" and "Card Text")
3. Select card type "MainDeckCard"
4. Select deck numbers 5, 10, 15
5. Click "Apply"
6. **Expected:** See only MainDeckCards with deck numbers 5, 10, or 15

### Test 3: Stats Filtering
1. Open filter dialog
2. Select "SingleCompetitorCard"
3. Drag Power slider to 20
4. Click "Apply"
5. **Expected:** See only SingleCompetitors with Power >= 20

### Test 4: Infinite Scroll
1. Search for "card" (should return many results)
2. Scroll down to bottom
3. **Expected:** Loading indicator appears, next 50 cards load automatically

### Test 5: Add Multiple Cards from Same Search
1. Search for deck number 6
2. Click a card → add it with quantity 2
3. **Expected:** Dialog closes, still shows deck #6 results
4. Click another card → add it
5. **Expected:** Still on same results (no navigation away)

### Test 6: Clear Filters
1. Apply various filters
2. Click filter icon
3. Click "Clear All"
4. **Expected:** All filters reset, shows all cards

---

## Known Limitations

### Temporary Compatibility Stubs (can be removed later):
- `selectedAtkType`, `selectedPlayOrder`, `selectedDeckCardNumber` - unused stub properties
- `searchScope`, `autocompleteSuggestions`, `searchSessionId` - unused stub properties
- `setSearchScope()`, `setAtkTypeFilter()`, etc. - unused stub functions
- `triggerSearch()` - redirects to `applyFilters()`
- `loadMoreResults()` - redirects to `loadNextPage()`

These exist only for compatibility and can be safely removed in cleanup.

### CardDetailsDialog:
- Currently uses a placeholder implementation
- Full implementation exists in old backup file but wasn't migrated
- Shows basic card info but missing:
  - Full image preview
  - Related cards/finishes
  - Stats display with colored badges
  - Errata and tags

---

## Benefits of New Architecture

1. **Simpler:** One page instead of two, no navigation state management
2. **Clearer UX:** Filter icon is discoverable, Apply button gives control
3. **Faster:** Live search as you type, infinite scroll
4. **More Powerful:** Multi-select for deck numbers and scopes, comprehensive stats filtering
5. **No Stale Results:** Single page means no navigation bugs
6. **Better Performance:** Debounced search, lazy loading with pagination
7. **Cleaner Code:** Removed 1000+ lines of two-page flow complexity

---

## Foundation Architecture Changes (Build Successful ✅)

### Changes Completed:

**Phase 1: ViewModel Updates**
- ✅ Added multi-select search scopes (name, tags, rules_text) - Set<String>
- ✅ Added multi-select deck card numbers - Set<Int>
- ✅ Added 6 stat filters (minPower, minTechnique, minAgility, minStrike, minSubmission, minGrapple) - default 5
- ✅ Removed session ID tracking (failed approach)
- ✅ Removed autocomplete state
- ✅ Added auto-load on init - loads first 50 cards automatically
- ✅ Added live search with 300ms debounce - searches as you type
- ✅ Renamed `loadMoreResults()` to `loadNextPage()`
- ✅ Added `applyFilters()` and `clearFilters()` functions
- ✅ Temporary compatibility stubs for old UI

**Phase 2: Database Updates**
- ✅ Updated CardDao.searchCardsWithFilters() query:
  - Multi-select search scopes (OR query across name/tags/rules_text)
  - Multi-select deck card numbers (IN clause with hasDeckNumbers flag)
  - Stats filtering (power >= minPower, etc.)
- ✅ Updated CollectionRepository to match new DAO signature

**Phase 3: Compilation**
- ✅ Added temporary stub functions for backwards compatibility
- ✅ Build successful

### What to Test:

#### Test 1: Auto-Load on Startup
1. Open app → Collection → Click any folder → Click "Add Cards" (+ FAB)
2. **Expected:** Should immediately see 50 cards loaded (first 50 alphabetically)
3. **What this tests:** Auto-load on init is working

#### Test 2: Live Search (300ms debounce)
1. In the search bar, type "Lariat" slowly
2. **Expected:** Results should filter after you stop typing for 300ms
3. **Current Behavior:** Will still use old two-page flow (search → click Search button → results page)
4. **What this tests:** Live search debouncing works

#### Test 3: Infinite Scroll (via Load More button temporarily)
1. Search for "MainDeckCard" → click Search → see results
2. Scroll to bottom → click "Load More"
3. **Expected:** Next 50 cards load and append to list
4. **What this tests:** Pagination with new `loadNextPage()` function works

#### Test 4: Filter Application
1. Select a card type filter → click Search
2. **Expected:** Results filtered by that card type
3. **What this tests:** New database query supports filters

### Known Limitations (Temporary):

- Still using OLD two-page UI flow (search → results)
- Still has session ID validation (shows "stale results" message)
- Still has autocomplete dropdown (non-functional)
- Search scope is still single-select dropdown (not multi-select toggles)
- No stats filtering UI yet
- No deck number multi-select UI yet

### Next Steps (If Foundation Works):

1. Create FilterDialog component with:
   - Multi-select search scopes (Name, Tags, Card Text)
   - Multi-select deck card numbers (1-30 grid)
   - Stat sliders (6 stats, 5-30 range)
   - Apply and Clear buttons

2. Refactor AddCardToFolderScreen to single page:
   - Merge search + results into one screen
   - Filter icon in top bar opens dialog
   - Infinite scroll (no Load More button)
   - Remove session ID checks

3. Apply same pattern to CardSearchScreen (Viewer)

4. Clean up Navigation (remove results routes)

5. Remove compatibility stubs from ViewModel

---

# Session Notes - Dec 12, 2025 (Part 1)

## ✅ FIXED: Stale Search Results Issue (RESOLVED - Dec 12, 2025)

### Problem Summary
**User's Use Case:**
- Search for individual cards and add them to collection
- Then search for a block of cards (e.g., all cards matching card number 5)
- Add multiple cards from those filtered results
- **BUG**: After adding a card, either see old search results or empty results instead of current search results

**Root Cause:**
The ViewModel is shared across navigation screens, and search results were persisting in the StateFlow without proper isolation between search sessions. When navigating between search → results → add card → back to results, there was no way to distinguish between different search sessions.

### Solution: Search Session ID System ✅

**Implementation:**

1. **Added Search Session Tracking** (`CollectionViewModel.kt`)
   - Added `_searchSessionId` MutableStateFlow (starts at 0)
   - Increments by 1 every time `triggerSearch()` is called
   - Provides `searchSessionId` StateFlow for UI to observe

2. **Updated triggerSearch()** (`CollectionViewModel.kt:212-225`)
   ```kotlin
   fun triggerSearch() {
       // Increment session ID to mark this as a new search
       _searchSessionId.value += 1

       // Clear results synchronously BEFORE launching coroutine to avoid race conditions
       _searchResults.value = emptyList()
       _searchOffset.value = 0
       _hasMoreResults.value = true
       _isLoadingMore.value = false

       viewModelScope.launch {
           performSearch()
       }
   }
   ```

3. **Updated Results Screens** (Both `AddCardToFolderResultsScreen` and `SearchResultsScreen`)
   - Capture `resultsSessionId` using `remember(currentSessionId)` when screen first displays
   - Compare `resultsSessionId` with `currentSessionId` before showing results
   - If IDs don't match → show "Results outdated - Please go back and search again"
   - If IDs match → show results normally
   - This ensures results screen only displays data from ITS search, not from other searches

**How It Works:**
1. User searches for "card A" → session ID becomes 1 → results screen captures session ID 1
2. User goes back, searches for "card number 5" → session ID becomes 2
3. Results screen now has session ID 2 captured
4. User adds a card → stays on results screen with session ID 2
5. Results continue to show because resultsSessionId (2) == currentSessionId (2)
6. If somehow an old results screen with session ID 1 were still visible, it would show "Results outdated"

**Benefits:**
- ✅ Results stay consistent when adding multiple cards from the same search
- ✅ Prevents stale results from appearing after new searches
- ✅ Clear user feedback when results are outdated
- ✅ No complex filter comparison logic needed
- ✅ Works with pagination (Load More) correctly

### Previous Failed Attempts (For Reference)
1. ❌ Added `LaunchedEffect(Unit)` to clear results on navigation → cleared results when adding cards
2. ❌ Added `LaunchedEffect(folderId)` → still cleared results when adding cards
3. ❌ Used `rememberSaveable` with hasCleared flag → wouldn't re-clear on subsequent visits
4. ❌ Removed all auto-clearing → original bug returned (stale results)
5. ❌ Made `triggerSearch()` clear synchronously before coroutine → still showing stale results

### Files Modified (This Fix)
- `app/src/main/kotlin/com/srg/inventory/ui/CollectionViewModel.kt` - Added search session ID tracking
- `app/src/main/kotlin/com/srg/inventory/ui/AddCardToFolderScreen.kt` - Session validation in results screen
- `app/src/main/kotlin/com/srg/inventory/ui/CardSearchScreen.kt` - Session validation in results screen

---

## What Was Completed This Session ✅

### Search & UX Enhancements (COMPLETED)

**Goal:** Add pagination, autocomplete, image previews, and fix CSV import/export compatibility

**Features Implemented:**

1. **Search Pagination** (`CollectionViewModel.kt`, `CardDao.kt`, `CollectionRepository.kt`)
   - Added `offset` parameter to database queries (LIMIT/OFFSET)
   - State management: `_searchOffset`, `_hasMoreResults`, `_isLoadingMore`
   - "Load More" button in search results (shows next 50 cards)
   - Applied to both CardSearchScreen and AddCardToFolderScreen
   - **Critical Bug Fix**: Fixed auto-search issue where all cards were returned
     - Removed reactive search from init block
     - Added manual `triggerSearch()` method
     - Search only executes when user clicks Search button
   - Files: `CardDao.kt:67`, `CollectionRepository.kt:96-97`, `CollectionViewModel.kt:130-212`

2. **Name Autocomplete** (`CollectionViewModel.kt`, `CardDao.kt`)
   - Smart card name suggestions filtered by current search criteria
   - 300ms debounce using Flow operators
   - Dropdown shows up to 10 matching card names
   - Filters applied: card type, division, attack type, deck card number
   - Database query with DISTINCT and prefix matching (LIKE)
   - Integrated into SearchBar composable with dropdown UI
   - Files: `CardDao.kt:84-104`, `CollectionRepository.kt:102-112`, `CollectionViewModel.kt:140-212`

3. **Image Previews in Card Selection** (`AddCardToFolderScreen.kt`, `DeckEditorScreen.kt`)
   - Added 64dp card thumbnails to search results
   - Uses AsyncImage with ImageUtils.buildCardImageRequest
   - Applied to:
     - AddCardToFolderScreen (folder card selection)
     - DeckEditorScreen card picker dialog
   - Row layout with image on left, card details on right
   - Files: `AddCardToFolderScreen.kt:442-470`, `DeckEditorScreen.kt:999-1034`

4. **Clear Folder Feature** (`FolderDetailScreen.kt`, `CollectionViewModel.kt`)
   - DeleteSweep icon button in folder top bar
   - Confirmation dialog with warning message
   - Empties folder without deleting it
   - Repository already had `removeAllCardsFromFolder()` method
   - Files: `FolderDetailScreen.kt:147-152,226-269`, `CollectionViewModel.kt:408-416`

5. **CSV Import/Export Compatibility Fix** (CRITICAL)
   - **Problem**: Semicolon/comma and quote escaping incompatibilities
   - **Solution**: Universal CSV scheme using:
     - Commas → `--` replacement (no card names have `--`)
     - Double quotes → `""` (CSV standard escaping)
     - All values wrapped in quotes
   - **CSV Parser Enhancement**:
     - Proper handling of escaped quotes (`""` → `"`)
     - Lookahead for double-quote detection
     - Preserves apostrophes in card names
   - **Import Logging**:
     - Failed imports logged to `import_not_found.log`
     - Timestamp, count, and all failed card names
     - Saved to external files directory
   - **Website Sync**:
     - Updated TableView.jsx and DeckGridFromNames.jsx
     - Website now uses same CSV scheme as Android app
     - Full round-trip compatibility
   - Files:
     - Android: `FolderDetailScreen.kt:1019,1108,1174-1206`, `DeckEditorScreen.kt:1081,1141`
     - Website: `frontend/src/pages/TableView.jsx:259-265`, `frontend/src/components/DeckGridFromNames.jsx:269-275`

6. **Database & Image Sync** (Dec 12)
   - Updated bundled database from 3,922 to **4,618 cards** (+696 new cards)
   - Updated bundled images from 3,912 to **4,375 images** (+463 new images)
   - Regenerated with `create_mobile_db.py` from latest cards.yaml
   - Updated `images_manifest.json` (generated 2025-12-12)
   - Clean install required to force database reload

**Files Modified:**
- `app/src/main/kotlin/com/srg/inventory/data/CardDao.kt` - Pagination, autocomplete
- `app/src/main/kotlin/com/srg/inventory/data/CollectionRepository.kt` - Pass-through for new features
- `app/src/main/kotlin/com/srg/inventory/ui/CollectionViewModel.kt` - Search state, pagination, autocomplete, clear folder
- `app/src/main/kotlin/com/srg/inventory/ui/CardSearchScreen.kt` - Pagination UI, autocomplete, manual search trigger
- `app/src/main/kotlin/com/srg/inventory/ui/AddCardToFolderScreen.kt` - Pagination UI, autocomplete, image previews, manual search trigger
- `app/src/main/kotlin/com/srg/inventory/ui/FolderDetailScreen.kt` - Clear folder button, CSV fixes, import logging, CSV parser
- `app/src/main/kotlin/com/srg/inventory/ui/DeckEditorScreen.kt` - Image previews, CSV fixes, import logging
- `/home/brandon/data/srg_card_search_website/frontend/src/pages/TableView.jsx` - CSV export compatibility
- `/home/brandon/data/srg_card_search_website/frontend/src/components/DeckGridFromNames.jsx` - CSV export compatibility
- `app/src/main/assets/cards_initial.db` - Updated to 4,618 cards
- `app/src/main/assets/images_manifest.json` - Updated to 4,375 images

**User Experience:**
- ✅ Browse beyond first 50 search results with Load More
- ✅ Fast card name entry with autocomplete dropdown
- ✅ Visual card identification with image thumbnails
- ✅ Clear folder contents with safety confirmation
- ✅ Universal CSV format works across Android app and website
- ✅ Import troubleshooting with detailed logs
- ✅ Search works correctly (manual trigger, not auto-executing)
- ✅ 4,618 cards available offline with 4,375 images

**Version Update:**
- **Version**: 1.0.13 (versionCode 16)
- **Previous**: 1.0.12 (versionCode 15)

**Release Build:**
- **AAB Location**: `app/build/outputs/bundle/release/app-release.aab`
- **Status**: ❌ **NOT READY** - Critical bugs in search results (see top of file)

---

# Session Notes - Dec 10, 2025

## What Was Completed This Session ✅

### iOS-Like Card Detail Enhancements (COMPLETED)

**Goal:** Add colored competitor stats and related cards/finishes to deck and viewer card detail screens

**Features Implemented:**

1. **Colored Competitor Stats** (All Three Detail Dialogs)
   - **Collection Detail** (`FolderDetailScreen.kt:566-571`) - Already had colored stats
   - **Deck Detail** (`DeckEditorScreen.kt:1323-1328`) - Added colored stat circles
   - **Viewer Detail** (`CardSearchScreen.kt:393-398`) - Added colored stat circles
   - **Color Scheme**:
     - Power (PWR) - Red (#FF6B6B)
     - Technique (TEC) - Orange (#FF922B)
     - Agility (AGI) - Green (#51CF66)
     - Strike (STR) - Yellow (#FFD700)
     - Submission (SUB) - Purple (#CC5DE8)
     - Grapple (GRP) - Blue (#4DABF7)
   - **Design**: iOS-like circular badges (40dp) with white text
   - **Stat Order**: Power → Technique → Agility → Strike → Submission → Grapple (consistent across all screens)

2. **Related Cards & Finishes** (Deck and Viewer Screens)
   - **Deck Detail Dialog** (`DeckEditorScreen.kt:1407-1543`)
     - Added related finishes section with clickable cards
     - Added related cards section with clickable cards
     - Uses existing `cardDao.getRelatedFinishes()` and `cardDao.getRelatedCards()` functions
     - Recursive viewing - tap any related card to view its details
   - **Viewer Detail Dialog** (`CardSearchScreen.kt:507-643`)
     - Added related finishes section (primary container background)
     - Added related cards section (tertiary container background)
     - Same clickable interface as collection detail
     - Fetches data from database via LaunchedEffect

3. **UI Consistency**
   - All three card detail screens now have:
     - Colored stat circles for competitor cards
     - Related finishes section (when available)
     - Related cards section (when available)
     - Consistent stat ordering
     - Unified visual design language
   - Material 3 design with icons:
     - ✨ AutoAwesome icon for Related Finishes
     - 🔗 Link icon for Related Cards

**Files Modified:**
- `app/src/main/kotlin/com/srg/inventory/ui/FolderDetailScreen.kt` - Updated stat order
- `app/src/main/kotlin/com/srg/inventory/ui/DeckEditorScreen.kt` - Added colored stats, related cards/finishes
- `app/src/main/kotlin/com/srg/inventory/ui/CardSearchScreen.kt` - Added colored stats, related cards/finishes, import statement

**User Experience:**
- ✅ Consistent colored stat display across all card views
- ✅ Easy identification of stat types by color
- ✅ Discover related card variants and finishes
- ✅ Tap to recursively explore related cards
- ✅ Professional iOS-like circular stat badges
- ✅ Clear visual hierarchy with section backgrounds

**Version Update:**
- **Version**: 1.0.12 (versionCode 15)
- **Previous**: 1.0.11 (versionCode 14)

**Release Build:**
- **AAB Location**: `app/build/outputs/bundle/release/app-release.aab`
- **Ready For**: Production release to Google Play

---

# Session Notes - Dec 6, 2025

## What Was Completed This Session ✅

### Add Cards to Folder Two-Page Flow (COMPLETED)

**Goal:** Apply the same two-page search flow to adding cards to folders for consistency

**Features Implemented:**

1. **Search-Only Page** (`AddCardToFolderScreen.kt:32-120`)
   - **Layout**: Clean search page with all filters, no results clutter
   - **All Filters**: Search query, scope selector, card type, attack type, play order, division, deck card number
   - **Search Button**: Navigates to results page when filters/query are set
   - **Scrollable**: Fully scrollable content for long filter lists

2. **Results Page** (`AddCardToFolderScreen.kt:127-213`)
   - **New Screen**: AddCardToFolderResultsScreen for displaying matches
   - **Header**: Shows result count with back button
   - **Card List**: Click card to open add quantity dialog
   - **Empty State**: Helpful message when no results found
   - **Back Navigation**: Returns to search page to refine filters

3. **Navigation Updates** (`Navigation.kt:31-33, 101-112`)
   - **New Route**: AddCardToFolderResults route added
   - **Folder ID**: Passed through navigation parameters
   - **Flow**: AddCardToFolder → [Search Button] → AddCardToFolderResults → [Back] → AddCardToFolder

**User Flow:**
```
Folder Detail → [+ Button]
  ↓
AddCardToFolderScreen (Search Page)
  ├─ All filters + search query
  └─ [Search Button] ─────→ AddCardToFolderResultsScreen (Results)
                                ├─ [← Back] returns to search
                                ├─ Click card → Add quantity dialog
                                └─ Confirm → Card added to folder
```

**Consistency Benefits:**
- ✅ Same UX as viewer search (familiar pattern)
- ✅ Cleaner search setup without results clutter
- ✅ Deck card number filter available for MainDeckCards
- ✅ Quick back navigation to refine search
- ✅ Clear separation of search vs results

**Files Modified:**
- `app/src/main/kotlin/com/srg/inventory/ui/AddCardToFolderScreen.kt` - Two-page split
- `app/src/main/kotlin/com/srg/inventory/ui/Navigation.kt` - New route

**Version Update:**
- **Version**: 1.0.11 (versionCode 14)
- **Previous**: 1.0.10 (versionCode 13)

**Release Build:**
- **AAB Created**: `app/build/outputs/bundle/release/app-release.aab` (181MB)
- **Ready For**: Google Play internal testing track

---

### Card Viewer Two-Page Rework (COMPLETED)

**Goal:** Transform viewer into a cleaner two-page flow: search page → results page

**Features Implemented:**

1. **Search-Only Page** (`CardSearchScreen.kt:28-127`)
   - **Layout**: Dedicated search page with all filters
   - **Filters Include**:
     - Search query with scope selector (All/Name/Rules/Tags)
     - Card Type filter
     - Attack Type filter (for MainDeckCards)
     - Play Order filter (for MainDeckCards)
     - Division filter (for Competitors)
     - **NEW**: Deck Card Number (1-30) filter (for MainDeckCards)
   - **Search Button**: Navigates to results, enabled when any filter/query is set
   - **UX**: Clean, scrollable, focused on search setup

2. **Results Page** (`CardSearchScreen.kt:132-215`)
   - **Header**: Shows result count (e.g., "5 results") with back button
   - **Back Navigation**: Quick return to search page to refine filters
   - **Card List**: Displays all matching cards with details
   - **Empty State**: "No cards found" with suggestion to adjust filters
   - **Card Details**: Tap any card to view full details in dialog

3. **Deck Card Number Filter** (Multiple Files)
   - **ViewModel State**: Added `selectedDeckCardNumber` StateFlow (`CollectionViewModel.kt:124-125`)
   - **Filter Function**: `setDeckCardNumberFilter(Int?)` (`CollectionViewModel.kt:367-369`)
   - **Clear Support**: Reset in `clearFilters()` (`CollectionViewModel.kt:380`)
   - **Database Query**: Added deckCardNumber parameter to `searchCardsWithFilters` (`CardDao.kt:62`)
   - **Repository Layer**: Pass-through support (`CollectionRepository.kt:92`)
   - **Search Flow**: Integrated into combine() flow with proper empty state check (`CollectionViewModel.kt:131-172`)
   - **Filter UI**: 30 chips (1-30) in FiltersSection, visible only for MainDeckCards (`AddCardToFolderScreen.kt`)

4. **State Management Fix**
   - **Problem**: DisposableEffect was clearing filters when navigating to results page
   - **Fix**: Removed DisposableEffect from CardSearchScreen
   - **Result**: Search state persists when navigating between pages

**Navigation Flow:**
```
Viewer Tab
  ↓
CardSearchScreen (Search Page)
  ├─ Search Bar + Scope Selector
  ├─ Card Type → triggers conditional filters
  ├─ MainDeck filters (Atk Type, Play Order, Deck Card #)
  ├─ Competitor filters (Division)
  └─ [Search Button] ─────→ SearchResultsScreen (Results Page)
                                ├─ [← Back] returns to search
                                ├─ Result count in header
                                ├─ Card list
                                └─ Card details dialog
```

**Files Modified:**
- `app/src/main/kotlin/com/srg/inventory/ui/CardSearchScreen.kt` - Two-page split, removed DisposableEffect
- `app/src/main/kotlin/com/srg/inventory/ui/Navigation.kt` - Added SearchResults route
- `app/src/main/kotlin/com/srg/inventory/ui/CollectionViewModel.kt` - Deck card number state, SearchFilters update
- `app/src/main/kotlin/com/srg/inventory/data/CardDao.kt` - Deck card number query parameter
- `app/src/main/kotlin/com/srg/inventory/data/CollectionRepository.kt` - Deck card number support
- `app/src/main/kotlin/com/srg/inventory/ui/AddCardToFolderScreen.kt` - Deck card number filter UI

**User Experience:**
- ✅ Cleaner search setup without results clutter
- ✅ All filters in one place before searching
- ✅ Deck card number filter for precise MainDeck searches
- ✅ Quick back navigation to refine search
- ✅ Clear result count and empty state messaging

**Version Update:**
- **Version**: 1.0.10 (versionCode 13)
- **Previous**: 1.0.9 (versionCode 12)

**Release Build:**
- **AAB Created**: `app/build/outputs/bundle/release/app-release.aab` (181MB)
- **Ready For**: Google Play internal testing track

---

### UI/UX Enhancements and Advanced Search (COMPLETED)

**Goal:** Improve user experience with better search capabilities, cleaner UI, and intuitive controls

**Features Implemented:**

1. **Smart Quantity Entry** (`AddCardToFolderScreen.kt:527-610`)
   - **Before**: Keyboard text input for quantity
   - **After**: +/- buttons with large centered number display
   - **Max**: 999 cards (buttons auto-disable at boundaries)
   - **UX**: No keyboard needed, faster input, clearer limits

2. **Scrollable Settings Screen** (`SettingsScreen.kt:55`)
   - **Problem**: Database info text getting cut off
   - **Fix**: Added `verticalScroll(rememberScrollState())` to main Column
   - **Result**: All settings content fully accessible

3. **Deck Import/Export UI Redesign** (`DeckEditorScreen.kt:142-202`)
   - **Import Button**: Dropdown menu with 3 clear options:
     - Import Deck from URL/QR (Link icon)
     - Import Deck from CSV (Down arrow icon)
     - Import Cards from Collection (Folder icon)
   - **Export Button**: Opens dialog with 2 well-described options:
     - QR Code / Shareable Link
     - Export to CSV
   - **Spectacles Selector**: Moved from top bar to Alternates section (matches other slots)

4. **Advanced Search Scope Selector** (`AddCardToFolderScreen.kt:193-232`)
   - **Feature**: Filter chips to search in specific fields
   - **Options**: All Fields | Name | Rules | Tags
   - **Dynamic Placeholder**: Updates based on selected scope
   - **Implementation**:
     - Database query with CASE statement (`CardDao.kt:46-67`)
     - ViewModel state management (`CollectionViewModel.kt`)
     - Clean UI with FilterChips

5. **Collection Folder Search** (`FolderDetailScreen.kt:104-354`)
   - **Location**: Magnifying glass icon next to Import/Export/QR in folder header
   - **Feature**: Search within specific folder by name, rules, or tags
   - **UI**: Full-screen dialog with search bar, results count, card list
   - **UX**: Click card to view details, close to return to folder

6. **Viewer Screen Cleanup** (`CardSearchScreen.kt:52-62`, `AddCardToFolderScreen.kt:493-524`)
   - **Header**: Removed "Card Search" text, show only card count
   - **Empty State**: No text shown when no search query (just filter inputs)
   - **After Search**: Shows "No cards found" only when user has typed a query
   - **Result**: Prevents text cutoff, cleaner initial view

7. **Icon Swap for Import/Export** (`FolderDetailScreen.kt:110, 121`, `DeckEditorScreen.kt:174`)
   - **Better Metaphor**:
     - Import (pulling data IN) = Down arrow (FileDownload)
     - Export (sending data OUT) = Up arrow (FileUpload)
   - **CSV Filter**: Changed file picker from `text/*` to `text/csv`
   - **Applied To**: Folder CSV import/export, Deck CSV import

8. **App Branding Update** (`SettingsScreen.kt:265, 275`)
   - **Name**: "SRG Supershow Collection Manager" (was "SRG Collection Manager")
   - **Description**: "Manage your SRG Supershow wrestling card collection and build decks"
   - **Clarity**: Makes it clear this is for Supershow by SRG Universe

**Files Modified:**
- `app/src/main/kotlin/com/srg/inventory/ui/AddCardToFolderScreen.kt` - Quantity entry, search scope, empty state
- `app/src/main/kotlin/com/srg/inventory/ui/SettingsScreen.kt` - Scrollable, branding
- `app/src/main/kotlin/com/srg/inventory/ui/DeckEditorScreen.kt` - Import/Export UI, Spectacles move, icon swap
- `app/src/main/kotlin/com/srg/inventory/ui/FolderDetailScreen.kt` - Folder search, icon swap
- `app/src/main/kotlin/com/srg/inventory/ui/CardSearchScreen.kt` - Viewer cleanup
- `app/src/main/kotlin/com/srg/inventory/data/CardDao.kt` - Search scope query
- `app/src/main/kotlin/com/srg/inventory/data/CollectionRepository.kt` - Search scope support
- `app/src/main/kotlin/com/srg/inventory/ui/CollectionViewModel.kt` - Search scope state

**Git Commits:**
1. `35fb879` - Improve UI and add advanced search features
2. `5b86ac3` - Fix compilation errors for experimental Material3 APIs
3. `7a520fc` - Refine UI based on user feedback
4. `aaf5b9f` - Fix UI based on correct requirements
5. `bb49421` - Swap import/export icons and filter CSV imports
6. `6324a21` - Update app name to SRG Supershow Collection Manager

**Version Update:**
- **Version**: 1.0.9 (versionCode 12)
- **Previous**: 1.0.8 (versionCode 11)

**Release Build:**
- **AAB Created**: `app/build/outputs/bundle/release/app-release.aab` (181MB)
- **Ready For**: Google Play internal testing track

**User Experience Improvements:**
- ✅ Faster card quantity entry without keyboard
- ✅ All settings accessible (no text cutoff)
- ✅ Clearer deck import/export with descriptive options
- ✅ Search specific folders for targeted card finding
- ✅ Filter search to exact fields (name/rules/tags only)
- ✅ Intuitive import/export icons (down=in, up=out)
- ✅ Clean viewer with no unnecessary text
- ✅ Clear Supershow branding

---

# Session Notes - Dec 1, 2025

## What Was Completed This Session ✅

### QR Code Scanner UX Fixes (COMPLETED)

**Goal:** Fix QR code scanning issues with orientation and dialog handling

**Issues Fixed:**

1. **Vertical Orientation Scanning** (`QRCodeScanScreen.kt:145`)
   - **Problem**: Scanner couldn't scan QR codes in vertical/portrait orientation
   - **Fix**: Removed `setOrientationLocked(false)` to use default ZXing behavior
   - **Result**: Scanner now works in both portrait and landscape orientations

2. **Dialog Scrolling in Landscape** (`ImportCollectionDialog.kt`, `ImportDeckDialog.kt`)
   - **Problem**: Import dialogs cut off buttons in landscape mode, couldn't scroll to access import/cancel
   - **Fixes**:
     - Made dialog height responsive: `fillMaxHeight(0.9f)` to use 90% of screen
     - Added `verticalScroll(rememberScrollState())` to entire dialog content
     - Replaced `LazyColumn` with regular `Column` + `forEach` (can't nest scrolling)
     - Removed `heightIn(max = 300.dp)` constraint on folder list
   - **Result**: Full dialog content scrollable, buttons always accessible

3. **Dialog Dismissal on Screen Rotation** (`QRCodeScanScreen.kt:38-49, 67-93, 229-282`)
   - **Problem**: Import dialog disappeared when rotating phone during QR import flow
   - **Fixes**:
     - Changed from `remember` to `rememberSaveable` for all state
     - Store primitive data instead of non-Parcelable `SharedListResponse`:
       - `sharedListId: String?`
       - `sharedListName: String?`
       - `sharedListCardCount: Int`
       - `sharedListType: String?`
       - `sharedListSpectacleType: String?`
     - Updated dialog conditions and callbacks to use saved state
   - **Result**: Dialog persists through rotation with all data intact

**Files Modified:**
- `app/src/main/kotlin/com/srg/inventory/ui/QRCodeScanScreen.kt` - Orientation fix, state persistence
- `app/src/main/kotlin/com/srg/inventory/ui/ImportCollectionDialog.kt` - Scrollable dialog
- `app/src/main/kotlin/com/srg/inventory/ui/ImportDeckDialog.kt` - Scrollable dialog

**Complete QR Code User Flow (Fixed):**
1. User taps Scan tab → Taps "Start Scanner" (works in portrait OR landscape)
2. Scans QR code → Import dialog appears with folder selection
3. User can scroll dialog to see all folders and buttons (landscape safe)
4. Rotating phone during import keeps dialog open and preserves state
5. User selects folder → Taps "Import" → Success!

---

### Version & Settings Updates (COMPLETED)

**Version Bump:**
- Updated to **v1.0.8** (versionCode 11)
- Previous: v1.0.7 (versionCode 10)

**Settings Screen Enhancement:**
- Added version display to About section: "Version 1.0.8 (11)"
- Uses `BuildConfig.VERSION_NAME` and `BuildConfig.VERSION_CODE`

**Documentation Updates:**
- **README.md**:
  - Added QR Code Import/Export section to features list
  - Updated "How to Use" with QR Code Sharing instructions
  - Updated Integration section with QR code capabilities
  - Updated Phase 3 roadmap with QR code completion
- **SESSION_NOTES.md**: This session added!

**Files Modified:**
- `app/build.gradle.kts` - Version bump to 1.0.8
- `app/src/main/kotlin/com/srg/inventory/ui/SettingsScreen.kt` - Version display
- `README.md` - QR code documentation
- `SESSION_NOTES.md` - This session

**Current State:**
- ✅ QR scanning works in all orientations
- ✅ Import dialogs fully accessible in landscape
- ✅ Dialogs survive screen rotation
- ✅ Version visible in Settings screen
- ✅ Documentation up to date
- ✅ App builds successfully (v1.0.8)

**APK Location:** `app/build/outputs/apk/debug/app-debug.apk` (176MB)

---

# Session Notes - Nov 29, 2025

## What Was Completed This Session ✅

### QR Code Import - Complete Flow Implementation (COMPLETED)

**Goal:** Complete the QR code import cycle so users can scan QR codes and import collections/decks into the app

**Implementation:**

1. **Import Dialogs Created:**
   - **`ImportCollectionDialog.kt`**:
     - Clean Material 3 dialog with folder selection
     - Radio button list of all available folders
     - Option to create new folder inline with name input
     - Shows shared list name and card count
     - Confirm/Cancel actions

   - **`ImportDeckDialog.kt`**:
     - Similar to collection dialog but for deck folders
     - Shows spectacle type badge (NEWMAN/VALIANT)
     - Shows deck name and card count
     - Inline deck folder creation
     - Material 3 design consistency

2. **CollectionViewModel Import Logic:**
   - Added state: `isImporting`, `importSuccess`
   - `importCollectionFromSharedList(sharedListId, folderId, folderName)`:
     - Fetches shared list from API via `RetrofitClient.api.getSharedList()`
     - Validates list type is COLLECTION
     - Creates new folder if folderId is null
     - Adds all cards to target folder with quantity=1
     - Reports success: "Imported X cards to 'Folder Name'"
     - Handles not-found cards gracefully

3. **DeckViewModel Import Logic:**
   - Added state: `isImporting`, `importSuccess`
   - `importDeckFromSharedList(sharedListId, folderId, folderName)`:
     - Fetches shared list from API
     - Validates list type is DECK
     - Creates new deck folder if needed
     - Extracts spectacle type from deck_data
     - Creates new deck with proper name and spectacle type
     - Imports all slots with full structure preservation:
       - `ENTRANCE` → `repository.setEntrance(deckId, cardUuid)`
       - `COMPETITOR` → `repository.setCompetitor(deckId, cardUuid)`
       - `DECK` → `repository.setDeckCard(deckId, cardUuid, slotNumber)`
       - `FINISH` → `repository.addFinish(deckId, cardUuid)`
       - `ALTERNATE` → `repository.addAlternate(deckId, cardUuid)`
     - Reports success with card count

4. **QRCodeScanScreen Complete Rewrite:**
   - Now accepts both `CollectionViewModel` and `DeckViewModel`
   - URL parsing helper functions:
     - `extractSharedListId()` - Extracts UUID from get-diced.com URLs
     - Supports: `https://get-diced.com/create-list?shared=UUID`
     - Also handles direct UUID strings
   - `handleScannedUrl()` function:
     - Shows loading overlay while fetching
     - Fetches shared list from API
     - Determines type (COLLECTION vs DECK)
     - Shows appropriate import dialog
     - Handles errors with user-friendly messages
   - Loading states and error display
   - Success toasts using `collectAsStateWithLifecycle`
   - Clean Material 3 UI

5. **Navigation and Dependency Updates:**
   - `CollectionNavHost` now accepts `deckViewModel` parameter
   - `MainScreen` creates both ViewModels:
     - `CollectionViewModel` via AndroidViewModelFactory
     - `DeckViewModel` via AndroidViewModelFactory
   - Both ViewModels passed to `QRCodeScanScreen`
   - Added dependency: `androidx.lifecycle:lifecycle-runtime-compose:2.6.2`

6. **UI Polish:**
   - Renamed bottom nav tab from "Collection" to "Lists" (text was getting cut off)
   - Updated `MainScreen.kt` label

**Files Created:**
- `app/src/main/kotlin/com/srg/inventory/ui/ImportCollectionDialog.kt` (217 lines)
- `app/src/main/kotlin/com/srg/inventory/ui/ImportDeckDialog.kt` (220 lines)

**Files Modified:**
- `app/src/main/kotlin/com/srg/inventory/ui/CollectionViewModel.kt` - Import state and logic
- `app/src/main/kotlin/com/srg/inventory/ui/DecksScreen.kt` - DeckViewModel import logic
- `app/src/main/kotlin/com/srg/inventory/ui/QRCodeScanScreen.kt` - Complete rewrite (312 lines)
- `app/src/main/kotlin/com/srg/inventory/ui/Navigation.kt` - Pass both ViewModels
- `app/src/main/kotlin/com/srg/inventory/ui/MainScreen.kt` - Create and pass DeckViewModel, rename tab
- `app/build.gradle.kts` - Added lifecycle-runtime-compose

**Complete QR Code User Flow:**

1. **Export from App:**
   - User goes to collection folder → Taps 📱 → QR code displays
   - User goes to deck editor → Taps 📱 → QR code displays
   - QR code shows clickable URL

2. **Import to App:**
   - User taps Scan tab → Grants camera permission
   - Taps "Start Scanner" → Scans QR code
   - App fetches shared list from API
   - Import dialog appears (collection or deck)
   - User selects destination folder (or creates new)
   - Taps "Import" → Cards/deck imported with structure
   - Success toast: "Imported X cards to 'Folder'"

**Current State:**
- ✅ Complete QR code export (collections and decks)
- ✅ Complete QR code import (collections and decks)
- ✅ Full deck structure preservation
- ✅ URL parsing and API integration
- ✅ Error handling and user feedback
- ✅ App builds successfully (176MB)
- ✅ Deployed to device

**APK Location:** `app/build/outputs/apk/debug/app-debug.apk`

---

### Website Enhancement - QR Codes for Deck Articles (COMPLETED)

**Goal:** Add QR code generation to deck articles so readers can scan and import decks directly

**Implementation:**

1. **Added QR Code Library:**
   - Installed `qrcode.react` to frontend
   - Provides `QRCodeSVG` component for React

2. **Enhanced ArticlePage.jsx:**
   - Added state for sharing: `sharing`, `shareUrl`
   - Created `handleShareDeck(names, title)` function:
     - Fetches all cards by name via `/cards/by-names`
     - Builds proper deck structure from card order:
       - Position 0 → COMPETITOR
       - Position 1 → ENTRANCE
       - MainDeckCards → DECK slots (using deck_card_number)
       - Everything else → ALTERNATE
     - Creates shared list via `/api/shared-lists` with:
       - `list_type: "DECK"`
       - `deck_data` with spectacle_type and slots
       - Description: "Deck from article: {title}"
     - Copies URL to clipboard automatically
   - Updated `CodeBlock` component to pass share handler to `DeckGridFromNames`

3. **Enhanced DeckGridFromNames.jsx:**
   - Imported `QRCodeSVG` from qrcode.react
   - Updated share success section to display:
     - **QR Code** (160x160px, level M)
       - White background with border
       - "Scan to import" caption
     - **Share URL input** with copy button
       - Flex layout for responsive design
       - Instructions: "Share this link or scan the QR code to import this deck"
   - Clean green theme matching success state

**Files Modified:**
- `/home/brandon/data/srg_card_search_website/frontend/src/pages/ArticlePage.jsx`
- `/home/brandon/data/srg_card_search_website/frontend/src/components/DeckGridFromNames.jsx`
- `/home/brandon/data/srg_card_search_website/frontend/package.json` - Added qrcode.react

**How It Works:**
1. Visit any deck article (e.g., `get-diced.com/article/d2-deck`)
2. Deck displayed with "Copy Shareable Link" button
3. Click button → QR code appears + URL copied
4. QR code is scannable immediately
5. Scan with phone app → Import deck with full structure

**Benefits:**
- Articles become interactive deck sharing platforms
- No manual card entry needed
- Full deck structure preserved (slots, spectacle type)
- Works seamlessly with mobile app import

**Current State:**
- ✅ Frontend built and deployed to get-diced.com
- ✅ QR codes display on all deck articles
- ✅ Copy to clipboard works
- ✅ Scannable from Android app

---

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

### Phase 3: QR Code Import Logic (COMPLETED)

**Goal:** Parse scanned QR codes and import collections/decks

**Implementation Completed:**

1. **Import Dialogs Created:**
   - `ImportCollectionDialog.kt`:
     - Shows shared list name and card count
     - Lists all available folders with radio button selection
     - Option to create new folder inline
     - Confirm/Cancel buttons
   - `ImportDeckDialog.kt`:
     - Shows shared list name, spectacle type badge, and card count
     - Lists all available deck folders with radio button selection
     - Option to create new deck folder inline
     - Confirm/Cancel buttons

2. **CollectionViewModel Enhanced:**
   - Added import state: `isImporting`, `importSuccess`
   - `importCollectionFromSharedList(sharedListId, folderId, folderName)`:
     - Fetches shared list from API
     - Verifies it's a COLLECTION type
     - Creates new folder if needed
     - Adds all cards to selected folder
     - Reports success with count of imported cards

3. **DeckViewModel Enhanced:**
   - Added import state: `isImporting`, `importSuccess`
   - `importDeckFromSharedList(sharedListId, folderId, folderName)`:
     - Fetches shared list from API
     - Verifies it's a DECK type
     - Creates new deck folder if needed
     - Creates new deck with proper spectacle type
     - Imports all slots with full structure:
       - ENTRANCE → repository.setEntrance()
       - COMPETITOR → repository.setCompetitor()
       - DECK → repository.setDeckCard(slotNumber)
       - FINISH → repository.addFinish()
       - ALTERNATE → repository.addAlternate()
     - Reports success with card count

4. **QRCodeScanScreen Rewritten:**
   - Now accepts both CollectionViewModel and DeckViewModel
   - URL parsing with `extractSharedListId()`:
     - Supports full URLs: `https://get-diced.com/create-list?shared=UUID`
     - Supports direct UUIDs
   - `handleScannedUrl()` function:
     - Fetches shared list from API
     - Determines type (COLLECTION vs DECK)
     - Shows appropriate import dialog
   - Loading overlay while fetching
   - Error message display
   - Success toasts after import

5. **Navigation Updated:**
   - `CollectionNavHost` now accepts both ViewModels
   - `MainScreen` creates both ViewModels
   - `QRCodeScanScreen` receives both ViewModels for import

6. **Dependencies Added:**
   - `androidx.lifecycle:lifecycle-runtime-compose:2.6.2` for collectAsStateWithLifecycle

**Files Created:**
- `app/src/main/kotlin/com/srg/inventory/ui/ImportCollectionDialog.kt`
- `app/src/main/kotlin/com/srg/inventory/ui/ImportDeckDialog.kt`

**Files Modified:**
- `app/src/main/kotlin/com/srg/inventory/ui/CollectionViewModel.kt` - Import functionality
- `app/src/main/kotlin/com/srg/inventory/ui/DecksScreen.kt` (DeckViewModel) - Import functionality
- `app/src/main/kotlin/com/srg/inventory/ui/QRCodeScanScreen.kt` - Complete rewrite with import logic
- `app/src/main/kotlin/com/srg/inventory/ui/Navigation.kt` - Pass ViewModels to scanner
- `app/src/main/kotlin/com/srg/inventory/ui/MainScreen.kt` - Create and pass DeckViewModel
- `app/build.gradle.kts` - Added lifecycle-runtime-compose dependency

**Current State:**
- ✅ QR code scanning functional
- ✅ URL parsing and shared list fetching
- ✅ Collection import with folder selection
- ✅ Deck import with full structure preservation
- ✅ Success/error messages
- ✅ App builds successfully (176MB debug APK)

**Complete QR Code Flow:**
1. User shares collection/deck → Generates QR code
2. Another user scans QR code → Fetches from API
3. App shows import dialog → User selects destination
4. Cards/deck imported with full structure → Success message

**APK Location:** `app/build/outputs/apk/debug/app-debug.apk`

---

## What's Next 📋

### Ready for Testing 🧪
The complete QR code import/export cycle is now implemented and ready for device testing:
- Export collections as QR codes
- Export decks as QR codes (preserving structure)
- Scan QR codes
- Import collections to folders
- Import decks with full slot structure

### Future Enhancements (Optional)
- Share directly to other apps via Android share sheet
- QR code size customization
- Batch import multiple QR codes for import

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

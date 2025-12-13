package com.srg.inventory.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.srg.inventory.api.CardSyncRepository
import com.srg.inventory.api.ImageSyncRepository
import com.srg.inventory.api.RetrofitClient
import com.srg.inventory.api.SharedListRequest
import com.srg.inventory.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for collection management with folders
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CollectionViewModel(application: Application) : AndroidViewModel(application) {

    private val database = UserCardDatabase.getDatabase(application)
    private val repository = CollectionRepository(
        database.folderDao(),
        database.cardDao(),
        database.folderCardDao()
    )
    private val cardSyncRepository = CardSyncRepository(application)
    private val imageSyncRepository = ImageSyncRepository(application)

    // ==================== UI State ====================

    data class FolderWithCount(
        val folder: Folder,
        val cardCount: Int
    )

    // Folders with card counts
    val foldersWithCounts: StateFlow<List<FolderWithCount>> = repository.getAllFolders()
        .flatMapLatest { folders ->
            combine(folders.map { folder ->
                repository.getCardCountInFolder(folder.id).map { count ->
                    FolderWithCount(folder, count)
                }
            }) { it.toList() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current folder being viewed
    private val _currentFolderId = MutableStateFlow<String?>(null)
    val currentFolderId: StateFlow<String?> = _currentFolderId.asStateFlow()

    // Cards in current folder with quantities (sorted by card type)
    val cardsInCurrentFolder: StateFlow<List<CardWithQuantity>> = _currentFolderId
        .flatMapLatest { folderId ->
            if (folderId != null) {
                repository.getCardsWithQuantitiesInFolder(folderId)
                    .map { cards -> sortCardsByType(cards) }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Sort cards by type in deck order:
     * 1. EntranceCard (alphabetical)
     * 2. SingleCompetitorCard (alphabetical)
     * 3. TornadoCompetitorCard (alphabetical)
     * 4. TrioCompetitorCard (alphabetical)
     * 5. MainDeckCard (by deck_card_number, then alphabetical)
     * 6. SpectacleCard (Valiant alphabetical, Newman alphabetical)
     * 7. CrowdMeterCard (alphabetical)
     */
    private fun sortCardsByType(cards: List<CardWithQuantity>): List<CardWithQuantity> {
        val typeOrder = mapOf(
            "EntranceCard" to 1,
            "SingleCompetitorCard" to 2,
            "TornadoCompetitorCard" to 3,
            "TrioCompetitorCard" to 4,
            "MainDeckCard" to 5,
            "SpectacleCard" to 6,
            "CrowdMeterCard" to 7
        )

        return cards.sortedWith(compareBy(
            // Primary: sort by card type order
            { typeOrder[it.card.cardType] ?: 99 },
            // Secondary: for MainDeckCard, sort by deck card number
            { if (it.card.cardType == "MainDeckCard") it.card.deckCardNumber ?: Int.MAX_VALUE else 0 },
            // Tertiary: for SpectacleCard, Valiant before Newman
            {
                if (it.card.cardType == "SpectacleCard") {
                    when {
                        it.card.name.contains("Valiant", ignoreCase = true) -> 0
                        it.card.name.contains("Newman", ignoreCase = true) -> 1
                        else -> 2
                    }
                } else 0
            },
            // Final: alphabetical by name
            { it.card.name.lowercase() }
        ))
    }

    // Search and filter state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Search scopes - multi-select: name, tags, rules_text (all enabled by default)
    private val _searchScopes = MutableStateFlow(setOf("name", "tags", "rules_text"))
    val searchScopes: StateFlow<Set<String>> = _searchScopes.asStateFlow()

    private val _selectedCardType = MutableStateFlow<String?>(null)
    val selectedCardType: StateFlow<String?> = _selectedCardType.asStateFlow()

    // Multi-select deck card numbers (for MainDeckCard)
    private val _selectedDeckCardNumbers = MutableStateFlow<Set<Int>>(emptySet())
    val selectedDeckCardNumbers: StateFlow<Set<Int>> = _selectedDeckCardNumbers.asStateFlow()

    // Stats filters - minimum values (5-30, default 5)
    private val _minPower = MutableStateFlow<Int>(5)
    val minPower: StateFlow<Int> = _minPower.asStateFlow()

    private val _minTechnique = MutableStateFlow<Int>(5)
    val minTechnique: StateFlow<Int> = _minTechnique.asStateFlow()

    private val _minAgility = MutableStateFlow<Int>(5)
    val minAgility: StateFlow<Int> = _minAgility.asStateFlow()

    private val _minStrike = MutableStateFlow<Int>(5)
    val minStrike: StateFlow<Int> = _minStrike.asStateFlow()

    private val _minSubmission = MutableStateFlow<Int>(5)
    val minSubmission: StateFlow<Int> = _minSubmission.asStateFlow()

    private val _minGrapple = MutableStateFlow<Int>(5)
    val minGrapple: StateFlow<Int> = _minGrapple.asStateFlow()

    private val _selectedDivision = MutableStateFlow<String?>(null)
    val selectedDivision: StateFlow<String?> = _selectedDivision.asStateFlow()

    private val _inCollectionFolderId = MutableStateFlow<String?>(null)
    val inCollectionFolderId: StateFlow<String?> = _inCollectionFolderId.asStateFlow()

    // Temporary stubs for backwards compatibility (will be removed in full refactor)
    val selectedAtkType: StateFlow<String?> = MutableStateFlow<String?>(null).asStateFlow()
    val selectedPlayOrder: StateFlow<String?> = MutableStateFlow<String?>(null).asStateFlow()
    val selectedDeckCardNumber: StateFlow<Int?> = MutableStateFlow<Int?>(null).asStateFlow()
    val searchScope: StateFlow<String> = MutableStateFlow("all").asStateFlow()
    val autocompleteSuggestions: StateFlow<List<String>> = MutableStateFlow<List<String>>(emptyList()).asStateFlow()
    val searchSessionId: StateFlow<Int> = MutableStateFlow(1).asStateFlow()

    fun clearAutocompleteSuggestions() {}
    fun setSearchScope(scope: String) {}
    fun setAtkTypeFilter(atkType: String?) {}
    fun setPlayOrderFilter(playOrder: String?) {}
    fun setDeckCardNumberFilter(deckCardNumber: Int?) {}
    fun triggerSearch() { applyFilters() }
    fun loadMoreResults() { loadNextPage() }

    // Pagination state
    private val _searchOffset = MutableStateFlow(0)
    val searchOffset: StateFlow<Int> = _searchOffset.asStateFlow()

    private val _hasMoreResults = MutableStateFlow(true)
    val hasMoreResults: StateFlow<Boolean> = _hasMoreResults.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    // Filtered card search results (for adding cards to folder)
    private val _searchResults = MutableStateFlow<List<Card>>(emptyList())
    val searchResults: StateFlow<List<Card>> = _searchResults.asStateFlow()

    init {
        // Live search as user types (with 500ms debounce)
        viewModelScope.launch {
            _searchQuery
                .debounce(500)
                .collect {
                    _searchResults.value = emptyList()
                    _searchOffset.value = 0
                    performSearch()
                }
        }
    }

    // Apply filters (called when user clicks Apply in filter dialog)
    fun applyFilters() {
        viewModelScope.launch {
            _searchResults.value = emptyList()
            _searchOffset.value = 0
            performSearch(showAll = false)
        }
    }

    // Clear all filters and reload all cards
    fun clearFilters() {
        _searchQuery.value = ""
        _searchScopes.value = setOf("name", "tags", "rules_text")
        _selectedCardType.value = null
        _selectedDeckCardNumbers.value = emptySet()
        _selectedDivision.value = null
        _minPower.value = 5
        _minTechnique.value = 5
        _minAgility.value = 5
        _minStrike.value = 5
        _minSubmission.value = 5
        _minGrapple.value = 5

        viewModelScope.launch {
            _searchResults.value = emptyList()
            _searchOffset.value = 0
            performSearch(showAll = true)
        }
    }

    private suspend fun performSearch(showAll: Boolean = false) {
        val newResults = repository.searchCardsWithFilters(
            searchQuery = if (showAll) null else _searchQuery.value.ifBlank { null },
            searchName = showAll || _searchScopes.value.contains("name"),
            searchTags = showAll || _searchScopes.value.contains("tags"),
            searchRulesText = showAll || _searchScopes.value.contains("rules_text"),
            cardType = if (showAll) null else _selectedCardType.value,
            division = if (showAll) null else _selectedDivision.value,
            deckCardNumbers = if (showAll) emptyList() else _selectedDeckCardNumbers.value.toList(),
            hasDeckNumbers = !showAll && _selectedDeckCardNumbers.value.isNotEmpty(),
            minPower = if (showAll) 5 else _minPower.value,
            minTechnique = if (showAll) 5 else _minTechnique.value,
            minAgility = if (showAll) 5 else _minAgility.value,
            minStrike = if (showAll) 5 else _minStrike.value,
            minSubmission = if (showAll) 5 else _minSubmission.value,
            minGrapple = if (showAll) 5 else _minGrapple.value,
            limit = 50,
            offset = _searchOffset.value
        ).first()

        if (_searchOffset.value == 0) {
            _searchResults.value = newResults
        } else {
            _searchResults.value = _searchResults.value + newResults
        }
        _hasMoreResults.value = newResults.size == 50
    }

    // Infinite scroll - load next page
    fun loadNextPage() {
        if (_isLoadingMore.value || !_hasMoreResults.value) return

        viewModelScope.launch {
            _isLoadingMore.value = true
            _searchOffset.value += 50
            performSearch()
            _isLoadingMore.value = false
        }
    }

    // Sync state
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

    private val _lastSyncTime = MutableStateFlow("Never")
    val lastSyncTime: StateFlow<String> = _lastSyncTime.asStateFlow()

    private val _cardCount = MutableStateFlow(0)
    val cardCount: StateFlow<Int> = _cardCount.asStateFlow()

    // Image sync state
    private val _isImageSyncing = MutableStateFlow(false)
    val isImageSyncing: StateFlow<Boolean> = _isImageSyncing.asStateFlow()

    private val _imageSyncProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val imageSyncProgress: StateFlow<Pair<Int, Int>?> = _imageSyncProgress.asStateFlow()

    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Share state
    private val _shareUrl = MutableStateFlow<String?>(null)
    val shareUrl: StateFlow<String?> = _shareUrl.asStateFlow()

    private val _isSharing = MutableStateFlow(false)
    val isSharing: StateFlow<Boolean> = _isSharing.asStateFlow()

    // Import state
    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _importSuccess = MutableStateFlow<String?>(null)
    val importSuccess: StateFlow<String?> = _importSuccess.asStateFlow()

    // Filter options
    val cardTypes: StateFlow<List<String>> = flow {
        emit(repository.getAllCardTypes())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val divisions: StateFlow<List<String>> = flow {
        emit(repository.getAllDivisions())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Ensure default folders exist
        viewModelScope.launch {
            repository.ensureDefaultFolders()
            updateCardCount()
            updateLastSyncTime()
        }
    }

    // ==================== Folder Operations ====================

    fun setCurrentFolder(folderId: String) {
        _currentFolderId.value = folderId
    }

    fun clearCurrentFolder() {
        _currentFolderId.value = null
    }

    fun createCustomFolder(name: String) {
        viewModelScope.launch {
            try {
                repository.createFolder(name)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create folder: ${e.message}"
            }
        }
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch {
            try {
                repository.deleteFolder(folder)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete folder: ${e.message}"
            }
        }
    }

    fun renameFolder(folderId: String, newName: String) {
        viewModelScope.launch {
            try {
                val folders = repository.getAllFolders().first()
                val folder = folders.find { it.id == folderId }
                folder?.let {
                    repository.updateFolder(it.copy(name = newName))
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to rename folder: ${e.message}"
            }
        }
    }

    fun clearFolder(folderId: String) {
        viewModelScope.launch {
            try {
                repository.clearFolder(folderId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to clear folder: ${e.message}"
            }
        }
    }

    // ==================== Card Operations ====================

    fun addCardToFolder(folderId: String, cardUuid: String, quantity: Int = 1) {
        viewModelScope.launch {
            try {
                repository.addCardToFolder(folderId, cardUuid, quantity)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add card: ${e.message}"
            }
        }
    }

    fun updateCardQuantityInFolder(folderId: String, cardUuid: String, quantity: Int) {
        viewModelScope.launch {
            try {
                repository.setCardQuantityInFolder(folderId, cardUuid, quantity)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update quantity: ${e.message}"
            }
        }
    }

    fun removeCardFromFolder(folderId: String, cardUuid: String) {
        viewModelScope.launch {
            try {
                repository.removeCardFromFolder(folderId, cardUuid)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to remove card: ${e.message}"
            }
        }
    }

    fun moveCardBetweenFolders(fromFolderId: String, toFolderId: String, cardUuid: String) {
        viewModelScope.launch {
            try {
                repository.moveCardBetweenFolders(fromFolderId, toFolderId, cardUuid)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to move card: ${e.message}"
            }
        }
    }

    suspend fun getCardByName(name: String): Card? = repository.getCardByName(name)

    suspend fun addCardToFolderSuspend(folderId: String, cardUuid: String, quantity: Int = 1) {
        repository.addCardToFolder(folderId, cardUuid, quantity)
    }

    // ==================== Search and Filter Operations ====================

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Filter setter functions
    fun setSearchScopes(scopes: Set<String>) {
        _searchScopes.value = scopes
    }

    fun setCardTypeFilter(cardType: String?) {
        _selectedCardType.value = cardType
        // Clear type-specific filters when changing card type
        if (cardType != "MainDeckCard") {
            _selectedDeckCardNumbers.value = emptySet()
        }
        if (cardType?.contains("Competitor") != true) {
            _selectedDivision.value = null
            // Reset stats to default when not competitor
            _minPower.value = 5
            _minTechnique.value = 5
            _minAgility.value = 5
            _minStrike.value = 5
            _minSubmission.value = 5
            _minGrapple.value = 5
        }
    }

    fun setDeckCardNumbers(numbers: Set<Int>) {
        _selectedDeckCardNumbers.value = numbers
    }

    fun setDivisionFilter(division: String?) {
        _selectedDivision.value = division
    }

    fun setMinPower(value: Int) {
        _minPower.value = value
    }

    fun setMinTechnique(value: Int) {
        _minTechnique.value = value
    }

    fun setMinAgility(value: Int) {
        _minAgility.value = value
    }

    fun setMinStrike(value: Int) {
        _minStrike.value = value
    }

    fun setMinSubmission(value: Int) {
        _minSubmission.value = value
    }

    fun setMinGrapple(value: Int) {
        _minGrapple.value = value
    }

    fun setInCollectionFolderFilter(folderId: String?) {
        _inCollectionFolderId.value = folderId
    }

    // ==================== Sync Operations ====================

    fun syncCardsFromWebsite() {
        viewModelScope.launch {
            try {
                _isSyncing.value = true
                _errorMessage.value = null
                _syncStatus.value = null

                val result = cardSyncRepository.syncDatabase { status ->
                    _syncStatus.value = status
                }

                result.onSuccess { syncResult ->
                    if (syncResult.alreadyUpToDate) {
                        _syncStatus.value = "Already up to date"
                    } else {
                        _syncStatus.value = "Updated ${syncResult.cardsUpdated} cards"
                    }
                    updateCardCount()
                    updateLastSyncTime()
                }.onFailure { error ->
                    _errorMessage.value = "Sync failed: ${error.message}"
                }

            } catch (e: Exception) {
                _errorMessage.value = "Sync error: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun syncImages() {
        viewModelScope.launch {
            try {
                _isImageSyncing.value = true
                _errorMessage.value = null
                _imageSyncProgress.value = null

                val result = imageSyncRepository.syncImages { downloaded, total ->
                    _imageSyncProgress.value = Pair(downloaded, total)
                }

                result.onSuccess { (downloaded, total) ->
                    if (total == 0) {
                        _imageSyncProgress.value = Pair(0, 0)
                    }
                }.onFailure { error ->
                    _errorMessage.value = "Image sync failed: ${error.message}"
                }

            } catch (e: Exception) {
                _errorMessage.value = "Image sync error: ${e.message}"
            } finally {
                _isImageSyncing.value = false
            }
        }
    }

    private suspend fun updateCardCount() {
        _cardCount.value = repository.getCardCount()
    }

    private fun updateLastSyncTime() {
        _lastSyncTime.value = cardSyncRepository.getLastSyncTimeString()
    }

    /**
     * Share a collection folder as a QR code
     */
    fun shareFolderAsQRCode(folderId: String, folderName: String) {
        viewModelScope.launch {
            try {
                _isSharing.value = true
                _errorMessage.value = null
                _shareUrl.value = null

                // Get all cards in the folder
                val cardsInFolder = repository.getCardsInFolder(folderId).first()
                val cardUuids = cardsInFolder.map { it.dbUuid }

                if (cardUuids.isEmpty()) {
                    _errorMessage.value = "Cannot share an empty folder"
                    return@launch
                }

                // Create shared list request
                val request = SharedListRequest(
                    name = folderName,
                    description = "Shared from SRG Collection Manager",
                    cardUuids = cardUuids,
                    listType = "COLLECTION",
                    deckData = null
                )

                // Call API
                val response = RetrofitClient.api.createSharedList(request)

                // Build full URL
                val fullUrl = "https://get-diced.com${response.url}"
                _shareUrl.value = fullUrl

            } catch (e: Exception) {
                _errorMessage.value = "Failed to share folder: ${e.message}"
            } finally {
                _isSharing.value = false
            }
        }
    }

    fun clearShareUrl() {
        _shareUrl.value = null
    }

    /**
     * Import a collection from a shared list
     */
    fun importCollectionFromSharedList(sharedListId: String, folderId: String?, folderName: String) {
        viewModelScope.launch {
            try {
                _isImporting.value = true
                _errorMessage.value = null
                _importSuccess.value = null

                // Fetch shared list from API
                val sharedList = RetrofitClient.api.getSharedList(sharedListId)

                // Verify it's a collection
                if (sharedList.listType != "COLLECTION") {
                    _errorMessage.value = "This is a deck, not a collection"
                    return@launch
                }

                if (sharedList.cardUuids.isEmpty()) {
                    _errorMessage.value = "Shared list is empty"
                    return@launch
                }

                // Get or create folder
                val targetFolderId = if (folderId != null) {
                    folderId
                } else {
                    // Create new folder
                    val newFolder = Folder(name = folderName, isDefault = false)
                    repository.createFolder(newFolder.name)
                    // Get the newly created folder
                    repository.getAllFolders().first()
                        .find { it.name == folderName }?.id
                        ?: throw Exception("Failed to create folder")
                }

                // Add all cards to folder
                var successCount = 0
                var notFoundCount = 0
                sharedList.cardUuids.forEach { cardUuid ->
                    try {
                        repository.addCardToFolder(targetFolderId, cardUuid, quantity = 1)
                        successCount++
                    } catch (e: Exception) {
                        notFoundCount++
                    }
                }

                _importSuccess.value = "Imported $successCount cards to \"$folderName\""
                if (notFoundCount > 0) {
                    _importSuccess.value += " ($notFoundCount cards not found)"
                }

            } catch (e: Exception) {
                _errorMessage.value = "Import failed: ${e.message}"
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun clearImportSuccess() {
        _importSuccess.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // ==================== Related Cards Operations ====================

    suspend fun getRelatedFinishes(cardUuid: String): List<Card> {
        return repository.getRelatedFinishes(cardUuid)
    }

    suspend fun getRelatedCards(cardUuid: String): List<Card> {
        return repository.getRelatedCards(cardUuid)
    }
}

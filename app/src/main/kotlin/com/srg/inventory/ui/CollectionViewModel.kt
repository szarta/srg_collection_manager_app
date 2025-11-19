package com.srg.inventory.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.srg.inventory.api.CardSyncRepository
import com.srg.inventory.api.GetDicedApi
import com.srg.inventory.api.RetrofitClient
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
    private val syncRepository = CardSyncRepository(
        RetrofitClient.api,
        database.cardDao()
    )

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

    // Cards in current folder with quantities
    val cardsInCurrentFolder: StateFlow<List<CardWithQuantity>> = _currentFolderId
        .flatMapLatest { folderId ->
            if (folderId != null) {
                repository.getCardsWithQuantitiesInFolder(folderId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search and filter state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCardType = MutableStateFlow<String?>(null)
    val selectedCardType: StateFlow<String?> = _selectedCardType.asStateFlow()

    private val _selectedAtkType = MutableStateFlow<String?>(null)
    val selectedAtkType: StateFlow<String?> = _selectedAtkType.asStateFlow()

    private val _selectedPlayOrder = MutableStateFlow<String?>(null)
    val selectedPlayOrder: StateFlow<String?> = _selectedPlayOrder.asStateFlow()

    private val _selectedDivision = MutableStateFlow<String?>(null)
    val selectedDivision: StateFlow<String?> = _selectedDivision.asStateFlow()

    // Filtered card search results (for adding cards to folder)
    val searchResults: StateFlow<List<Card>> = combine(
        _searchQuery,
        _selectedCardType,
        _selectedAtkType,
        _selectedPlayOrder,
        _selectedDivision
    ) { query, cardType, atkType, playOrder, division ->
        SearchFilters(query, cardType, atkType, playOrder, division)
    }.flatMapLatest { filters ->
        repository.searchCardsWithFilters(
            searchQuery = filters.query.ifBlank { null },
            cardType = filters.cardType,
            atkType = filters.atkType,
            playOrder = filters.playOrder,
            division = filters.division,
            releaseSet = null,
            isBanned = null,
            limit = 50
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Sync state
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncProgress = MutableStateFlow<CardSyncRepository.SyncProgress?>(null)
    val syncProgress: StateFlow<CardSyncRepository.SyncProgress?> = _syncProgress.asStateFlow()

    private val _lastSyncTime = MutableStateFlow("Never")
    val lastSyncTime: StateFlow<String> = _lastSyncTime.asStateFlow()

    private val _cardCount = MutableStateFlow(0)
    val cardCount: StateFlow<Int> = _cardCount.asStateFlow()

    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Filter options
    val cardTypes: StateFlow<List<String>> = flow {
        emit(repository.getAllCardTypes())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val divisions: StateFlow<List<String>> = flow {
        emit(repository.getAllDivisions())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    data class SearchFilters(
        val query: String,
        val cardType: String?,
        val atkType: String?,
        val playOrder: String?,
        val division: String?
    )

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

    // ==================== Search and Filter Operations ====================

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCardTypeFilter(cardType: String?) {
        _selectedCardType.value = cardType
    }

    fun setAtkTypeFilter(atkType: String?) {
        _selectedAtkType.value = atkType
    }

    fun setPlayOrderFilter(playOrder: String?) {
        _selectedPlayOrder.value = playOrder
    }

    fun setDivisionFilter(division: String?) {
        _selectedDivision.value = division
    }

    fun clearFilters() {
        _selectedCardType.value = null
        _selectedAtkType.value = null
        _selectedPlayOrder.value = null
        _selectedDivision.value = null
    }

    // ==================== Sync Operations ====================

    fun syncCardsFromWebsite() {
        viewModelScope.launch {
            try {
                _isSyncing.value = true
                _errorMessage.value = null

                val result = syncRepository.syncAllCards()

                result.onSuccess { progress ->
                    _syncProgress.value = progress
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

    private suspend fun updateCardCount() {
        _cardCount.value = repository.getCardCount()
    }

    private suspend fun updateLastSyncTime() {
        _lastSyncTime.value = syncRepository.getLastSyncTimeString()
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

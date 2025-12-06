package com.srg.inventory.ui

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.srg.inventory.api.DeckData
import com.srg.inventory.api.DeckSlot
import com.srg.inventory.api.RetrofitClient
import com.srg.inventory.api.SharedListRequest
import com.srg.inventory.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for deck management
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeckViewModel(application: Application) : AndroidViewModel(application) {
    private val database = UserCardDatabase.getDatabase(application)
    private val repository = DeckRepository(
        database.deckFolderDao(),
        database.deckDao(),
        database.deckCardDao(),
        database.cardDao()
    )

    // Deck folders with counts
    data class DeckFolderWithCount(
        val folder: DeckFolder,
        val deckCount: Int
    )

    val deckFoldersWithCounts: StateFlow<List<DeckFolderWithCount>> = repository.getAllDeckFolders()
        .flatMapLatest { folders ->
            if (folders.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(folders.map { folder ->
                    repository.getDeckCountInFolder(folder.id).map { count ->
                        DeckFolderWithCount(folder, count)
                    }
                }) { it.toList() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current folder being viewed
    private val _currentFolderId = MutableStateFlow<String?>(null)
    val currentFolderId: StateFlow<String?> = _currentFolderId.asStateFlow()

    // Decks in current folder
    val decksInCurrentFolder: StateFlow<List<DeckWithCardCount>> = _currentFolderId
        .flatMapLatest { folderId ->
            if (folderId != null) {
                repository.getDecksWithCardCount(folderId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current deck being edited
    private val _currentDeckId = MutableStateFlow<String?>(null)
    val currentDeckId: StateFlow<String?> = _currentDeckId.asStateFlow()

    // Cards in current deck
    val cardsInCurrentDeck: StateFlow<List<DeckCardWithDetails>> = _currentDeckId
        .flatMapLatest { deckId ->
            if (deckId != null) {
                repository.getCardsInDeck(deckId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current deck details
    val currentDeck: StateFlow<Deck?> = _currentDeckId
        .flatMapLatest { deckId ->
            if (deckId != null) {
                repository.getDeckByIdFlow(deckId)
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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

    init {
        viewModelScope.launch {
            repository.ensureDefaultDeckFolders()
        }
    }

    fun setCurrentFolder(folderId: String) {
        _currentFolderId.value = folderId
    }

    fun clearCurrentFolder() {
        _currentFolderId.value = null
    }

    fun setCurrentDeck(deckId: String) {
        _currentDeckId.value = deckId
    }

    fun clearCurrentDeck() {
        _currentDeckId.value = null
    }

    fun createDeck(folderId: String, name: String) {
        viewModelScope.launch {
            try {
                repository.createDeck(folderId, name)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create deck: ${e.message}"
            }
        }
    }

    fun deleteDeck(deckId: String) {
        viewModelScope.launch {
            try {
                repository.deleteDeckById(deckId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete deck: ${e.message}"
            }
        }
    }

    fun renameDeck(deckId: String, newName: String) {
        viewModelScope.launch {
            try {
                val deck = repository.getDeckById(deckId)
                deck?.let {
                    repository.updateDeck(it.copy(name = newName))
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to rename deck: ${e.message}"
            }
        }
    }

    fun updateDeckSpectacleType(deckId: String, spectacleType: SpectacleType) {
        viewModelScope.launch {
            try {
                val deck = repository.getDeckById(deckId)
                deck?.let {
                    repository.updateDeck(it.copy(spectacleType = spectacleType))
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update deck: ${e.message}"
            }
        }
    }

    fun createDeckFolder(name: String) {
        viewModelScope.launch {
            try {
                repository.createDeckFolder(name)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create folder: ${e.message}"
            }
        }
    }

    fun deleteDeckFolder(folder: DeckFolder) {
        viewModelScope.launch {
            try {
                repository.deleteDeckFolder(folder)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete folder: ${e.message}"
            }
        }
    }

    fun renameDeckFolder(folderId: String, newName: String) {
        viewModelScope.launch {
            try {
                val folders = repository.getAllDeckFolders().first()
                val folder = folders.find { it.id == folderId }
                folder?.let {
                    repository.updateDeckFolder(it.copy(name = newName))
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to rename folder: ${e.message}"
            }
        }
    }

    fun removeCardFromDeck(deckId: String, slotType: DeckSlotType, slotNumber: Int) {
        viewModelScope.launch {
            try {
                repository.removeCardFromDeck(deckId, slotType, slotNumber)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to remove card: ${e.message}"
            }
        }
    }

    fun setEntranceCard(deckId: String, cardUuid: String) {
        viewModelScope.launch {
            try {
                // Remove existing entrance first
                repository.removeCardFromDeck(deckId, DeckSlotType.ENTRANCE, 0)
                repository.setEntrance(deckId, cardUuid)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to set entrance: ${e.message}"
            }
        }
    }

    fun setCompetitorCard(deckId: String, cardUuid: String) {
        viewModelScope.launch {
            try {
                // Remove existing competitor first
                repository.removeCardFromDeck(deckId, DeckSlotType.COMPETITOR, 0)
                repository.setCompetitor(deckId, cardUuid)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to set competitor: ${e.message}"
            }
        }
    }

    fun setDeckCard(deckId: String, cardUuid: String, slotNumber: Int) {
        viewModelScope.launch {
            try {
                // Remove existing card at slot first
                repository.removeCardFromDeck(deckId, DeckSlotType.DECK, slotNumber)
                repository.setDeckCard(deckId, cardUuid, slotNumber)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to set deck card: ${e.message}"
            }
        }
    }

    fun addAlternateCard(deckId: String, cardUuid: String) {
        viewModelScope.launch {
            try {
                repository.addAlternate(deckId, cardUuid)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add alternate: ${e.message}"
            }
        }
    }

    /**
     * Share a deck as a QR code with full structure
     */
    fun shareDeckAsQRCode(deckId: String, deckName: String, spectacleType: SpectacleType) {
        viewModelScope.launch {
            try {
                _isSharing.value = true
                _errorMessage.value = null
                _shareUrl.value = null

                // Get all cards in the deck
                val deckCards = repository.getCardsInDeck(deckId).first()

                if (deckCards.isEmpty()) {
                    _errorMessage.value = "Cannot share an empty deck"
                    return@launch
                }

                // Build card UUIDs list (for backward compatibility)
                val cardUuids = deckCards.map { it.card.dbUuid }

                // Build deck slots with proper structure
                val slots = deckCards.map { cardWithDetails ->
                    DeckSlot(
                        slotType = when (cardWithDetails.slotType) {
                            DeckSlotType.ENTRANCE -> "ENTRANCE"
                            DeckSlotType.COMPETITOR -> "COMPETITOR"
                            DeckSlotType.DECK -> "DECK"
                            DeckSlotType.FINISH -> "FINISH"
                            DeckSlotType.ALTERNATE -> "ALTERNATE"
                        },
                        slotNumber = cardWithDetails.slotNumber,
                        cardUuid = cardWithDetails.card.dbUuid
                    )
                }

                // Create deck data
                val deckData = DeckData(
                    spectacleType = spectacleType.name,
                    slots = slots
                )

                // Create shared list request
                val request = SharedListRequest(
                    name = deckName,
                    description = "Shared from SRG Collection Manager",
                    cardUuids = cardUuids,
                    listType = "DECK",
                    deckData = deckData
                )

                // Call API
                val response = RetrofitClient.api.createSharedList(request)

                // Build full URL
                val fullUrl = "https://get-diced.com${response.url}"
                _shareUrl.value = fullUrl

            } catch (e: Exception) {
                _errorMessage.value = "Failed to share deck: ${e.message}"
            } finally {
                _isSharing.value = false
            }
        }
    }

    fun clearShareUrl() {
        _shareUrl.value = null
    }

    /**
     * Import a deck from a shared list with full structure preservation
     */
    fun importDeckFromSharedList(sharedListId: String, folderId: String?, folderName: String) {
        viewModelScope.launch {
            try {
                _isImporting.value = true
                _errorMessage.value = null
                _importSuccess.value = null

                // Fetch shared list from API
                val sharedList = RetrofitClient.api.getSharedList(sharedListId)

                // Verify it's a deck
                if (sharedList.listType != "DECK") {
                    _errorMessage.value = "This is a collection, not a deck"
                    return@launch
                }

                if (sharedList.deckData == null) {
                    _errorMessage.value = "Deck structure data is missing"
                    return@launch
                }

                // Get or create folder
                val targetFolderId = if (folderId != null) {
                    folderId
                } else {
                    // Create new folder
                    val newFolder = DeckFolder(name = folderName, isDefault = false)
                    repository.createDeckFolder(newFolder.name)
                    // Get the newly created folder
                    repository.getAllDeckFolders().first()
                        .find { it.name == folderName }?.id
                        ?: throw Exception("Failed to create folder")
                }

                // Create new deck with name from shared list
                val deckName = sharedList.name ?: "Imported Deck"
                val spectacleType = when (sharedList.deckData.spectacleType) {
                    "VALIANT" -> SpectacleType.VALIANT
                    "NEWMAN" -> SpectacleType.NEWMAN
                    else -> SpectacleType.NEWMAN
                }

                val newDeck = repository.createDeck(targetFolderId, deckName, spectacleType)

                // Import all slots with their structure
                var successCount = 0
                sharedList.deckData.slots.forEach { slot ->
                    try {
                        val slotType = when (slot.slotType) {
                            "ENTRANCE" -> DeckSlotType.ENTRANCE
                            "COMPETITOR" -> DeckSlotType.COMPETITOR
                            "DECK" -> DeckSlotType.DECK
                            "FINISH" -> DeckSlotType.FINISH
                            "ALTERNATE" -> DeckSlotType.ALTERNATE
                            else -> DeckSlotType.ALTERNATE
                        }

                        when (slotType) {
                            DeckSlotType.ENTRANCE -> repository.setEntrance(newDeck.id, slot.cardUuid)
                            DeckSlotType.COMPETITOR -> repository.setCompetitor(newDeck.id, slot.cardUuid)
                            DeckSlotType.DECK -> repository.setDeckCard(newDeck.id, slot.cardUuid, slot.slotNumber)
                            DeckSlotType.FINISH -> repository.addFinish(newDeck.id, slot.cardUuid)
                            DeckSlotType.ALTERNATE -> repository.addAlternate(newDeck.id, slot.cardUuid)
                        }
                        successCount++
                    } catch (e: Exception) {
                        // Card not found or invalid slot, continue
                    }
                }

                _importSuccess.value = "Imported \"$deckName\" with $successCount cards to \"$folderName\""

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

    /**
     * Import cards from a collection folder into a deck
     * Automatically slots cards based on type:
     * - EntranceCard → ENTRANCE slot
     * - Competitor cards → COMPETITOR slot (first found)
     * - MainDeckCard → DECK slots (by deck_card_number)
     * - Extra cards → ALTERNATES
     */
    fun importFolderToDeck(deckId: String, collectionFolderId: String) {
        viewModelScope.launch {
            try {
                _errorMessage.value = null

                // Get the collection repository
                val collectionRepository = com.srg.inventory.data.CollectionRepository(
                    database.folderDao(),
                    database.cardDao(),
                    database.folderCardDao()
                )

                // Get all cards from the collection folder
                val cardsInFolder = collectionRepository.getCardsInFolder(collectionFolderId).first()

                if (cardsInFolder.isEmpty()) {
                    _errorMessage.value = "Selected folder is empty"
                    return@launch
                }

                // Track what we've added
                var entranceAdded = false
                var competitorAdded = false
                val deckSlotsUsed = mutableSetOf<Int>()
                var alternatesCount = 0

                // Get current deck cards to avoid overwriting
                val currentDeckCards = repository.getDeckCards(deckId)
                val hasEntrance = currentDeckCards.any { it.slotType == DeckSlotType.ENTRANCE }
                val hasCompetitor = currentDeckCards.any { it.slotType == DeckSlotType.COMPETITOR }
                val usedDeckSlots = currentDeckCards
                    .filter { it.slotType == DeckSlotType.DECK }
                    .map { it.slotNumber }
                    .toSet()

                // Process each card
                for (card in cardsInFolder) {
                    when (card.cardType) {
                        "EntranceCard" -> {
                            if (!hasEntrance && !entranceAdded) {
                                repository.setEntrance(deckId, card.dbUuid)
                                entranceAdded = true
                            } else {
                                repository.addAlternate(deckId, card.dbUuid)
                                alternatesCount++
                            }
                        }

                        "SingleCompetitorCard", "TornadoCompetitorCard", "TrioCompetitorCard" -> {
                            if (!hasCompetitor && !competitorAdded) {
                                repository.setCompetitor(deckId, card.dbUuid)
                                competitorAdded = true
                            } else {
                                repository.addAlternate(deckId, card.dbUuid)
                                alternatesCount++
                            }
                        }

                        "MainDeckCard" -> {
                            // Try to slot by deck_card_number if available
                            val slotNumber = card.deckCardNumber
                            if (slotNumber != null && slotNumber in 1..30) {
                                if (!usedDeckSlots.contains(slotNumber) && !deckSlotsUsed.contains(slotNumber)) {
                                    repository.setDeckCard(deckId, card.dbUuid, slotNumber)
                                    deckSlotsUsed.add(slotNumber)
                                } else {
                                    // Slot already filled, add to alternates
                                    repository.addAlternate(deckId, card.dbUuid)
                                    alternatesCount++
                                }
                            } else {
                                // No deck_card_number, add to alternates
                                repository.addAlternate(deckId, card.dbUuid)
                                alternatesCount++
                            }
                        }

                        else -> {
                            // All other card types (spectacles, etc.) go to alternates
                            repository.addAlternate(deckId, card.dbUuid)
                            alternatesCount++
                        }
                    }
                }

                // Build success message
                val parts = mutableListOf<String>()
                if (entranceAdded) parts.add("entrance")
                if (competitorAdded) parts.add("competitor")
                if (deckSlotsUsed.isNotEmpty()) parts.add("${deckSlotsUsed.size} deck cards")
                if (alternatesCount > 0) parts.add("$alternatesCount alternates")

                _importSuccess.value = "Imported: ${parts.joinToString(", ")}"

            } catch (e: Exception) {
                _errorMessage.value = "Import failed: ${e.message}"
            }
        }
    }
}

/**
 * Screen showing deck folders (Singles, Tornado, Trios, Tag)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecksScreen(
    onFolderClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeckViewModel = viewModel()
) {
    val foldersWithCounts by viewModel.deckFoldersWithCounts.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var folderToRename by remember { mutableStateOf<DeckViewModel.DeckFolderWithCount?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Decks") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Create folder")
            }
        }
    ) { paddingValues ->
        if (foldersWithCounts.isEmpty()) {
            // Loading or empty state
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(foldersWithCounts, key = { it.folder.id }) { folderWithCount ->
                    DeckFolderItem(
                        folder = folderWithCount.folder,
                        deckCount = folderWithCount.deckCount,
                        onClick = { onFolderClick(folderWithCount.folder.id) },
                        onRename = if (!folderWithCount.folder.isDefault) {
                            { folderToRename = folderWithCount }
                        } else null,
                        onDelete = if (!folderWithCount.folder.isDefault) {
                            { viewModel.deleteDeckFolder(folderWithCount.folder) }
                        } else null
                    )
                }
            }
        }
    }

    // Create folder dialog
    if (showCreateDialog) {
        CreateDeckFolderDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                viewModel.createDeckFolder(name)
                showCreateDialog = false
            }
        )
    }

    // Rename folder dialog
    folderToRename?.let { folderWithCount ->
        RenameDeckFolderDialog(
            currentName = folderWithCount.folder.name,
            onDismiss = { folderToRename = null },
            onRename = { newName ->
                viewModel.renameDeckFolder(folderWithCount.folder.id, newName)
                folderToRename = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckFolderItem(
    folder: DeckFolder,
    deckCount: Int,
    onClick: () -> Unit,
    onRename: (() -> Unit)? = null,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Folder icon
            Surface(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (folder.id) {
                            "singles" -> Icons.Default.Person
                            "tornado" -> Icons.Default.Cyclone
                            "trios" -> Icons.Default.Groups
                            "tag" -> Icons.Default.Group
                            else -> Icons.Default.Folder
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Folder info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$deckCount deck${if (deckCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Rename button for custom folders
            if (onRename != null) {
                IconButton(onClick = onRename) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Rename folder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Delete button for custom folders
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete folder",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CreateDeckFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Deck Folder") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Folder Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RenameDeckFolderDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Deck Folder") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Folder Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onRename(name) },
                enabled = name.isNotBlank() && name != currentName
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

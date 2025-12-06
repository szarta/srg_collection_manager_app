package com.srg.inventory.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.srg.inventory.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject
import android.content.ClipData
import android.content.ClipboardManager

/**
 * Screen for editing a deck's contents
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckEditorScreen(
    deckId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeckViewModel = viewModel()
) {
    val deck by viewModel.currentDeck.collectAsState()
    val cardsInDeck by viewModel.cardsInCurrentDeck.collectAsState()
    val isSharing by viewModel.isSharing.collectAsState()
    val shareUrl by viewModel.shareUrl.collectAsState()
    val importSuccess by viewModel.importSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showSpectacleMenu by remember { mutableStateOf(false) }
    var showCardPicker by remember { mutableStateOf<DeckSlotType?>(null) }
    var currentSlotNumber by remember { mutableStateOf(0) }
    var showImportUrlDialog by remember { mutableStateOf(false) }
    var showImportFolderDialog by remember { mutableStateOf(false) }
    var showImportMenu by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showCollectionSearch by remember { mutableStateOf(false) }
    var cardToView by remember { mutableStateOf<Card?>(null) }

    // File picker for import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                importDeckFromCsv(context, it, deckId, viewModel)
            }
        }
    }

    LaunchedEffect(deckId) {
        viewModel.setCurrentDeck(deckId)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearCurrentDeck()
        }
    }

    // Show import success toast
    LaunchedEffect(importSuccess) {
        importSuccess?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearImportSuccess()
        }
    }

    // Show error toast
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    // Group cards by slot type
    val entrance = cardsInDeck.find { it.slotType == DeckSlotType.ENTRANCE }
    val competitor = cardsInDeck.find { it.slotType == DeckSlotType.COMPETITOR }
    val deckCards = cardsInDeck.filter { it.slotType == DeckSlotType.DECK }
        .sortedBy { it.slotNumber }
    val alternates = cardsInDeck.filter { it.slotType == DeckSlotType.ALTERNATE }
        .sortedBy { it.slotNumber }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(deck?.name ?: "Deck")
                            deck?.let {
                                Text(
                                    text = "${cardsInDeck.size} cards",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
                // Action buttons row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Import button with dropdown menu
                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { showImportMenu = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.FileDownload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import")
                        }
                        DropdownMenu(
                            expanded = showImportMenu,
                            onDismissRequest = { showImportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Import Deck from URL/QR") },
                                onClick = {
                                    showImportUrlDialog = true
                                    showImportMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Import Deck from CSV") },
                                onClick = {
                                    importLauncher.launch("text/*")
                                    showImportMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Import Cards from Collection") },
                                onClick = {
                                    showImportFolderDialog = true
                                    showImportMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) }
                            )
                        }
                    }

                    // Export button
                    Button(
                        onClick = { showExportDialog = true },
                        enabled = cardsInDeck.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.FileUpload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export")
                    }

                    // Search in collection button
                    IconButton(
                        onClick = { showImportFolderDialog = true }
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search collection")
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Entrance slot
            item {
                DeckSlotHeader("Entrance")
            }
            item {
                DeckSlotItem(
                    label = "Entrance Card",
                    card = entrance?.card,
                    onClick = {
                        if (entrance?.card != null) {
                            cardToView = entrance.card
                        } else {
                            showCardPicker = DeckSlotType.ENTRANCE
                            currentSlotNumber = 0
                        }
                    },
                    onClear = entrance?.let {
                        { viewModel.removeCardFromDeck(deckId, DeckSlotType.ENTRANCE, 0) }
                    }
                )
            }

            // Competitor slot
            item {
                DeckSlotHeader("Competitor")
            }
            item {
                DeckSlotItem(
                    label = "Competitor Card",
                    card = competitor?.card,
                    onClick = {
                        if (competitor?.card != null) {
                            cardToView = competitor.card
                        } else {
                            showCardPicker = DeckSlotType.COMPETITOR
                            currentSlotNumber = 0
                        }
                    },
                    onClear = competitor?.let {
                        { viewModel.removeCardFromDeck(deckId, DeckSlotType.COMPETITOR, 0) }
                    }
                )
            }

            // Deck cards 1-30
            item {
                DeckSlotHeader("Deck Cards")
            }
            items((1..30).toList()) { slotNumber ->
                val deckCard = deckCards.find { it.slotNumber == slotNumber }
                DeckSlotItem(
                    label = "#$slotNumber",
                    card = deckCard?.card,
                    onClick = {
                        if (deckCard?.card != null) {
                            cardToView = deckCard.card
                        } else {
                            showCardPicker = DeckSlotType.DECK
                            currentSlotNumber = slotNumber
                        }
                    },
                    onClear = deckCard?.let {
                        { viewModel.removeCardFromDeck(deckId, DeckSlotType.DECK, slotNumber) }
                    }
                )
            }

            // Alternates
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DeckSlotHeader("Alternates")
                    IconButton(onClick = {
                        showCardPicker = DeckSlotType.ALTERNATE
                        currentSlotNumber = 0
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add alternate")
                    }
                }
            }
            if (alternates.isEmpty()) {
                item {
                    Text(
                        text = "No alternates added",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                }
            } else {
                items(alternates) { alternate ->
                    DeckSlotItem(
                        label = "Alternate",
                        card = alternate.card,
                        onClick = { cardToView = alternate.card },
                        onClear = {
                            viewModel.removeCardFromDeck(
                                deckId,
                                DeckSlotType.ALTERNATE,
                                alternate.slotNumber
                            )
                        }
                    )
                }
            }

            // Spectacles selector
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSpectacleMenu = true },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Spectacles",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = deck?.spectacleType?.name ?: "VALIANT",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                DropdownMenu(
                    expanded = showSpectacleMenu,
                    onDismissRequest = { showSpectacleMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Valiant") },
                        onClick = {
                            viewModel.updateDeckSpectacleType(deckId, SpectacleType.VALIANT)
                            showSpectacleMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Newman") },
                        onClick = {
                            viewModel.updateDeckSpectacleType(deckId, SpectacleType.NEWMAN)
                            showSpectacleMenu = false
                        }
                    )
                }
            }

            // Spectacle info
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Spectacle: ${deck?.spectacleType?.name ?: "Valiant"}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "All ${deck?.spectacleType?.name ?: "Valiant"} spectacle cards included",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }

    // Card picker dialog
    showCardPicker?.let { slotType ->
        CardPickerDialog(
            slotType = slotType,
            slotNumber = currentSlotNumber,
            deckId = deckId,
            folderId = deck?.folderId ?: "",
            viewModel = viewModel,
            onDismiss = { showCardPicker = null }
        )
    }

    // Import from folder dialog
    if (showImportFolderDialog) {
        ImportFolderDialog(
            deckId = deckId,
            viewModel = viewModel,
            onDismiss = { showImportFolderDialog = false }
        )
    }

    // Import from URL dialog
    if (showImportUrlDialog) {
        ImportUrlDialog(
            onDismiss = { showImportUrlDialog = false },
            onImport = { url ->
                showImportUrlDialog = false
                scope.launch {
                    importDeckFromUrl(context, url, deckId, viewModel)
                }
            }
        )
    }

    // Export dialog
    if (showExportDialog) {
        ExportDeckDialog(
            deck = deck,
            cardsInDeck = cardsInDeck,
            isSharing = isSharing,
            onDismiss = { showExportDialog = false },
            onExportQRCode = {
                deck?.let {
                    viewModel.shareDeckAsQRCode(deckId, it.name, it.spectacleType)
                }
                showExportDialog = false
            },
            onExportCSV = {
                scope.launch {
                    exportDeckToCsv(context, deck, cardsInDeck)
                }
                showExportDialog = false
            }
        )
    }

    // QR Code share dialog
    shareUrl?.let { url ->
        QRCodeDialog(
            url = url,
            title = "Share Deck",
            onDismiss = { viewModel.clearShareUrl() }
        )
    }

    // Card detail dialog
    cardToView?.let { card ->
        DeckCardDetailDialog(
            card = card,
            onDismiss = { cardToView = null }
        )
    }
}

@Composable
fun ExportDeckDialog(
    deck: Deck?,
    cardsInDeck: List<DeckCardWithDetails>,
    isSharing: Boolean,
    onDismiss: () -> Unit,
    onExportQRCode: () -> Unit,
    onExportCSV: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Deck") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Choose how you want to export \"${deck?.name ?: "this deck"}\"",
                    style = MaterialTheme.typography.bodyMedium
                )

                // QR Code option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isSharing) { onExportQRCode() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.QrCode2,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "QR Code / Shareable Link",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Generate a QR code and URL to share online",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        if (isSharing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // CSV option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExportCSV() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Export to CSV",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Save deck as a CSV file for local backup",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ImportUrlDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import from URL") },
        text = {
            Column {
                Text(
                    text = "Paste a get-diced.com shareable link",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onImport(url) },
                enabled = url.isNotBlank()
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportFolderDialog(
    deckId: String,
    viewModel: DeckViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { UserCardDatabase.getDatabase(context) }
    val folderDao = remember { database.folderDao() }

    var folders by remember { mutableStateOf<List<Folder>>(emptyList()) }
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Load folders
    LaunchedEffect(Unit) {
        isLoading = true
        folders = folderDao.getAllFoldersList()
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import from Collection Folder") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Select a collection folder to import cards from. Cards will be automatically slotted based on their type.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (folders.isEmpty()) {
                    Text(
                        text = "No collection folders found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // Folder selection list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(folders, key = { it.id }) { folder ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedFolderId = folder.id },
                                color = if (selectedFolderId == folder.id) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                shape = MaterialTheme.shapes.small
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedFolderId == folder.id,
                                        onClick = { selectedFolderId = folder.id }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = folder.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (folder.isDefault) {
                                            Text(
                                                text = "Default",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedFolderId?.let { folderId ->
                        viewModel.importFolderToDeck(deckId, folderId)
                        onDismiss()
                    }
                },
                enabled = selectedFolderId != null
            ) {
                Text("Import")
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
fun DeckSlotHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun DeckSlotItem(
    label: String,
    card: Card?,
    onClick: () -> Unit,
    onClear: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Slot label
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(80.dp)
            )

            // Card name or empty
            if (card != null) {
                Text(
                    text = card.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                if (onClear != null) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Remove",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                Text(
                    text = "Tap to select",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardPickerDialog(
    slotType: DeckSlotType,
    slotNumber: Int,
    deckId: String,
    folderId: String,
    viewModel: DeckViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var cards by remember { mutableStateOf<List<Card>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Get database and load cards
    val database = remember { UserCardDatabase.getDatabase(context) }
    val cardDao = remember { database.cardDao() }

    // Determine card type filter based on slot type and folder
    val cardTypeFilter = when (slotType) {
        DeckSlotType.ENTRANCE -> "EntranceCard"
        DeckSlotType.COMPETITOR -> when (folderId) {
            "singles" -> "SingleCompetitorCard"
            "tornado" -> "TornadoCompetitorCard"
            "trios" -> "TrioCompetitorCard"
            else -> listOf("SingleCompetitorCard", "TornadoCompetitorCard", "TrioCompetitorCard") // Tag or custom
        }
        DeckSlotType.DECK -> "MainDeckCard"
        DeckSlotType.ALTERNATE -> null // Any card type
        else -> null
    }

    // Load cards on first composition
    LaunchedEffect(Unit) {
        isLoading = true
        cards = when {
            // For deck slots, filter by deck_card_number
            slotType == DeckSlotType.DECK -> cardDao.getCardsByTypeAndNumber("MainDeckCard", slotNumber)
            cardTypeFilter is String -> cardDao.getCardsByTypeSuspend(cardTypeFilter)
            cardTypeFilter is List<*> -> {
                val allCards = mutableListOf<Card>()
                for (type in cardTypeFilter) {
                    allCards.addAll(cardDao.getCardsByTypeSuspend(type as String))
                }
                allCards.sortedBy { it.name }
            }
            else -> cardDao.getAllCardsSuspend()
        }
        isLoading = false
    }

    // Filter cards by search query
    val filteredCards = remember(cards, searchQuery) {
        if (searchQuery.isBlank()) {
            cards
        } else {
            cards.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f),
        title = {
            Text(
                when (slotType) {
                    DeckSlotType.ENTRANCE -> "Select Entrance Card"
                    DeckSlotType.COMPETITOR -> "Select Competitor"
                    DeckSlotType.DECK -> "Select Deck Card #$slotNumber"
                    DeckSlotType.ALTERNATE -> "Select Alternate"
                    else -> "Select Card"
                }
            )
        },
        text = {
            Column {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Card list
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (filteredCards.isEmpty()) {
                    Text(
                        text = "No cards found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredCards, key = { it.dbUuid }) { card ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            when (slotType) {
                                                DeckSlotType.ENTRANCE -> viewModel.setEntranceCard(deckId, card.dbUuid)
                                                DeckSlotType.COMPETITOR -> viewModel.setCompetitorCard(deckId, card.dbUuid)
                                                DeckSlotType.DECK -> viewModel.setDeckCard(deckId, card.dbUuid, slotNumber)
                                                DeckSlotType.ALTERNATE -> viewModel.addAlternateCard(deckId, card.dbUuid)
                                                else -> {}
                                            }
                                            onDismiss()
                                        }
                                    },
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = card.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (card.cardType == "MainDeckCard" && card.deckCardNumber != null) {
                                        Text(
                                            text = "#${card.deckCardNumber}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Export deck to CSV
private suspend fun exportDeckToCsv(
    context: android.content.Context,
    deck: Deck?,
    cardsInDeck: List<DeckCardWithDetails>
) {
    if (deck == null) return

    withContext(Dispatchers.IO) {
        try {
            val fileName = "${deck.name.replace(" ", "_")}_deck.csv"
            val file = File(context.cacheDir, fileName)

            file.bufferedWriter().use { writer ->
                // Header
                writer.write("Slot Type,Slot Number,Card Name\n")

                // Sort cards by slot type and number
                val sortedCards = cardsInDeck.sortedWith(
                    compareBy(
                        { when (it.slotType) {
                            DeckSlotType.ENTRANCE -> 0
                            DeckSlotType.COMPETITOR -> 1
                            DeckSlotType.DECK -> 2
                            DeckSlotType.ALTERNATE -> 3
                            else -> 4
                        }},
                        { it.slotNumber }
                    )
                )

                for (cardWithDetails in sortedCards) {
                    val slotType = cardWithDetails.slotType.name
                    val slotNumber = cardWithDetails.slotNumber
                    val cardName = cardWithDetails.card.name.replace("\"", "\"\"")
                    writer.write("$slotType,$slotNumber,\"$cardName\"\n")
                }
            }

            // Share the file
            withContext(Dispatchers.Main) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Export Deck"))
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

// Import deck from CSV
private suspend fun importDeckFromCsv(
    context: android.content.Context,
    uri: Uri,
    deckId: String,
    viewModel: DeckViewModel
) {
    withContext(Dispatchers.IO) {
        try {
            val database = UserCardDatabase.getDatabase(context)
            val cardDao = database.cardDao()
            var importedCount = 0
            val notFound = mutableListOf<String>()

            // Read all lines first
            val lines = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).readLines()
            } ?: emptyList()

            // Process lines
            var skipFirst = false
            if (lines.isNotEmpty()) {
                val firstLine = lines[0].lowercase()
                skipFirst = firstLine.contains("slot") || firstLine.contains("card")
            }

            val linesToProcess = if (skipFirst) lines.drop(1) else lines

            for (line in linesToProcess) {
                val parts = parseCsvLine(line)
                if (parts.size >= 3) {
                    val slotTypeStr = parts[0].trim().uppercase()
                    val slotNumber = parts[1].trim().toIntOrNull() ?: 0
                    val cardName = parts[2].trim().trim('"')

                    // Find card by name
                    val card = cardDao.getCardByName(cardName)
                    if (card != null) {
                        val slotType = try {
                            DeckSlotType.valueOf(slotTypeStr)
                        } catch (e: Exception) {
                            null
                        }

                        if (slotType != null) {
                            when (slotType) {
                                DeckSlotType.ENTRANCE -> viewModel.setEntranceCard(deckId, card.dbUuid)
                                DeckSlotType.COMPETITOR -> viewModel.setCompetitorCard(deckId, card.dbUuid)
                                DeckSlotType.DECK -> viewModel.setDeckCard(deckId, card.dbUuid, slotNumber)
                                DeckSlotType.ALTERNATE -> viewModel.addAlternateCard(deckId, card.dbUuid)
                                else -> {}
                            }
                            importedCount++
                        }
                    } else {
                        notFound.add(cardName)
                    }
                }
            }

            withContext(Dispatchers.Main) {
                val message = if (notFound.isEmpty()) {
                    "Imported $importedCount cards"
                } else {
                    "Imported $importedCount cards. Not found: ${notFound.take(3).joinToString(", ")}${if (notFound.size > 3) "..." else ""}"
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

// Parse CSV line handling quoted values
private fun parseCsvLine(line: String): List<String> {
    val result = mutableListOf<String>()
    var current = StringBuilder()
    var inQuotes = false

    for (char in line) {
        when {
            char == '"' -> inQuotes = !inQuotes
            char == ',' && !inQuotes -> {
                result.add(current.toString())
                current = StringBuilder()
            }
            else -> current.append(char)
        }
    }
    result.add(current.toString())
    return result
}

// Share deck to web API
private suspend fun shareDeckToWeb(
    context: android.content.Context,
    deck: Deck?,
    cardsInDeck: List<DeckCardWithDetails>
) {
    if (deck == null || cardsInDeck.isEmpty()) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Add cards to deck before sharing", Toast.LENGTH_SHORT).show()
        }
        return
    }

    withContext(Dispatchers.IO) {
        try {
            // Collect card UUIDs
            val cardUuids = cardsInDeck.map { it.card.dbUuid }

            // Build JSON payload
            val json = JSONObject().apply {
                put("name", deck.name)
                put("description", "Shared from SRG Collection Manager")
                put("card_uuids", JSONArray(cardUuids))
            }

            // Make API request
            val url = URL("https://get-diced.com/api/shared-lists")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            connection.outputStream.use { os ->
                os.write(json.toString().toByteArray())
            }

            val responseCode = connection.responseCode
            if (responseCode == 200 || responseCode == 201) {
                val response = connection.inputStream.bufferedReader().readText()
                val responseJson = JSONObject(response)
                val shareUrl = "https://get-diced.com${responseJson.getString("url")}"

                withContext(Dispatchers.Main) {
                    // Copy to clipboard
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Deck URL", shareUrl)
                    clipboard.setPrimaryClip(clip)

                    Toast.makeText(context, "Link copied to clipboard!", Toast.LENGTH_LONG).show()
                }
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Share failed: $errorBody", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Share failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

/**
 * Dialog showing full card details including image, stats, rules, and errata
 */
@Composable
private fun DeckCardDetailDialog(
    card: Card,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(card.name)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = card.cardType.replace("Card", ""),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card image
                item {
                    coil.compose.AsyncImage(
                        model = com.srg.inventory.utils.ImageUtils.buildCardImageRequest(context, card.dbUuid, thumbnail = false),
                        contentDescription = card.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.7f),
                        contentScale = ContentScale.Fit
                    )
                }

                // Stats for competitors
                if (card.isCompetitor) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Stats",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                CompetitorStatItem("PWR", card.power)
                                CompetitorStatItem("AGI", card.agility)
                                CompetitorStatItem("STR", card.strike)
                                CompetitorStatItem("SUB", card.submission)
                                CompetitorStatItem("GRP", card.grapple)
                                CompetitorStatItem("TEC", card.technique)
                            }
                            card.division?.let {
                                Text(
                                    text = "Division: $it",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Main deck properties
                if (card.isMainDeck) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Properties",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            card.deckCardNumber?.let {
                                Text("Deck #: $it", style = MaterialTheme.typography.bodySmall)
                            }
                            card.atkType?.let {
                                Text("Attack Type: $it", style = MaterialTheme.typography.bodySmall)
                            }
                            card.playOrder?.let {
                                Text("Play Order: $it", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                // Rules text
                card.rulesText?.let { rules ->
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Rules",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = rules,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Errata
                card.errataText?.let { errata ->
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Errata",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = errata,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // Release set
                card.releaseSet?.let { set ->
                    item {
                        Text(
                            text = "Set: $set",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun CompetitorStatItem(label: String, value: Int?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value?.toString() ?: "-",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

// Import deck from shareable URL
private suspend fun importDeckFromUrl(
    context: android.content.Context,
    urlString: String,
    deckId: String,
    viewModel: DeckViewModel
) {
    withContext(Dispatchers.IO) {
        try {
            // Extract shared ID from URL
            // Formats: https://get-diced.com/create-list?shared=xxx or just the ID
            val sharedId = when {
                urlString.contains("shared=") -> {
                    urlString.substringAfter("shared=").substringBefore("&")
                }
                urlString.matches(Regex("^[a-zA-Z0-9-]+$")) -> urlString
                else -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Invalid URL format", Toast.LENGTH_LONG).show()
                    }
                    return@withContext
                }
            }

            // Fetch shared list from API
            val url = URL("https://get-diced.com/api/shared-lists/$sharedId")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val responseJson = JSONObject(response)
                val cardUuids = responseJson.getJSONArray("card_uuids")

                var importedCount = 0

                // Add cards to deck as alternates (since we don't know slot assignments)
                for (i in 0 until cardUuids.length()) {
                    val uuid = cardUuids.getString(i)
                    viewModel.addAlternateCard(deckId, uuid)
                    importedCount++
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Imported $importedCount cards as alternates",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else if (responseCode == 404) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Shared list not found", Toast.LENGTH_LONG).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Import failed: HTTP $responseCode", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

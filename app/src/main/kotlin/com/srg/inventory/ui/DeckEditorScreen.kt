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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showSpectacleMenu by remember { mutableStateOf(false) }
    var showCardPicker by remember { mutableStateOf<DeckSlotType?>(null) }
    var currentSlotNumber by remember { mutableStateOf(0) }
    var showImportUrlDialog by remember { mutableStateOf(false) }

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
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Share as QR Code button
                    IconButton(
                        onClick = {
                            deck?.let {
                                viewModel.shareDeckAsQRCode(deckId, it.name, it.spectacleType)
                            }
                        },
                        enabled = !isSharing && cardsInDeck.isNotEmpty()
                    ) {
                        if (isSharing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.QrCode2, contentDescription = "Share as QR Code")
                        }
                    }
                    // Import from URL button
                    IconButton(onClick = { showImportUrlDialog = true }) {
                        Icon(Icons.Default.Link, contentDescription = "Import from URL")
                    }
                    // Import CSV button
                    IconButton(onClick = { importLauncher.launch("text/*") }) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Import CSV")
                    }
                    // Export button
                    IconButton(onClick = {
                        scope.launch {
                            exportDeckToCsv(context, deck, cardsInDeck)
                        }
                    }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export CSV")
                    }
                    // Spectacle type selector
                    Box {
                        TextButton(onClick = { showSpectacleMenu = true }) {
                            Text(deck?.spectacleType?.name ?: "VALIANT")
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
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
                        showCardPicker = DeckSlotType.ENTRANCE
                        currentSlotNumber = 0
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
                        showCardPicker = DeckSlotType.COMPETITOR
                        currentSlotNumber = 0
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
                        showCardPicker = DeckSlotType.DECK
                        currentSlotNumber = slotNumber
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
                        onClick = { /* View card details */ },
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

    // QR Code share dialog
    shareUrl?.let { url ->
        QRCodeDialog(
            url = url,
            title = "Share Deck",
            onDismiss = { viewModel.clearShareUrl() }
        )
    }
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

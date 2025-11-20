package com.srg.inventory.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.srg.inventory.data.Card
import com.srg.inventory.data.CardWithQuantity
import com.srg.inventory.utils.ImageUtils
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Screen showing cards within a specific folder
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    folderId: String,
    viewModel: CollectionViewModel,
    onBackClick: () -> Unit,
    onAddCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardsWithQuantities by viewModel.cardsInCurrentFolder.collectAsState()
    val foldersWithCounts by viewModel.foldersWithCounts.collectAsState()

    val currentFolder = remember(foldersWithCounts, folderId) {
        foldersWithCounts.find { it.folder.id == folderId }?.folder
    }

    var cardToView by remember { mutableStateOf<CardWithQuantity?>(null) }
    var cardToEditQuantity by remember { mutableStateOf<CardWithQuantity?>(null) }

    LaunchedEffect(folderId) {
        viewModel.setCurrentFolder(folderId)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearCurrentFolder()
        }
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // File picker for CSV import
    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                importCsvToFolder(
                    context = context,
                    uri = it,
                    folderId = folderId,
                    viewModel = viewModel
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(currentFolder?.name ?: "Folder")
                        Text(
                            text = "${cardsWithQuantities.size} card${if (cardsWithQuantities.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Import from CSV
                    IconButton(onClick = { csvImportLauncher.launch("text/*") }) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Import from CSV")
                    }
                    // Export to CSV
                    if (cardsWithQuantities.isNotEmpty()) {
                        IconButton(onClick = {
                            exportFolderToCsv(
                                context = context,
                                folderName = currentFolder?.name ?: "folder",
                                cards = cardsWithQuantities
                            )
                        }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Export to CSV")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddCardClick) {
                Icon(Icons.Default.Add, contentDescription = "Add card")
            }
        }
    ) { paddingValues ->
        if (cardsWithQuantities.isEmpty()) {
            EmptyFolderState(
                onAddCard = onAddCardClick,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cardsWithQuantities, key = { it.card.dbUuid }) { cardWithQuantity ->
                    CardInFolderItem(
                        cardWithQuantity = cardWithQuantity,
                        onClick = { cardToView = cardWithQuantity },
                        onEditQuantityClick = { cardToEditQuantity = cardWithQuantity }
                    )
                }
            }
        }
    }

    // View card details dialog
    cardToView?.let { cardWithQty ->
        CardDetailDialog(
            card = cardWithQty.card,
            onDismiss = { cardToView = null }
        )
    }

    // Edit quantity dialog
    cardToEditQuantity?.let { cardWithQty ->
        EditQuantityDialog(
            cardName = cardWithQty.card.name,
            currentQuantity = cardWithQty.quantity,
            onDismiss = { cardToEditQuantity = null },
            onQuantityChange = { newQuantity ->
                viewModel.updateCardQuantityInFolder(folderId, cardWithQty.card.dbUuid, newQuantity)
                cardToEditQuantity = null
            },
            onDelete = {
                viewModel.removeCardFromFolder(folderId, cardWithQty.card.dbUuid)
                cardToEditQuantity = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardInFolderItem(
    cardWithQuantity: CardWithQuantity,
    onClick: () -> Unit,
    onEditQuantityClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Card icon/placeholder
            Surface(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (cardWithQuantity.card.cardType) {
                            "MainDeckCard" -> Icons.Default.Style
                            "SingleCompetitorCard" -> Icons.Default.Person
                            "TornadoCompetitorCard" -> Icons.Default.Groups
                            "TrioCompetitorCard" -> Icons.Default.Groups3
                            "EntranceCard" -> Icons.Default.MeetingRoom
                            "SpectacleCard" -> Icons.Default.AutoAwesome
                            "CrowdMeterCard" -> Icons.Default.BarChart
                            else -> Icons.Default.CreditCard
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Card info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cardWithQuantity.card.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Card type badge
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = cardWithQuantity.card.cardType.replace("Card", ""),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Additional info for specific card types
                    if (cardWithQuantity.card.isMainDeck) {
                        cardWithQuantity.card.atkType?.let { atkType ->
                            Text(
                                text = "• $atkType",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (cardWithQuantity.card.isCompetitor) {
                        cardWithQuantity.card.division?.let { division ->
                            Text(
                                text = "• $division",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Quantity badge
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "×${cardWithQuantity.quantity}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Edit quantity action
            IconButton(onClick = onEditQuantityClick) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit quantity",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun EmptyFolderState(
    onAddCard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.ContentPaste,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No cards in this folder",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add cards to start building your collection",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAddCard) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Cards")
        }
    }
}

@Composable
fun CardDetailDialog(
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
                    AsyncImage(
                        model = ImageUtils.buildCardImageRequest(context, card.dbUuid, thumbnail = false),
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
fun EditQuantityDialog(
    cardName: String,
    currentQuantity: Int,
    onDismiss: () -> Unit,
    onQuantityChange: (Int) -> Unit,
    onDelete: () -> Unit
) {
    var quantity by remember { mutableStateOf(currentQuantity) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Quantity") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = cardName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // +/- controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconButton(
                        onClick = { if (quantity > 1) quantity-- },
                        enabled = quantity > 1
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }

                    Text(
                        text = quantity.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = { quantity++ }) {
                        Icon(Icons.Default.Add, contentDescription = "Increase")
                    }
                }

                // Delete button
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Remove from folder")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onQuantityChange(quantity) },
                enabled = quantity != currentQuantity
            ) {
                Text("Save")
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

/**
 * Export folder contents to CSV and share via Android share sheet
 */
private fun exportFolderToCsv(
    context: android.content.Context,
    folderName: String,
    cards: List<CardWithQuantity>
) {
    try {
        // Build CSV content
        val csvBuilder = StringBuilder()
        csvBuilder.appendLine("Name,Quantity,Card Type,Deck #,Attack Type,Play Order,Division")

        for (cardWithQty in cards) {
            val card = cardWithQty.card
            val name = card.name.replace(",", ";").replace("\"", "'")
            val quantity = cardWithQty.quantity
            val cardType = card.cardType.replace("Card", "")
            val deckNum = card.deckCardNumber?.toString() ?: ""
            val atkType = card.atkType ?: ""
            val playOrder = card.playOrder ?: ""
            val division = card.division ?: ""

            csvBuilder.appendLine("\"$name\",$quantity,$cardType,$deckNum,$atkType,$playOrder,$division")
        }

        // Write to cache file
        val fileName = "${folderName.replace(" ", "_")}_export.csv"
        val file = File(context.cacheDir, fileName)
        file.writeText(csvBuilder.toString())

        // Share via FileProvider
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "$folderName - SRG Collection Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Export $folderName"))

    } catch (e: Exception) {
        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

/**
 * Import cards from CSV file into folder
 * Supports two formats:
 * 1. App format: Name,Quantity,Card Type,Deck #,Attack Type,Play Order,Division
 * 2. Simple format: Name,Quantity (or just Name with default quantity 1)
 */
private suspend fun importCsvToFolder(
    context: android.content.Context,
    uri: Uri,
    folderId: String,
    viewModel: CollectionViewModel
) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        if (inputStream == null) {
            Toast.makeText(context, "Could not open file", Toast.LENGTH_SHORT).show()
            return
        }

        val reader = BufferedReader(InputStreamReader(inputStream))
        val lines = reader.readLines()
        reader.close()

        if (lines.isEmpty()) {
            Toast.makeText(context, "File is empty", Toast.LENGTH_SHORT).show()
            return
        }

        var imported = 0
        var notFound = 0
        val notFoundNames = mutableListOf<String>()

        // Check if first line is a header
        val firstLine = lines.first()
        val hasHeader = firstLine.contains("name", ignoreCase = true) &&
                       (firstLine.contains("quantity", ignoreCase = true) ||
                        firstLine.contains("card_type", ignoreCase = true) ||
                        firstLine.contains("card type", ignoreCase = true))

        // Find name column index for website format (may not be first column)
        val headers = if (hasHeader) parseCsvLineParts(firstLine).map { it.trim('"', ' ').lowercase() } else emptyList()
        val nameColumnIndex = headers.indexOfFirst { it.contains("name") }.takeIf { it >= 0 } ?: 0
        val quantityColumnIndex = headers.indexOfFirst { it.contains("quantity") }

        val dataLines = if (hasHeader) lines.drop(1) else lines

        for (line in dataLines) {
            if (line.isBlank()) continue

            val parts = parseCsvLineParts(line)
            if (parts.isEmpty()) continue

            val cardName = parts.getOrNull(nameColumnIndex)?.trim('"', ' ') ?: continue
            if (cardName.isBlank()) continue

            val quantity = if (quantityColumnIndex >= 0) {
                parts.getOrNull(quantityColumnIndex)?.toIntOrNull() ?: 1
            } else {
                1
            }

            val card = viewModel.getCardByName(cardName)
            if (card != null) {
                viewModel.addCardToFolderSuspend(folderId, card.dbUuid, quantity)
                imported++
            } else {
                notFound++
                if (notFoundNames.size < 5) {
                    notFoundNames.add(cardName)
                }
            }
        }

        val message = buildString {
            append("Imported $imported card${if (imported != 1) "s" else ""}")
            if (notFound > 0) {
                append(", $notFound not found")
                if (notFoundNames.isNotEmpty()) {
                    append(": ${notFoundNames.joinToString(", ")}")
                    if (notFound > notFoundNames.size) {
                        append("...")
                    }
                }
            }
        }

        Toast.makeText(context, message, Toast.LENGTH_LONG).show()

    } catch (e: Exception) {
        Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

/**
 * Parse a CSV line into parts
 * Handles quoted values and various formats
 */
private fun parseCsvLineParts(line: String): List<String> {
    val parts = mutableListOf<String>()
    var current = StringBuilder()
    var inQuotes = false

    for (char in line) {
        when {
            char == '"' -> inQuotes = !inQuotes
            char == ',' && !inQuotes -> {
                parts.add(current.toString().trim())
                current = StringBuilder()
            }
            else -> current.append(char)
        }
    }
    parts.add(current.toString().trim())

    return parts
}

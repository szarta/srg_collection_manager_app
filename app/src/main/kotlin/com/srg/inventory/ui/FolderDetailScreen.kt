package com.srg.inventory.ui

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
    val allCardsInFolder by viewModel.cardsInCurrentFolder.collectAsState()
    val foldersWithCounts by viewModel.foldersWithCounts.collectAsState()
    val isSharing by viewModel.isSharing.collectAsState()
    val shareUrl by viewModel.shareUrl.collectAsState()

    // Filter state for folder view
    val selectedCardType by viewModel.selectedCardType.collectAsState()
    val selectedDeckCardNumbers by viewModel.selectedDeckCardNumbers.collectAsState()
    val selectedDivision by viewModel.selectedDivision.collectAsState()
    val minPower by viewModel.minPower.collectAsState()
    val minTechnique by viewModel.minTechnique.collectAsState()
    val minAgility by viewModel.minAgility.collectAsState()
    val minStrike by viewModel.minStrike.collectAsState()
    val minSubmission by viewModel.minSubmission.collectAsState()
    val minGrapple by viewModel.minGrapple.collectAsState()

    // Apply filters to cards in folder
    val cardsWithQuantities = remember(
        allCardsInFolder,
        selectedCardType,
        selectedDeckCardNumbers,
        selectedDivision,
        minPower,
        minTechnique,
        minAgility,
        minStrike,
        minSubmission,
        minGrapple
    ) {
        allCardsInFolder.filter { cardWithQty ->
            val card = cardWithQty.card

            // Card type filter
            if (selectedCardType != null && card.cardType != selectedCardType) {
                return@filter false
            }

            // Deck card number filter (multi-select)
            if (selectedDeckCardNumbers.isNotEmpty() && card.deckCardNumber != null) {
                if (!selectedDeckCardNumbers.contains(card.deckCardNumber)) {
                    return@filter false
                }
            }

            // Division filter
            if (selectedDivision != null && card.division != selectedDivision) {
                return@filter false
            }

            // Stats filters (only apply to competitors)
            if (card.isCompetitor) {
                if (card.power != null && card.power < minPower) return@filter false
                if (card.technique != null && card.technique < minTechnique) return@filter false
                if (card.agility != null && card.agility < minAgility) return@filter false
                if (card.strike != null && card.strike < minStrike) return@filter false
                if (card.submission != null && card.submission < minSubmission) return@filter false
                if (card.grapple != null && card.grapple < minGrapple) return@filter false
            }

            true
        }
    }

    val currentFolder = remember(foldersWithCounts, folderId) {
        foldersWithCounts.find { it.folder.id == folderId }?.folder
    }

    var cardToView by remember { mutableStateOf<CardWithQuantity?>(null) }
    var cardToEditQuantity by remember { mutableStateOf<CardWithQuantity?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showClearFolderDialog by remember { mutableStateOf(false) }

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
                    // Filter cards in folder
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter cards")
                    }
                    // Import from CSV (download from file)
                    IconButton(onClick = { csvImportLauncher.launch("text/csv") }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Import from CSV")
                    }
                    // Export to CSV (upload/share to file)
                    if (cardsWithQuantities.isNotEmpty()) {
                        IconButton(onClick = {
                            exportFolderToCsv(
                                context = context,
                                folderName = currentFolder?.name ?: "folder",
                                cards = cardsWithQuantities
                            )
                        }) {
                            Icon(Icons.Default.FileUpload, contentDescription = "Export to CSV")
                        }
                    }
                    // Share as QR Code
                    if (cardsWithQuantities.isNotEmpty() && currentFolder != null) {
                        IconButton(
                            onClick = {
                                viewModel.shareFolderAsQRCode(
                                    folderId = currentFolder.id,
                                    folderName = currentFolder.name
                                )
                            },
                            enabled = !isSharing
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
                    }
                    // Clear Folder
                    if (cardsWithQuantities.isNotEmpty()) {
                        IconButton(onClick = { showClearFolderDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear folder")
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
            onDismiss = { cardToView = null },
            viewModel = viewModel
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

    // Filter dialog
    if (showFilterDialog) {
        FilterDialog(
            viewModel = viewModel,
            onDismiss = { showFilterDialog = false },
            onApply = { showFilterDialog = false }
        )
    }

    // Clear folder confirmation dialog
    if (showClearFolderDialog) {
        AlertDialog(
            onDismissRequest = { showClearFolderDialog = false },
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null)
            },
            title = {
                Text("Clear Folder?")
            },
            text = {
                Column {
                    Text("Are you sure you want to clear all cards from this folder?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This will remove ${cardsWithQuantities.size} card${if (cardsWithQuantities.size != 1) "s" else ""} from the folder.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Make sure to export it first if you want to keep a backup!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearFolder(folderId)
                        showClearFolderDialog = false
                    }
                ) {
                    Text("Clear Folder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // QR Code share dialog
    shareUrl?.let { url ->
        QRCodeDialog(
            url = url,
            title = "Share Collection",
            onDismiss = { viewModel.clearShareUrl() }
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
            // Card image thumbnail
            AsyncImage(
                model = ImageUtils.buildCardImageRequest(LocalContext.current, cardWithQuantity.card.dbUuid, thumbnail = true),
                contentDescription = cardWithQuantity.card.name,
                modifier = Modifier
                    .size(64.dp)
                    .aspectRatio(0.7f),
                contentScale = ContentScale.Crop
            )

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
    onDismiss: () -> Unit,
    viewModel: CollectionViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var relatedFinishes by remember { mutableStateOf<List<Card>>(emptyList()) }
    var relatedCards by remember { mutableStateOf<List<Card>>(emptyList()) }
    var selectedRelatedCard by remember { mutableStateOf<Card?>(null) }

    LaunchedEffect(card.dbUuid) {
        scope.launch {
            relatedFinishes = viewModel.getRelatedFinishes(card.dbUuid)
            relatedCards = viewModel.getRelatedCards(card.dbUuid)
        }
    }

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
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Stats",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                CompetitorStatItemWithColor("Power", "PWR", card.power)
                                CompetitorStatItemWithColor("Technique", "TEC", card.technique)
                                CompetitorStatItemWithColor("Agility", "AGI", card.agility)
                                CompetitorStatItemWithColor("Strike", "STR", card.strike)
                                CompetitorStatItemWithColor("Submission", "SUB", card.submission)
                                CompetitorStatItemWithColor("Grapple", "GRP", card.grapple)
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

                // Related Finishes
                if (relatedFinishes.isNotEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Related Finishes",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                relatedFinishes.forEach { finish ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedRelatedCard = finish },
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(
                                                model = ImageUtils.buildCardImageRequest(context, finish.dbUuid, thumbnail = true),
                                                contentDescription = finish.name,
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .aspectRatio(0.7f),
                                                contentScale = ContentScale.Crop
                                            )
                                            Text(
                                                text = finish.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                Icons.Default.ChevronRight,
                                                contentDescription = "View",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Related Cards
                if (relatedCards.isNotEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Link,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Related Cards",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                relatedCards.forEach { relatedCard ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedRelatedCard = relatedCard },
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(
                                                model = ImageUtils.buildCardImageRequest(context, relatedCard.dbUuid, thumbnail = true),
                                                contentDescription = relatedCard.name,
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .aspectRatio(0.7f),
                                                contentScale = ContentScale.Crop
                                            )
                                            Text(
                                                text = relatedCard.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                Icons.Default.ChevronRight,
                                                contentDescription = "View",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
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

    // Show related card detail recursively
    selectedRelatedCard?.let { relatedCard ->
        CardDetailDialog(
            card = relatedCard,
            onDismiss = { selectedRelatedCard = null },
            viewModel = viewModel
        )
    }
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

@Composable
private fun CompetitorStatItemWithColor(statName: String, label: String, value: Int?) {
    val backgroundColor = when (statName) {
        "Strike" -> androidx.compose.ui.graphics.Color(0xFFFFD700) // Yellow
        "Power" -> androidx.compose.ui.graphics.Color(0xFFFF6B6B) // Red
        "Agility" -> androidx.compose.ui.graphics.Color(0xFF51CF66) // Green
        "Technique" -> androidx.compose.ui.graphics.Color(0xFFFF922B) // Orange
        "Grapple" -> androidx.compose.ui.graphics.Color(0xFF4DABF7) // Blue
        "Submission" -> androidx.compose.ui.graphics.Color(0xFFCC5DE8) // Purple
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            modifier = Modifier.size(40.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            color = backgroundColor
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value?.toString() ?: "-",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.White
                )
            }
        }
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
            val name = card.name.replace(",", "--").replace("\"", "\"\"")  // CSV standard: double-up quotes
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

            val cardName = parts.getOrNull(nameColumnIndex)?.trim()?.replace("--", ",") ?: continue
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
                notFoundNames.add(cardName)  // Store all not-found names for logging
            }
        }

        // Write not-found cards to log file
        var logFilePath: String? = null
        if (notFoundNames.isNotEmpty()) {
            try {
                val logFile = File(context.getExternalFilesDir(null), "import_not_found.log")
                logFile.bufferedWriter().use { writer ->
                    writer.write("Import Failed Cards Log\n")
                    writer.write("Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}\n")
                    writer.write("Total not found: ${notFoundNames.size}\n")
                    writer.write("---\n\n")
                    notFoundNames.forEach { name ->
                        writer.write("$name\n")
                    }
                }
                logFilePath = logFile.absolutePath
            } catch (e: Exception) {
                // If log writing fails, just continue
            }
        }

        val message = buildString {
            append("Imported $imported card${if (imported != 1) "s" else ""}")
            if (notFound > 0) {
                append(", $notFound not found")
                if (logFilePath != null) {
                    append("\nLog saved to: $logFilePath")
                } else if (notFoundNames.isNotEmpty()) {
                    val preview = notFoundNames.take(5)
                    append(": ${preview.joinToString(", ")}")
                    if (notFound > preview.size) {
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
    var i = 0

    while (i < line.length) {
        val char = line[i]
        when {
            char == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                // Escaped quote: "" becomes "
                current.append('"')
                i++ // Skip the next quote
            }
            char == '"' -> {
                // Toggle quote mode (but don't add the quote to output)
                inQuotes = !inQuotes
            }
            char == ',' && !inQuotes -> {
                // Field separator
                parts.add(current.toString().trim())
                current = StringBuilder()
            }
            else -> {
                current.append(char)
            }
        }
        i++
    }
    parts.add(current.toString().trim())

    return parts
}

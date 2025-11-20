package com.srg.inventory.ui

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
import coil.compose.AsyncImage
import com.srg.inventory.data.Card
import com.srg.inventory.data.CardWithQuantity
import com.srg.inventory.utils.ImageUtils

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
                        onViewClick = { cardToView = cardWithQuantity },
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

@Composable
fun CardInFolderItem(
    cardWithQuantity: CardWithQuantity,
    onViewClick: () -> Unit,
    onEditQuantityClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
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

            // Actions
            IconButton(onClick = onViewClick) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "View card details",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
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

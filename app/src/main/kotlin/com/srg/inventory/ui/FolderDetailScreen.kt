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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.srg.inventory.data.CardWithQuantity

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

    var cardToEdit by remember { mutableStateOf<CardWithQuantity?>(null) }

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
                        onEditClick = { cardToEdit = cardWithQuantity },
                        onRemoveClick = {
                            viewModel.removeCardFromFolder(folderId, cardWithQuantity.card.dbUuid)
                        }
                    )
                }
            }
        }
    }

    // Edit quantity dialog
    cardToEdit?.let { card ->
        EditQuantityDialog(
            cardName = card.card.name,
            currentQuantity = card.quantity,
            onDismiss = { cardToEdit = null },
            onConfirm = { newQuantity ->
                viewModel.updateCardQuantityInFolder(folderId, card.card.dbUuid, newQuantity)
                cardToEdit = null
            }
        )
    }
}

@Composable
fun CardInFolderItem(
    cardWithQuantity: CardWithQuantity,
    onEditClick: () -> Unit,
    onRemoveClick: () -> Unit,
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
            IconButton(onClick = onEditClick) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit quantity",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onRemoveClick) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove from folder",
                    tint = MaterialTheme.colorScheme.error
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
fun EditQuantityDialog(
    cardName: String,
    currentQuantity: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var quantity by remember { mutableStateOf(currentQuantity.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Quantity") },
        text = {
            Column {
                Text(
                    text = cardName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it.filter { char -> char.isDigit() } },
                    label = { Text("Quantity") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newQuantity = quantity.toIntOrNull() ?: currentQuantity
                    onConfirm(newQuantity)
                },
                enabled = quantity.isNotBlank() && quantity.toIntOrNull() != null && quantity.toInt() > 0
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

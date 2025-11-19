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
import androidx.compose.ui.unit.dp
import com.srg.inventory.data.UserCard

/**
 * Collection view screen showing owned and wanted cards
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    cards: List<UserCard>,
    filter: CollectionFilter,
    onFilterChange: (CollectionFilter) -> Unit,
    onUpdateQuantities: (UserCard, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Filter tabs
        TabRow(selectedTabIndex = filter.ordinal) {
            Tab(
                selected = filter == CollectionFilter.ALL,
                onClick = { onFilterChange(CollectionFilter.ALL) },
                text = { Text("All") }
            )
            Tab(
                selected = filter == CollectionFilter.OWNED,
                onClick = { onFilterChange(CollectionFilter.OWNED) },
                text = { Text("Owned") }
            )
            Tab(
                selected = filter == CollectionFilter.WANTED,
                onClick = { onFilterChange(CollectionFilter.WANTED) },
                text = { Text("Wishlist") }
            )
        }

        // Card list
        if (cards.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No cards yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Scan a card to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cards, key = { it.cardId }) { card ->
                    CardItem(
                        card = card,
                        onUpdateQuantities = onUpdateQuantities
                    )
                }
            }
        }
    }
}

/**
 * Individual card item in the collection list
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardItem(
    card: UserCard,
    onUpdateQuantities: (UserCard, Int, Int) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { showEditDialog = true }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.cardName,
                    style = MaterialTheme.typography.titleMedium
                )
                if (card.isCustom) {
                    Text(
                        text = "Custom card",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (card.quantityOwned > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Owned",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${card.quantityOwned}")
                    }
                }

                if (card.quantityWanted > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Wanted",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${card.quantityWanted}")
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        EditCardDialog(
            card = card,
            onDismiss = { showEditDialog = false },
            onSave = { owned, wanted ->
                onUpdateQuantities(card, owned, wanted)
                showEditDialog = false
            }
        )
    }
}

/**
 * Dialog for editing card quantities
 */
@Composable
fun EditCardDialog(
    card: UserCard,
    onDismiss: () -> Unit,
    onSave: (Int, Int) -> Unit
) {
    var ownedCount by remember { mutableStateOf(card.quantityOwned.toString()) }
    var wantedCount by remember { mutableStateOf(card.quantityWanted.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${card.cardName}") },
        text = {
            Column {
                OutlinedTextField(
                    value = ownedCount,
                    onValueChange = { ownedCount = it.filter { c -> c.isDigit() } },
                    label = { Text("Owned") },
                    leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = wantedCount,
                    onValueChange = { wantedCount = it.filter { c -> c.isDigit() } },
                    label = { Text("Wanted") },
                    leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val owned = ownedCount.toIntOrNull() ?: 0
                    val wanted = wantedCount.toIntOrNull() ?: 0
                    onSave(owned, wanted)
                }
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

enum class CollectionFilter {
    ALL, OWNED, WANTED
}

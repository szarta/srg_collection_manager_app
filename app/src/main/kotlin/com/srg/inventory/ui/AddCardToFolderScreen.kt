package com.srg.inventory.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.srg.inventory.utils.ImageUtils

/**
 * Screen for searching and adding cards to a folder
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardToFolderScreen(
    folderId: String,
    viewModel: CollectionViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val selectedCardType by viewModel.selectedCardType.collectAsState()
    val selectedAtkType by viewModel.selectedAtkType.collectAsState()
    val selectedPlayOrder by viewModel.selectedPlayOrder.collectAsState()
    val selectedDivision by viewModel.selectedDivision.collectAsState()
    val cardTypes by viewModel.cardTypes.collectAsState()
    val divisions by viewModel.divisions.collectAsState()

    var cardToAdd by remember { mutableStateOf<Card?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearFilters()
            viewModel.updateSearchQuery("")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Cards") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedCardType != null || selectedAtkType != null ||
                        selectedPlayOrder != null || selectedDivision != null) {
                        IconButton(onClick = { viewModel.clearFilters() }) {
                            Icon(Icons.Default.FilterAltOff, contentDescription = "Clear filters")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            SearchBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier.padding(16.dp)
            )

            // Filters
            FiltersSection(
                cardTypes = cardTypes,
                selectedCardType = selectedCardType,
                onCardTypeSelected = { viewModel.setCardTypeFilter(it) },
                selectedAtkType = selectedAtkType,
                onAtkTypeSelected = { viewModel.setAtkTypeFilter(it) },
                selectedPlayOrder = selectedPlayOrder,
                onPlayOrderSelected = { viewModel.setPlayOrderFilter(it) },
                divisions = divisions,
                selectedDivision = selectedDivision,
                onDivisionSelected = { viewModel.setDivisionFilter(it) },
                showMainDeckFilters = selectedCardType == "MainDeckCard",
                showCompetitorFilters = selectedCardType?.contains("Competitor") == true
            )

            Divider()

            // Results
            if (searchResults.isEmpty()) {
                EmptySearchResults(
                    hasQuery = searchQuery.isNotBlank(),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchResults, key = { it.dbUuid }) { card ->
                        SearchResultCard(
                            card = card,
                            onClick = { cardToAdd = card }
                        )
                    }
                }
            }
        }
    }

    // Add card dialog
    cardToAdd?.let { card ->
        AddCardDialog(
            card = card,
            onDismiss = { cardToAdd = null },
            onConfirm = { quantity ->
                viewModel.addCardToFolder(folderId, card.dbUuid, quantity)
                cardToAdd = null
            }
        )
    }
}

@Composable
fun SearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        label = { Text("Search cards") },
        placeholder = { Text("Card name or text...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun FiltersSection(
    cardTypes: List<String>,
    selectedCardType: String?,
    onCardTypeSelected: (String?) -> Unit,
    selectedAtkType: String?,
    onAtkTypeSelected: (String?) -> Unit,
    selectedPlayOrder: String?,
    onPlayOrderSelected: (String?) -> Unit,
    divisions: List<String>,
    selectedDivision: String?,
    onDivisionSelected: (String?) -> Unit,
    showMainDeckFilters: Boolean,
    showCompetitorFilters: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Card Type Filter
        if (cardTypes.isNotEmpty()) {
            FilterChipsRow(
                title = "Card Type",
                options = cardTypes,
                selectedOption = selectedCardType,
                onOptionSelected = onCardTypeSelected
            )
        }

        // Main Deck Filters (shown only when MainDeckCard is selected)
        if (showMainDeckFilters) {
            FilterChipsRow(
                title = "Attack Type",
                options = listOf("Strike", "Grapple", "Submission"),
                selectedOption = selectedAtkType,
                onOptionSelected = onAtkTypeSelected
            )
            FilterChipsRow(
                title = "Play Order",
                options = listOf("Lead", "Followup", "Finish"),
                selectedOption = selectedPlayOrder,
                onOptionSelected = onPlayOrderSelected
            )
        }

        // Competitor Filters (shown only when a Competitor card type is selected)
        if (showCompetitorFilters && divisions.isNotEmpty()) {
            FilterChipsRow(
                title = "Division",
                options = divisions,
                selectedOption = selectedDivision,
                onOptionSelected = onDivisionSelected
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChipsRow(
    title: String,
    options: List<String>,
    selectedOption: String?,
    onOptionSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(options) { option ->
                FilterChip(
                    selected = option == selectedOption,
                    onClick = {
                        onOptionSelected(if (option == selectedOption) null else option)
                    },
                    label = { Text(option) }
                )
            }
        }
    }
}

@Composable
fun SearchResultCard(
    card: Card,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                        imageVector = when (card.cardType) {
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
                    text = card.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = card.cardType.replace("Card", ""),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (card.isMainDeck) {
                        // Show deck card number first (most important for MainDeck cards)
                        card.deckCardNumber?.let {
                            Text(
                                text = "• #$it",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        card.atkType?.let {
                            Text(
                                text = "• $it",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        card.playOrder?.let {
                            Text(
                                text = "• $it",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (card.isCompetitor) {
                        card.division?.let {
                            Text(
                                text = "• $it",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (card.isBanned) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = "BANNED",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Icon(
                Icons.Default.Add,
                contentDescription = "Add to folder",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun EmptySearchResults(
    hasQuery: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            if (hasQuery) Icons.Default.SearchOff else Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (hasQuery) "No cards found" else "Search for cards",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (hasQuery) "Try a different search or adjust filters" else "Use the search bar to find cards",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AddCardDialog(
    card: Card,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val context = LocalContext.current
    var quantity by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Folder") },
        text = {
            Column {
                AsyncImage(
                    model = ImageUtils.buildCardImageRequest(context, card.dbUuid, thumbnail = false),
                    contentDescription = card.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.7f),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = card.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
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
                    val qty = quantity.toIntOrNull() ?: 1
                    onConfirm(qty)
                },
                enabled = quantity.isNotBlank() && quantity.toIntOrNull() != null && quantity.toInt() > 0
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

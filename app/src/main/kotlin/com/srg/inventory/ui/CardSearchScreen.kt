package com.srg.inventory.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import coil.compose.AsyncImage
import com.srg.inventory.data.Card
import com.srg.inventory.utils.ImageUtils

/**
 * Standalone card search screen for browsing cards
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardSearchScreen(
    viewModel: CollectionViewModel,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val selectedCardType by viewModel.selectedCardType.collectAsState()
    val selectedAtkType by viewModel.selectedAtkType.collectAsState()
    val selectedPlayOrder by viewModel.selectedPlayOrder.collectAsState()
    val selectedDivision by viewModel.selectedDivision.collectAsState()
    val selectedDeckCardNumber by viewModel.selectedDeckCardNumber.collectAsState()
    val cardTypes by viewModel.cardTypes.collectAsState()
    val divisions by viewModel.divisions.collectAsState()
    val cardCount by viewModel.cardCount.collectAsState()
    val searchScope by viewModel.searchScope.collectAsState()
    val inCollectionFolderId by viewModel.inCollectionFolderId.collectAsState()
    val foldersWithCounts by viewModel.foldersWithCounts.collectAsState()

    var selectedCard by remember { mutableStateOf<Card?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Card Search",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Search bar
            SearchBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                searchScope = searchScope,
                modifier = Modifier.padding(16.dp)
            )

            // Search scope selector
            SearchScopeSelector(
                selectedScope = searchScope,
                onScopeSelected = { viewModel.setSearchScope(it) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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
                showCompetitorFilters = selectedCardType?.contains("Competitor") == true,
                selectedDeckCardNumber = selectedDeckCardNumber,
                onDeckCardNumberSelected = { viewModel.setDeckCardNumberFilter(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Search button
            Button(
                onClick = onSearchClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                enabled = searchQuery.isNotBlank() || selectedCardType != null ||
                         selectedAtkType != null || selectedPlayOrder != null ||
                         selectedDivision != null || selectedDeckCardNumber != null
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Search")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Search results screen showing filtered cards
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsScreen(
    viewModel: CollectionViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchResults by viewModel.searchResults.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var selectedCard by remember { mutableStateOf<Card?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "${searchResults.size} results",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to search")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (searchResults.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.SearchOff,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No cards found",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Try adjusting your search filters",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Results list
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults, key = { it.dbUuid }) { card ->
                    BrowseCardItem(
                        card = card,
                        onClick = { selectedCard = card }
                    )
                }
            }
        }
    }

    // Card details dialog
    selectedCard?.let { card ->
        CardDetailsDialog(
            card = card,
            onDismiss = { selectedCard = null }
        )
    }
}

@Composable
fun BrowseCardItem(
    card: Card,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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
            // Card image thumbnail
            AsyncImage(
                model = ImageUtils.buildCardImageRequest(context, card.dbUuid, thumbnail = true),
                contentDescription = card.name,
                modifier = Modifier
                    .size(64.dp)
                    .aspectRatio(0.7f),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Card info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2
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
                Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CardDetailsDialog(
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
                                StatItem("PWR", card.power)
                                StatItem("AGI", card.agility)
                                StatItem("STR", card.strike)
                                StatItem("SUB", card.submission)
                                StatItem("GRP", card.grapple)
                                StatItem("TEC", card.technique)
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
                            card.atkType?.let {
                                Text("Attack Type: $it", style = MaterialTheme.typography.bodySmall)
                            }
                            card.playOrder?.let {
                                Text("Play Order: $it", style = MaterialTheme.typography.bodySmall)
                            }
                            card.deckCardNumber?.let {
                                Text("Deck #: $it", style = MaterialTheme.typography.bodySmall)
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

                // Tags
                card.tags?.let { tags ->
                    if (tags.isNotBlank()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Tags",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tags,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

                // Banned status
                if (card.isBanned) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Block,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "This card is banned",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
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
fun StatItem(label: String, value: Int?) {
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

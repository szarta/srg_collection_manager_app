package com.srg.inventory.ui

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.srg.inventory.data.Card
import com.srg.inventory.utils.ImageUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

/**
 * Single-page screen for browsing all cards (Viewer tab)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardSearchScreen(
    viewModel: CollectionViewModel,
    modifier: Modifier = Modifier
) {
    val searchResults by viewModel.searchResults.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val hasMoreResults by viewModel.hasMoreResults.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()

    var showFilterDialog by rememberSaveable { mutableStateOf(false) }
    var selectedCard by remember { mutableStateOf<Card?>(null) }

    val listState = rememberLazyListState()

    // Infinite scroll handler
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            val lastVisibleItem = visibleItems.lastOrNull()
            lastVisibleItem?.index
        }.collect { lastVisibleIndex ->
            if (lastVisibleIndex != null &&
                lastVisibleIndex >= searchResults.size - 5 &&
                hasMoreResults &&
                !isLoadingMore) {
                viewModel.loadNextPage()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Card Viewer") },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filters")
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
            // Search bar at top
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                label = { Text("Search cards") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                singleLine = true
            )

            // Results count
            if (searchResults.isNotEmpty()) {
                Text(
                    text = "${searchResults.size}+ cards",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Results list with infinite scroll
            if (searchResults.isEmpty() && searchQuery.isEmpty()) {
                // Empty state - no search yet
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Search for cards",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Type in the search box or use filters",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (searchResults.isEmpty()) {
                // Empty state - search returned no results
                Column(
                    modifier = Modifier
                        .fillMaxSize()
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
                        text = "Try adjusting your search or filters",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Results list
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchResults.size, key = { searchResults[it].dbUuid }) { index ->
                        val card = searchResults[index]
                        BrowseCardItem(
                            card = card,
                            onClick = { selectedCard = card }
                        )
                    }

                    // Loading indicator at bottom
                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }

    // Filter dialog
    if (showFilterDialog) {
        FilterDialog(
            viewModel = viewModel,
            onDismiss = { showFilterDialog = false },
            onApply = {
                showFilterDialog = false
                viewModel.applyFilters()
            }
        )
    }

    // Card details dialog
    selectedCard?.let { card ->
        CardDetailsDialog(
            card = card,
            onDismiss = { selectedCard = null },
            onCardSelected = { newCard -> selectedCard = newCard },
            viewModel = viewModel
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Card image
            AsyncImage(
                model = ImageUtils.buildCardImageRequest(context, card.dbUuid, thumbnail = true),
                contentDescription = card.name,
                modifier = Modifier
                    .size(60.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Card info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = card.cardType.replace("Card", ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (card.cardType == "MainDeckCard" && card.deckCardNumber != null) {
                    Text(
                        text = "Deck #${card.deckCardNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
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
    onDismiss: () -> Unit,
    onCardSelected: (Card) -> Unit,
    viewModel: CollectionViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var relatedFinishes by remember { mutableStateOf<List<Card>>(emptyList()) }
    var relatedCards by remember { mutableStateOf<List<Card>>(emptyList()) }

    LaunchedEffect(card.dbUuid) {
        scope.launch {
            relatedFinishes = viewModel.getRelatedFinishes(card.dbUuid)
            relatedCards = viewModel.getRelatedCards(card.dbUuid)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        modifier = if (isLandscape) Modifier.fillMaxWidth(0.95f) else Modifier.fillMaxWidth(0.9f),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
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
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        text = {
            if (isLandscape) {
                // Landscape mode: image on left, details on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Card image with zoom - constrained height for landscape
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        ZoomableImage(
                            model = ImageUtils.buildCardImageRequest(context, card.dbUuid, thumbnail = false),
                            contentDescription = card.name,
                            modifier = Modifier
                                .heightIn(max = 300.dp)
                                .aspectRatio(0.7f)
                                .align(Alignment.TopCenter)
                        )
                    }

                    // Card details
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CardDetailsContent(card, relatedFinishes, relatedCards, context, onCardSelected)
                    }
                }
            } else {
                // Portrait mode: image on top, details below
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Card image with zoom
                    item {
                        ZoomableImage(
                            model = ImageUtils.buildCardImageRequest(context, card.dbUuid, thumbnail = false),
                            contentDescription = card.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.7f)
                        )
                    }

                    // Card details
                    CardDetailsContent(card, relatedFinishes, relatedCards, context, onCardSelected)
                }
            }
        },
        confirmButton = {}
    )
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
            shape = CircleShape,
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
 * Zoomable image composable with pinch-to-zoom support
 */
@Composable
private fun ZoomableImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)

        // Only allow panning when zoomed in
        if (scale > 1f) {
            offset += offsetChange
        } else {
            offset = Offset.Zero
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
    ) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .transformable(state = state),
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * Card details content (stats, rules, related cards, etc.)
 */
private fun LazyListScope.CardDetailsContent(
    card: Card,
    relatedFinishes: List<Card>,
    relatedCards: List<Card>,
    context: android.content.Context,
    onCardSelected: (Card) -> Unit
) {
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
                                .clickable { onCardSelected(finish) },
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
                                .clickable { onCardSelected(relatedCard) },
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

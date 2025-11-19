package com.srg.inventory.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Main screen with navigation between search and collection views
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: CardViewModel = viewModel()) {
    var selectedTab by remember { mutableStateOf(0) }

    val allCards by viewModel.allUserCards.collectAsState()
    val ownedCards by viewModel.ownedCards.collectAsState()
    val wantedCards by viewModel.wantedCards.collectAsState()
    val searchSuggestions by viewModel.searchSuggestions.collectAsState()

    var collectionFilter by remember { mutableStateOf(CollectionFilter.ALL) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SRG Card Inventory") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("Search") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    label = { Text("Collection") }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> {
                    ManualAddScreen(
                        searchSuggestions = searchSuggestions,
                        onSearchQueryChanged = viewModel::searchCardNames,
                        onAddCardByName = viewModel::addCardByName
                    )
                }
                1 -> {
                    val displayCards = when (collectionFilter) {
                        CollectionFilter.ALL -> allCards
                        CollectionFilter.OWNED -> ownedCards
                        CollectionFilter.WANTED -> wantedCards
                    }

                    CollectionScreen(
                        cards = displayCards,
                        filter = collectionFilter,
                        onFilterChange = { collectionFilter = it },
                        onUpdateQuantities = viewModel::updateCardQuantities
                    )
                }
            }
        }
    }
}

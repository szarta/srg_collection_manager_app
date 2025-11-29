package com.srg.inventory.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

/**
 * Main screen with 5-tab bottom navigation
 * Tabs: Collection | Decks | Viewer | Scan | Settings
 */
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val collectionViewModel: CollectionViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(
            context.applicationContext as android.app.Application
        )
    )
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Define bottom nav items
    val bottomNavItems = listOf(
        BottomNavItem(
            route = Screen.Collection.route,
            icon = Icons.Default.Folder,
            label = "Collection"
        ),
        BottomNavItem(
            route = Screen.Decks.route,
            icon = Icons.Default.Style,
            label = "Decks"
        ),
        BottomNavItem(
            route = Screen.CardSearch.route,
            icon = Icons.Default.Search,
            label = "Viewer"
        ),
        BottomNavItem(
            route = Screen.Scan.route,
            icon = Icons.Default.QrCodeScanner,
            label = "Scan"
        ),
        BottomNavItem(
            route = Screen.Settings.route,
            icon = Icons.Default.Settings,
            label = "Settings"
        )
    )

    // Determine if we should show bottom navigation
    // Hide it on detail screens (FolderDetail, AddCardToFolder)
    val showBottomNav = currentDestination?.route in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    // Pop up to the start destination to avoid building up a large stack
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    // Avoid multiple copies of the same destination
                                    launchSingleTop = true
                                    // Restore state when reselecting a previously selected item
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        CollectionNavHost(
            navController = navController,
            viewModel = collectionViewModel,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

data class BottomNavItem(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)

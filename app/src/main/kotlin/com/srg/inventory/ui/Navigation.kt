package com.srg.inventory.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

/**
 * Navigation routes for the app
 */
sealed class Screen(val route: String) {
    // Top-level tabs
    object Collection : Screen("collection")
    object Decks : Screen("decks")
    object CardSearch : Screen("card_search")
    object Settings : Screen("settings")

    // Collection sub-screens
    object FolderDetail : Screen("folder/{folderId}") {
        fun createRoute(folderId: String) = "folder/$folderId"
    }
    object AddCardToFolder : Screen("folder/{folderId}/add") {
        fun createRoute(folderId: String) = "folder/$folderId/add"
    }

    // Deck sub-screens
    object DeckFolderDetail : Screen("deck_folder/{folderId}") {
        fun createRoute(folderId: String) = "deck_folder/$folderId"
    }
    object DeckDetail : Screen("deck/{deckId}") {
        fun createRoute(deckId: String) = "deck/$deckId"
    }
}

/**
 * Main navigation host for the Collection feature
 */
@Composable
fun CollectionNavHost(
    navController: NavHostController,
    viewModel: CollectionViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Collection.route,
        modifier = modifier
    ) {
        // Collection screen (folders list)
        composable(Screen.Collection.route) {
            FoldersScreen(
                viewModel = viewModel,
                onFolderClick = { folderId ->
                    navController.navigate(Screen.FolderDetail.createRoute(folderId))
                }
            )
        }

        // Folder detail
        composable(
            route = Screen.FolderDetail.route,
            arguments = listOf(navArgument("folderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getString("folderId") ?: return@composable
            FolderDetailScreen(
                folderId = folderId,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onAddCardClick = {
                    navController.navigate(Screen.AddCardToFolder.createRoute(folderId))
                }
            )
        }

        // Add card to folder
        composable(
            route = Screen.AddCardToFolder.route,
            arguments = listOf(navArgument("folderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getString("folderId") ?: return@composable
            AddCardToFolderScreen(
                folderId = folderId,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Decks screen
        composable(Screen.Decks.route) {
            DecksScreen(
                onFolderClick = { folderId ->
                    navController.navigate(Screen.DeckFolderDetail.createRoute(folderId))
                }
            )
        }

        // Deck folder detail (list of decks in folder)
        composable(
            route = Screen.DeckFolderDetail.route,
            arguments = listOf(navArgument("folderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getString("folderId") ?: return@composable
            DeckListScreen(
                folderId = folderId,
                onBackClick = { navController.popBackStack() },
                onDeckClick = { deckId ->
                    navController.navigate(Screen.DeckDetail.createRoute(deckId))
                }
            )
        }

        // Deck editor
        composable(
            route = Screen.DeckDetail.route,
            arguments = listOf(navArgument("deckId") { type = NavType.StringType })
        ) { backStackEntry ->
            val deckId = backStackEntry.arguments?.getString("deckId") ?: return@composable
            DeckEditorScreen(
                deckId = deckId,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Card Viewer screen
        composable(Screen.CardSearch.route) {
            CardSearchScreen(
                viewModel = viewModel
            )
        }

        // Settings screen
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel
            )
        }
    }
}

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
    object Folders : Screen("folders")
    object FolderDetail : Screen("folder/{folderId}") {
        fun createRoute(folderId: String) = "folder/$folderId"
    }
    object AddCardToFolder : Screen("folder/{folderId}/add") {
        fun createRoute(folderId: String) = "folder/$folderId/add"
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
        startDestination = Screen.Folders.route,
        modifier = modifier
    ) {
        // Folders list
        composable(Screen.Folders.route) {
            FoldersScreen(
                viewModel = viewModel,
                onFolderClick = { folderId ->
                    navController.navigate(Screen.FolderDetail.createRoute(folderId))
                },
                onSyncClick = {
                    viewModel.syncCardsFromWebsite()
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
    }
}

package com.srg.inventory.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController

/**
 * Main screen with navigation for the collection feature
 * Updated to use folder-based collection system
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

    CollectionNavHost(
        navController = navController,
        viewModel = collectionViewModel
    )
}

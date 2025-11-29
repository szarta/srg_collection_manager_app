package com.srg.inventory.ui

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.srg.inventory.api.RetrofitClient
import com.srg.inventory.api.SharedListResponse
import kotlinx.coroutines.launch

/**
 * Screen for scanning QR codes to import collections and decks
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRCodeScanScreen(
    collectionViewModel: CollectionViewModel,
    deckViewModel: DeckViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showCollectionImportDialog by remember { mutableStateOf(false) }
    var showDeckImportDialog by remember { mutableStateOf(false) }
    var sharedListData by remember { mutableStateOf<SharedListResponse?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Collect folders for import dialogs
    val folders by collectionViewModel.foldersWithCounts.collectAsStateWithLifecycle()
    val deckFolders by deckViewModel.deckFoldersWithCounts.collectAsStateWithLifecycle()
    val importSuccess by collectionViewModel.importSuccess.collectAsStateWithLifecycle()
    val deckImportSuccess by deckViewModel.importSuccess.collectAsStateWithLifecycle()

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) {
            Toast.makeText(context, "Camera permission required to scan QR codes", Toast.LENGTH_LONG).show()
        }
    }

    // QR Scanner launcher
    val scanLauncher = rememberLauncherForActivityResult(
        contract = ScanContract()
    ) { result ->
        if (result.contents != null) {
            handleScannedUrl(
                url = result.contents,
                onLoading = { isLoading = it },
                onError = { errorMessage = it },
                onSuccess = { sharedList ->
                    sharedListData = sharedList
                    when (sharedList.listType) {
                        "COLLECTION" -> showCollectionImportDialog = true
                        "DECK" -> showDeckImportDialog = true
                        else -> errorMessage = "Unknown list type: ${sharedList.listType}"
                    }
                },
                scope = scope
            )
        }
    }

    // Check permission on composition
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Show success messages
    LaunchedEffect(importSuccess) {
        if (importSuccess != null) {
            Toast.makeText(context, importSuccess, Toast.LENGTH_LONG).show()
            collectionViewModel.clearImportSuccess()
        }
    }

    LaunchedEffect(deckImportSuccess) {
        if (deckImportSuccess != null) {
            Toast.makeText(context, deckImportSuccess, Toast.LENGTH_LONG).show()
            deckViewModel.clearImportSuccess()
        }
    }

    Scaffold { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Scan QR Code",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Scan a QR code from get-diced.com to import a collection or deck",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (hasPermission) {
                            val options = ScanOptions().apply {
                                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                setPrompt("Scan a QR code from get-diced.com")
                                setBeepEnabled(true)
                                setOrientationLocked(false)
                            }
                            scanLauncher.launch(options)
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.7f),
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Scanner")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Camera permission: ${if (hasPermission) "Granted" else "Required"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hasPermission) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )

                // Show error message
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Loading overlay
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Loading shared list...")
                        }
                    }
                }
            }
        }
    }

    // Import dialogs
    if (showCollectionImportDialog && sharedListData != null) {
        ImportCollectionDialog(
            sharedListName = sharedListData?.name,
            cardCount = sharedListData?.cardUuids?.size ?: 0,
            folders = folders.map { it.folder },
            onDismiss = {
                showCollectionImportDialog = false
                sharedListData = null
            },
            onImport = { folderId, folderName ->
                val sharedId = extractSharedListId(sharedListData?.id ?: "")
                if (sharedId != null) {
                    collectionViewModel.importCollectionFromSharedList(sharedId, folderId, folderName)
                }
                showCollectionImportDialog = false
                sharedListData = null
            }
        )
    }

    if (showDeckImportDialog && sharedListData != null) {
        val deckData = sharedListData?.deckData
        if (deckData != null) {
            ImportDeckDialog(
                sharedListName = sharedListData?.name,
                spectacleType = deckData.spectacleType,
                cardCount = sharedListData?.cardUuids?.size ?: 0,
                folders = deckFolders.map { it.folder },
                onDismiss = {
                    showDeckImportDialog = false
                    sharedListData = null
                },
                onImport = { folderId, folderName ->
                    val sharedId = extractSharedListId(sharedListData?.id ?: "")
                    if (sharedId != null) {
                        deckViewModel.importDeckFromSharedList(sharedId, folderId, folderName)
                    }
                    showDeckImportDialog = false
                    sharedListData = null
                }
            )
        }
    }
}

/**
 * Parse URL and fetch shared list
 */
private fun handleScannedUrl(
    url: String,
    onLoading: (Boolean) -> Unit,
    onError: (String) -> Unit,
    onSuccess: (SharedListResponse) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val sharedId = extractSharedListId(url)

    if (sharedId == null) {
        onError("Invalid QR code. Expected a get-diced.com shared list URL.")
        return
    }

    scope.launch {
        try {
            onLoading(true)
            onError("")

            val sharedList = RetrofitClient.api.getSharedList(sharedId)
            onSuccess(sharedList)

        } catch (e: Exception) {
            onError("Failed to load shared list: ${e.message}")
        } finally {
            onLoading(false)
        }
    }
}

/**
 * Extract shared list ID from URL
 * Supports: https://get-diced.com/create-list?shared=UUID
 */
private fun extractSharedListId(url: String): String? {
    return try {
        // Check if it's already just the UUID (from the API response)
        if (url.contains("http") || url.contains("get-diced.com")) {
            // Parse query parameter
            val sharedParam = url.substringAfter("shared=", "").substringBefore("&")
            if (sharedParam.isNotBlank()) sharedParam else null
        } else {
            // Assume it's already the UUID
            url
        }
    } catch (e: Exception) {
        null
    }
}

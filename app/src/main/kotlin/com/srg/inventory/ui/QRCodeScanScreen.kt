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
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/**
 * Screen for scanning QR codes to import collections and decks
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRCodeScanScreen(
    onScanResult: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }

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
            onScanResult(result.contents)
        }
    }

    // Check permission on composition
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Scaffold { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
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
                modifier = Modifier.fillMaxWidth(0.7f)
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
        }
    }
}

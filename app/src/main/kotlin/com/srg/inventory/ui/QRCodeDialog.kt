package com.srg.inventory.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Dialog that displays a QR code for a shareable URL
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRCodeDialog(
    url: String,
    title: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // QR Code
                val qrBitmap = generateQRCode(url, 512)
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.size(300.dp)
                    )
                } else {
                    Text(
                        text = "Failed to generate QR code",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // URL text (clickable)
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodySmall.copy(
                        textDecoration = TextDecoration.Underline
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Instructions
                Text(
                    text = "Scan this QR code to view or import this list",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Generate a QR code bitmap from a URL
 */
private fun generateQRCode(content: String, size: Int): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(
                    x,
                    y,
                    if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                )
            }
        }
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

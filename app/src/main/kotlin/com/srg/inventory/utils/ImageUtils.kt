package com.srg.inventory.utils

import android.content.Context
import coil.request.ImageRequest
import java.io.File

/**
 * Utility functions for loading card images
 *
 * Image loading strategy:
 * 1. Check synced images in internal storage (downloaded from server)
 * 2. Fall back to bundled mobile assets
 * 3. Show placeholder if unavailable
 */
object ImageUtils {

    private const val BASE_URL = "https://get-diced.com/images"
    private const val SYNCED_IMAGES_DIR = "synced_images"

    /**
     * Get the synced image file if it exists
     */
    fun getSyncedImageFile(context: Context, cardUuid: String): File? {
        val first2 = cardUuid.take(2)
        val file = File(context.filesDir, "$SYNCED_IMAGES_DIR/$first2/$cardUuid.webp")
        return if (file.exists()) file else null
    }

    /**
     * Get the asset path for a bundled card image
     */
    fun getAssetPath(cardUuid: String, thumbnail: Boolean = true): String {
        val first2 = cardUuid.take(2)
        // Always use mobile-optimized images
        return "mobile/$first2/$cardUuid.webp"
    }

    /**
     * Get the URL for a card image from the server
     */
    fun getImageUrl(cardUuid: String, thumbnail: Boolean = true): String {
        val first2 = cardUuid.take(2)
        val type = if (thumbnail) "thumbnails" else "fullsize"
        return "$BASE_URL/$type/$first2/$cardUuid.webp"
    }

    /**
     * Check if an image exists in bundled assets
     */
    fun imageExistsInAssets(context: Context, cardUuid: String, thumbnail: Boolean = true): Boolean {
        return try {
            val assetPath = getAssetPath(cardUuid, thumbnail)
            context.assets.open(assetPath).use { true }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Build an ImageRequest that loads from synced images first, then bundled assets
     */
    fun buildCardImageRequest(
        context: Context,
        cardUuid: String,
        thumbnail: Boolean = true
    ): ImageRequest {
        // Check synced images first
        val syncedFile = getSyncedImageFile(context, cardUuid)
        if (syncedFile != null) {
            return ImageRequest.Builder(context)
                .data(syncedFile)
                .error(com.srg.inventory.R.drawable.ic_launcher_foreground)
                .crossfade(true)
                .build()
        }

        // Fall back to bundled assets
        val assetPath = getAssetPath(cardUuid, thumbnail)
        val assetUri = "file:///android_asset/$assetPath"

        return ImageRequest.Builder(context)
            .data(assetUri)
            .error(com.srg.inventory.R.drawable.ic_launcher_foreground)
            .crossfade(true)
            .build()
    }

    /**
     * Build an ImageRequest that loads from server with fallback
     */
    fun buildServerImageRequest(
        context: Context,
        cardUuid: String,
        thumbnail: Boolean = true
    ): ImageRequest {
        val url = getImageUrl(cardUuid, thumbnail)

        return ImageRequest.Builder(context)
            .data(url)
            .error(com.srg.inventory.R.drawable.ic_launcher_foreground)
            .crossfade(true)
            .build()
    }
}

package com.srg.inventory.utils

import android.content.Context
import coil.request.ImageRequest
import java.io.File

/**
 * Utility functions for loading card images
 *
 * Image loading strategy:
 * 1. Check bundled assets (thumbnails/{first2}/{uuid}.webp)
 * 2. Check downloaded cache
 * 3. Load from server (https://get-diced.com/images/thumbnails/{first2}/{uuid}.webp)
 * 4. Show placeholder if unavailable
 */
object ImageUtils {

    private const val BASE_URL = "https://get-diced.com/images"

    /**
     * Get the asset path for a bundled card image
     */
    fun getAssetPath(cardUuid: String, thumbnail: Boolean = true): String {
        val first2 = cardUuid.take(2)
        val type = if (thumbnail) "thumbnails" else "fullsize"
        return "$type/$first2/$cardUuid.webp"
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
     * Build an ImageRequest that tries assets first, then server
     */
    fun buildCardImageRequest(
        context: Context,
        cardUuid: String,
        thumbnail: Boolean = true
    ): ImageRequest {
        // Try to load from assets first
        val assetPath = getAssetPath(cardUuid, thumbnail)
        val assetUri = "file:///android_asset/$assetPath"

        return ImageRequest.Builder(context)
            .data(assetUri)
            .error(com.srg.inventory.R.drawable.ic_launcher_foreground) // Fallback to app icon for now
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
            .error(com.srg.inventory.R.drawable.ic_launcher_foreground) // Fallback to app icon for now
            .crossfade(true)
            .build()
    }
}

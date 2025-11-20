package com.srg.inventory.api

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Repository for syncing card images from server.
 * Compares local manifest hashes with server manifest and downloads changed/new images.
 */
class ImageSyncRepository(private val context: Context) {

    private val api = RetrofitClient.api
    private val httpClient = OkHttpClient()
    private val gson = Gson()

    companion object {
        private const val TAG = "ImageSyncRepository"
        private const val IMAGES_DIR = "synced_images"
        private const val LOCAL_MANIFEST_FILE = "local_manifest.json"
    }

    /**
     * Get the directory where synced images are stored
     */
    fun getSyncedImagesDir(): File {
        return File(context.filesDir, IMAGES_DIR).apply { mkdirs() }
    }

    /**
     * Check if we have a synced version of an image
     */
    fun getSyncedImageFile(uuid: String): File? {
        val first2 = uuid.take(2)
        val file = File(getSyncedImagesDir(), "$first2/$uuid.webp")
        return if (file.exists()) file else null
    }

    /**
     * Load the bundled manifest from assets
     */
    private fun loadBundledManifest(): ImageManifest? {
        return try {
            context.assets.open("images_manifest.json").bufferedReader().use { reader ->
                gson.fromJson(reader, ImageManifest::class.java)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bundled manifest: ${e.message}")
            null
        }
    }

    /**
     * Load the local manifest (for synced images)
     */
    private fun loadLocalManifest(): ImageManifest? {
        val file = File(context.filesDir, LOCAL_MANIFEST_FILE)
        return if (file.exists()) {
            try {
                gson.fromJson(file.readText(), ImageManifest::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading local manifest: ${e.message}")
                null
            }
        } else {
            null
        }
    }

    /**
     * Save the local manifest after sync
     */
    private fun saveLocalManifest(manifest: ImageManifest) {
        val file = File(context.filesDir, LOCAL_MANIFEST_FILE)
        file.writeText(gson.toJson(manifest))
    }

    /**
     * Get combined local hashes (bundled + synced)
     */
    private fun getLocalHashes(): Map<String, String> {
        val hashes = mutableMapOf<String, String>()

        // Start with bundled manifest
        loadBundledManifest()?.images?.forEach { (uuid, info) ->
            hashes[uuid] = info.hash
        }

        // Override with synced manifest (newer versions)
        loadLocalManifest()?.images?.forEach { (uuid, info) ->
            hashes[uuid] = info.hash
        }

        return hashes
    }

    /**
     * Sync images from server.
     * Returns: Pair<downloaded count, total to sync>
     */
    suspend fun syncImages(
        onProgress: (downloaded: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting image sync...")

            // Fetch server manifest
            val serverManifest = api.getImageManifest()
            Log.d(TAG, "Server manifest: ${serverManifest.image_count} images")

            // Get local hashes
            val localHashes = getLocalHashes()
            Log.d(TAG, "Local hashes: ${localHashes.size} images")

            // Find images that need syncing (missing or different hash)
            val toSync = serverManifest.images.filter { (uuid, serverInfo) ->
                val localHash = localHashes[uuid]
                localHash == null || localHash != serverInfo.hash
            }

            Log.d(TAG, "Images to sync: ${toSync.size}")

            if (toSync.isEmpty()) {
                return@withContext Result.success(Pair(0, 0))
            }

            // Download images
            var downloaded = 0
            val syncedImages = mutableMapOf<String, ImageInfo>()

            toSync.forEach { (uuid, serverInfo) ->
                try {
                    downloadImage(uuid, serverInfo.path)
                    syncedImages[uuid] = serverInfo
                    downloaded++
                    onProgress(downloaded, toSync.size)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to download image $uuid: ${e.message}")
                }
            }

            // Update local manifest with synced images
            if (syncedImages.isNotEmpty()) {
                val existingLocal = loadLocalManifest()
                val updatedImages = (existingLocal?.images ?: emptyMap()) + syncedImages
                val updatedManifest = ImageManifest(
                    version = serverManifest.version,
                    generated = serverManifest.generated,
                    image_count = updatedImages.size,
                    images = updatedImages
                )
                saveLocalManifest(updatedManifest)
            }

            Log.d(TAG, "Image sync complete. Downloaded: $downloaded/${toSync.size}")
            Result.success(Pair(downloaded, toSync.size))

        } catch (e: Exception) {
            Log.e(TAG, "Error syncing images: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Download a single image from server
     */
    private suspend fun downloadImage(uuid: String, path: String) = withContext(Dispatchers.IO) {
        val url = "${GetDicedApi.BASE_URL}images/mobile/$path"
        val request = Request.Builder().url(url).build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Failed to download: ${response.code}")
            }

            val first2 = uuid.take(2)
            val dir = File(getSyncedImagesDir(), first2).apply { mkdirs() }
            val file = File(dir, "$uuid.webp")

            response.body?.byteStream()?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    /**
     * Get sync status: how many images need syncing
     */
    suspend fun getSyncStatus(): Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        try {
            val serverManifest = api.getImageManifest()
            val localHashes = getLocalHashes()

            val toSync = serverManifest.images.count { (uuid, serverInfo) ->
                val localHash = localHashes[uuid]
                localHash == null || localHash != serverInfo.hash
            }

            Result.success(Pair(toSync, serverManifest.image_count))
        } catch (e: Exception) {
            Log.e(TAG, "Error checking sync status: ${e.message}")
            Result.failure(e)
        }
    }
}

package com.srg.inventory.api

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/**
 * Repository for syncing card database from server.
 * Compares local database hash with server manifest and downloads/merges when needed.
 * Preserves user data (folders, folder_cards, decks, deck_cards) while replacing card data.
 */
class CardSyncRepository(private val context: Context) {

    private val api = RetrofitClient.api
    private val httpClient = OkHttpClient()

    companion object {
        private const val TAG = "CardSyncRepository"
        private const val MANIFEST_PREF = "card_sync_prefs"
        private const val KEY_LAST_HASH = "last_db_hash"
        private const val KEY_LAST_SYNC = "last_sync_time"
    }

    private val prefs = context.getSharedPreferences(MANIFEST_PREF, Context.MODE_PRIVATE)

    /**
     * Get the stored hash of the last synced database
     */
    private fun getLastSyncedHash(): String? {
        return prefs.getString(KEY_LAST_HASH, null)
    }

    /**
     * Save the hash after successful sync
     */
    private fun saveLastSyncedHash(hash: String) {
        prefs.edit()
            .putString(KEY_LAST_HASH, hash)
            .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
            .apply()
    }

    /**
     * Get time since last sync in human-readable format
     */
    fun getLastSyncTimeString(): String {
        val lastSyncTime = prefs.getLong(KEY_LAST_SYNC, 0)
        if (lastSyncTime == 0L) return "Never"

        val currentTime = System.currentTimeMillis()
        val diffMs = currentTime - lastSyncTime

        val days = diffMs / (1000 * 60 * 60 * 24)
        val hours = (diffMs / (1000 * 60 * 60)) % 24
        val minutes = (diffMs / (1000 * 60)) % 60

        return when {
            days > 0 -> "$days day${if (days > 1) "s" else ""} ago"
            hours > 0 -> "$hours hour${if (hours > 1) "s" else ""} ago"
            minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
            else -> "Just now"
        }
    }

    /**
     * Check if sync is needed by comparing hashes
     * Returns: Pair<needsSync, serverCardCount>
     */
    suspend fun checkSyncNeeded(): Result<Pair<Boolean, Int>> = withContext(Dispatchers.IO) {
        try {
            val serverManifest = api.getCardsManifest()
            val localHash = getLastSyncedHash()

            val needsSync = localHash == null || localHash != serverManifest.hash
            Result.success(Pair(needsSync, serverManifest.card_count))
        } catch (e: Exception) {
            Log.e(TAG, "Error checking sync status: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Sync card database from server.
     * Downloads new database and merges card data while preserving user data.
     */
    suspend fun syncDatabase(
        onProgress: (status: String) -> Unit = {}
    ): Result<SyncResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting card database sync...")
            onProgress("Checking for updates...")

            // Fetch server manifest
            val serverManifest = api.getCardsManifest()
            val localHash = getLastSyncedHash()

            if (localHash == serverManifest.hash) {
                Log.d(TAG, "Database already up to date")
                return@withContext Result.success(SyncResult(
                    success = true,
                    cardsUpdated = 0,
                    alreadyUpToDate = true
                ))
            }

            onProgress("Downloading database...")
            Log.d(TAG, "Downloading new database (${serverManifest.card_count} cards, ${serverManifest.size_bytes} bytes)")

            // Download database to temp file
            val tempDbFile = File(context.cacheDir, "temp_cards.db")
            downloadDatabase(tempDbFile)

            onProgress("Merging card data...")
            Log.d(TAG, "Merging card data...")

            // Merge the downloaded database into the user database
            val cardsUpdated = mergeCardData(tempDbFile)

            // Clean up temp file
            tempDbFile.delete()

            // Save the hash on success
            saveLastSyncedHash(serverManifest.hash)

            Log.d(TAG, "Sync complete. Updated $cardsUpdated cards")
            Result.success(SyncResult(
                success = true,
                cardsUpdated = cardsUpdated,
                alreadyUpToDate = false
            ))

        } catch (e: Exception) {
            Log.e(TAG, "Error syncing database: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Download database file from server
     */
    private suspend fun downloadDatabase(outputFile: File) = withContext(Dispatchers.IO) {
        val url = "${GetDicedApi.BASE_URL}api/cards/database"
        val request = Request.Builder().url(url).build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Failed to download database: ${response.code}")
            }

            response.body?.byteStream()?.use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    /**
     * Merge card data from downloaded database into user database.
     * Preserves user data (folders, folder_cards, decks, deck_cards).
     * Returns number of cards updated.
     */
    private fun mergeCardData(sourceDbFile: File): Int {
        val userDbPath = context.getDatabasePath("user_cards.db").absolutePath

        // Open both databases
        val sourceDb = SQLiteDatabase.openDatabase(sourceDbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        val userDb = SQLiteDatabase.openDatabase(userDbPath, null, SQLiteDatabase.OPEN_READWRITE)

        var cardsUpdated = 0

        try {
            // Enable foreign keys (required for CASCADE deletes to work)
            userDb.execSQL("PRAGMA foreign_keys = ON")

            userDb.beginTransaction()

            // Drop temp table if it exists from a previous failed sync
            userDb.execSQL("DROP TABLE IF EXISTS folder_cards_backup")

            // Backup folder_cards before deleting cards (to preserve user's collection data)
            Log.d(TAG, "Backing up folder_cards...")
            userDb.execSQL("CREATE TEMP TABLE folder_cards_backup AS SELECT * FROM folder_cards")

            val backupCursor = userDb.rawQuery("SELECT COUNT(*) FROM folder_cards_backup", null)
            backupCursor.use { cursor ->
                if (cursor.moveToFirst()) {
                    val backupCount = cursor.getInt(0)
                    Log.d(TAG, "Backed up $backupCount folder_cards entries")
                }
            }

            // Clear existing card data (but preserve user data)
            userDb.execSQL("DELETE FROM card_related_finishes")
            userDb.execSQL("DELETE FROM card_related_cards")
            userDb.execSQL("DELETE FROM cards")  // This cascades and deletes folder_cards due to foreign key

            // Copy cards from source database
            val cardsCursor = sourceDb.rawQuery("SELECT * FROM cards", null)
            cardsCursor.use { cursor ->
                while (cursor.moveToNext()) {
                    val values = android.content.ContentValues()
                    for (i in 0 until cursor.columnCount) {
                        val name = cursor.getColumnName(i)
                        when (cursor.getType(i)) {
                            android.database.Cursor.FIELD_TYPE_NULL -> values.putNull(name)
                            android.database.Cursor.FIELD_TYPE_INTEGER -> values.put(name, cursor.getLong(i))
                            android.database.Cursor.FIELD_TYPE_FLOAT -> values.put(name, cursor.getDouble(i))
                            android.database.Cursor.FIELD_TYPE_STRING -> values.put(name, cursor.getString(i))
                            android.database.Cursor.FIELD_TYPE_BLOB -> values.put(name, cursor.getBlob(i))
                        }
                    }
                    userDb.insert("cards", null, values)
                    cardsUpdated++
                }
            }

            // Copy related finishes
            val finishesCursor = sourceDb.rawQuery("SELECT * FROM card_related_finishes", null)
            finishesCursor.use { cursor ->
                while (cursor.moveToNext()) {
                    val values = android.content.ContentValues()
                    values.put("card_uuid", cursor.getString(cursor.getColumnIndexOrThrow("card_uuid")))
                    values.put("finish_uuid", cursor.getString(cursor.getColumnIndexOrThrow("finish_uuid")))
                    userDb.insert("card_related_finishes", null, values)
                }
            }

            // Copy related cards
            val relatedCursor = sourceDb.rawQuery("SELECT * FROM card_related_cards", null)
            relatedCursor.use { cursor ->
                while (cursor.moveToNext()) {
                    val values = android.content.ContentValues()
                    values.put("card_uuid", cursor.getString(cursor.getColumnIndexOrThrow("card_uuid")))
                    values.put("related_uuid", cursor.getString(cursor.getColumnIndexOrThrow("related_uuid")))
                    userDb.insert("card_related_cards", null, values)
                }
            }

            // Verify folder_cards was cleared by cascade
            val afterDeleteCursor = userDb.rawQuery("SELECT COUNT(*) FROM folder_cards", null)
            afterDeleteCursor.use { cursor ->
                if (cursor.moveToFirst()) {
                    val remainingCount = cursor.getInt(0)
                    Log.d(TAG, "After cascade delete: $remainingCount folder_cards entries (should be 0)")
                    if (remainingCount > 0) {
                        Log.w(TAG, "CASCADE delete didn't work properly, manually clearing folder_cards")
                        userDb.execSQL("DELETE FROM folder_cards")
                    }
                }
            }

            // Restore folder_cards from backup (only for cards that still exist)
            Log.d(TAG, "Restoring folder_cards...")
            userDb.execSQL("""
                INSERT OR REPLACE INTO folder_cards (folder_id, card_uuid, quantity, added_at)
                SELECT fcb.folder_id, fcb.card_uuid, fcb.quantity, fcb.added_at
                FROM folder_cards_backup fcb
                INNER JOIN cards c ON fcb.card_uuid = c.db_uuid
            """)

            // Log how many folder_cards were restored
            val restoredCursor = userDb.rawQuery("SELECT COUNT(*) FROM folder_cards", null)
            restoredCursor.use { cursor ->
                if (cursor.moveToFirst()) {
                    val restoredCount = cursor.getInt(0)
                    Log.d(TAG, "Restored $restoredCount folder_cards entries")
                }
            }

            // Clean up temp table
            userDb.execSQL("DROP TABLE IF EXISTS folder_cards_backup")

            userDb.setTransactionSuccessful()
        } finally {
            userDb.endTransaction()
            userDb.close()
            sourceDb.close()
        }

        return cardsUpdated
    }
}

/**
 * Result of a database sync operation
 */
data class SyncResult(
    val success: Boolean,
    val cardsUpdated: Int,
    val alreadyUpToDate: Boolean = false
)

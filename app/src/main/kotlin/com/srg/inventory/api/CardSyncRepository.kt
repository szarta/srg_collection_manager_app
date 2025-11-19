package com.srg.inventory.api

import android.util.Log
import com.srg.inventory.data.CardDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for syncing cards from get-diced.com API to local database
 */
class CardSyncRepository(
    private val api: GetDicedApi,
    private val cardDao: CardDao
) {
    companion object {
        private const val TAG = "CardSyncRepository"
        private const val BATCH_SIZE = 100
    }

    /**
     * Sync state for tracking progress
     */
    data class SyncProgress(
        val currentBatch: Int,
        val totalFetched: Int,
        val isComplete: Boolean,
        val error: String? = null
    )

    /**
     * Sync all cards from the website API to local database
     * Returns SyncProgress with final state
     */
    suspend fun syncAllCards(): Result<SyncProgress> = withContext(Dispatchers.IO) {
        try {
            var offset = 0
            var totalFetched = 0
            var batchNumber = 1

            Log.d(TAG, "Starting card sync from get-diced.com")

            do {
                Log.d(TAG, "Fetching batch $batchNumber (offset: $offset)")

                val response = api.searchCards(
                    limit = BATCH_SIZE,
                    offset = offset
                )

                val cardsToInsert = response.items.toEntities()

                // Insert batch into database
                cardDao.insertCards(cardsToInsert)

                totalFetched += response.items.size
                offset += BATCH_SIZE
                batchNumber++

                Log.d(TAG, "Synced $totalFetched / ${response.totalCount} cards")

            } while (response.items.size == BATCH_SIZE && totalFetched < response.totalCount)

            val finalProgress = SyncProgress(
                currentBatch = batchNumber - 1,
                totalFetched = totalFetched,
                isComplete = true
            )

            Log.d(TAG, "Card sync completed. Total cards: $totalFetched")
            Result.success(finalProgress)

        } catch (e: Exception) {
            Log.e(TAG, "Error syncing cards: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Sync specific cards by UUIDs (for updating existing cards or fetching related cards)
     */
    suspend fun syncCardsByUuids(uuids: List<String>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (uuids.isEmpty()) {
                return@withContext Result.success(0)
            }

            val response = api.getCardsByUuids(CardBatchRequest(uuids))
            val cards = response.rows.toEntities()

            cardDao.insertCards(cards)

            if (response.missing.isNotEmpty()) {
                Log.w(TAG, "Missing cards: ${response.missing}")
            }

            Result.success(cards.size)

        } catch (e: Exception) {
            Log.e(TAG, "Error syncing cards by UUIDs: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get a single card from API and save to database
     */
    suspend fun syncCardByUuid(uuid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cardDto = api.getCard(uuid)
            val card = cardDto.toEntity()
            cardDao.insertCard(card)

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error syncing card $uuid: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Check if sync is needed (e.g., more than 7 days since last sync)
     */
    suspend fun isSyncNeeded(maxAgeDays: Int = 7): Boolean = withContext(Dispatchers.IO) {
        try {
            val lastSyncTime = cardDao.getLastSyncTime() ?: return@withContext true
            val currentTime = System.currentTimeMillis()
            val daysSinceSync = (currentTime - lastSyncTime) / (1000 * 60 * 60 * 24)

            daysSinceSync >= maxAgeDays

        } catch (e: Exception) {
            Log.e(TAG, "Error checking sync status: ${e.message}", e)
            true // If error, assume sync is needed
        }
    }

    /**
     * Get time since last sync in human-readable format
     */
    suspend fun getLastSyncTimeString(): String = withContext(Dispatchers.IO) {
        try {
            val lastSyncTime = cardDao.getLastSyncTime() ?: return@withContext "Never"
            val currentTime = System.currentTimeMillis()
            val diffMs = currentTime - lastSyncTime

            val days = diffMs / (1000 * 60 * 60 * 24)
            val hours = (diffMs / (1000 * 60 * 60)) % 24
            val minutes = (diffMs / (1000 * 60)) % 60

            when {
                days > 0 -> "$days day${if (days > 1) "s" else ""} ago"
                hours > 0 -> "$hours hour${if (hours > 1) "s" else ""} ago"
                minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
                else -> "Just now"
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error getting last sync time: ${e.message}", e)
            "Unknown"
        }
    }
}

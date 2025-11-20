package com.srg.inventory.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for managing cards synced from get-diced.com
 */
@Dao
interface CardDao {

    @Query("SELECT * FROM cards ORDER BY name ASC")
    fun getAllCards(): Flow<List<Card>>

    @Query("SELECT * FROM cards WHERE db_uuid = :uuid")
    suspend fun getCardByUuid(uuid: String): Card?

    @Query("SELECT * FROM cards WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getCardByName(name: String): Card?

    @Query("SELECT * FROM cards WHERE name LIKE '%' || :searchQuery || '%' COLLATE NOCASE ORDER BY name LIMIT 50")
    fun searchCards(searchQuery: String): Flow<List<Card>>

    @Query("SELECT * FROM cards WHERE card_type = :cardType ORDER BY name ASC")
    fun getCardsByType(cardType: String): Flow<List<Card>>

    @Query("SELECT DISTINCT card_type FROM cards ORDER BY card_type ASC")
    suspend fun getAllCardTypes(): List<String>

    @Query("SELECT DISTINCT release_set FROM cards WHERE release_set IS NOT NULL ORDER BY release_set ASC")
    suspend fun getAllReleaseSets(): List<String>

    @Query("SELECT DISTINCT division FROM cards WHERE division IS NOT NULL ORDER BY division ASC")
    suspend fun getAllDivisions(): List<String>

    // Search with filters
    @Query("""
        SELECT * FROM cards
        WHERE (:searchQuery IS NULL OR name LIKE '%' || :searchQuery || '%' COLLATE NOCASE
               OR rules_text LIKE '%' || :searchQuery || '%' COLLATE NOCASE)
        AND (:cardType IS NULL OR card_type = :cardType)
        AND (:atkType IS NULL OR atk_type = :atkType)
        AND (:playOrder IS NULL OR play_order = :playOrder)
        AND (:division IS NULL OR division = :division)
        AND (:releaseSet IS NULL OR release_set = :releaseSet)
        AND (:isBanned IS NULL OR is_banned = :isBanned)
        ORDER BY name ASC
        LIMIT :limit
    """)
    fun searchCardsWithFilters(
        searchQuery: String?,
        cardType: String?,
        atkType: String?,
        playOrder: String?,
        division: String?,
        releaseSet: String?,
        isBanned: Boolean?,
        limit: Int = 100
    ): Flow<List<Card>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: Card)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<Card>)

    @Update
    suspend fun updateCard(card: Card)

    @Delete
    suspend fun deleteCard(card: Card)

    @Query("DELETE FROM cards")
    suspend fun deleteAllCards()

    @Query("SELECT COUNT(*) FROM cards")
    suspend fun getCardCount(): Int

    @Query("SELECT synced_at FROM cards ORDER BY synced_at DESC LIMIT 1")
    suspend fun getLastSyncTime(): Long?
}

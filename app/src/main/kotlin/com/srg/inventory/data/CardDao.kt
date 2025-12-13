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

    @Query("SELECT * FROM cards WHERE card_type = :cardType ORDER BY name ASC")
    suspend fun getCardsByTypeSuspend(cardType: String): List<Card>

    @Query("SELECT * FROM cards WHERE card_type = :cardType AND deck_card_number = :deckCardNumber ORDER BY name ASC")
    suspend fun getCardsByTypeAndNumber(cardType: String, deckCardNumber: Int): List<Card>

    @Query("SELECT * FROM cards ORDER BY name ASC")
    suspend fun getAllCardsSuspend(): List<Card>

    @Query("SELECT DISTINCT card_type FROM cards ORDER BY card_type ASC")
    suspend fun getAllCardTypes(): List<String>

    @Query("SELECT DISTINCT release_set FROM cards WHERE release_set IS NOT NULL ORDER BY release_set ASC")
    suspend fun getAllReleaseSets(): List<String>

    @Query("SELECT DISTINCT division FROM cards WHERE division IS NOT NULL ORDER BY division ASC")
    suspend fun getAllDivisions(): List<String>

    // Search with filters - multi-select support for scopes and deck numbers
    @Query("""
        SELECT DISTINCT cards.* FROM cards
        WHERE (
            :searchQuery IS NULL OR (
                (:searchName AND name LIKE '%' || :searchQuery || '%' COLLATE NOCASE) OR
                (:searchTags AND tags LIKE '%' || :searchQuery || '%' COLLATE NOCASE) OR
                (:searchRulesText AND rules_text LIKE '%' || :searchQuery || '%' COLLATE NOCASE)
            )
        )
        AND (:cardType IS NULL OR card_type = :cardType)
        AND (:division IS NULL OR division = :division)
        AND (CASE WHEN :hasDeckNumbers THEN deck_card_number IN (:deckCardNumbers) ELSE 1 END)
        AND (power IS NULL OR power >= :minPower)
        AND (technique IS NULL OR technique >= :minTechnique)
        AND (agility IS NULL OR agility >= :minAgility)
        AND (strike IS NULL OR strike >= :minStrike)
        AND (submission IS NULL OR submission >= :minSubmission)
        AND (grapple IS NULL OR grapple >= :minGrapple)
        ORDER BY name ASC
        LIMIT :limit OFFSET :offset
    """)
    fun searchCardsWithFilters(
        searchQuery: String?,
        searchName: Boolean,
        searchTags: Boolean,
        searchRulesText: Boolean,
        cardType: String?,
        division: String?,
        deckCardNumbers: List<Int>,
        hasDeckNumbers: Boolean,
        minPower: Int,
        minTechnique: Int,
        minAgility: Int,
        minStrike: Int,
        minSubmission: Int,
        minGrapple: Int,
        limit: Int,
        offset: Int
    ): Flow<List<Card>>

    // Get card name suggestions for autocomplete
    @Query("""
        SELECT DISTINCT name FROM cards
        WHERE (:prefix = '' OR name LIKE :prefix || '%' COLLATE NOCASE)
        AND (:cardType IS NULL OR card_type = :cardType)
        AND (:atkType IS NULL OR atk_type = :atkType)
        AND (:playOrder IS NULL OR play_order = :playOrder)
        AND (:division IS NULL OR division = :division)
        AND (:deckCardNumber IS NULL OR deck_card_number = :deckCardNumber)
        ORDER BY name ASC
        LIMIT :limit
    """)
    suspend fun getCardNameSuggestions(
        prefix: String,
        cardType: String?,
        atkType: String?,
        playOrder: String?,
        division: String?,
        deckCardNumber: Int?,
        limit: Int = 20
    ): List<String>

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

    // Related cards queries
    @Query("""
        SELECT cards.* FROM cards
        INNER JOIN card_related_finishes ON cards.db_uuid = card_related_finishes.finish_uuid
        WHERE card_related_finishes.card_uuid = :cardUuid
        ORDER BY cards.name ASC
    """)
    suspend fun getRelatedFinishes(cardUuid: String): List<Card>

    @Query("""
        SELECT cards.* FROM cards
        INNER JOIN card_related_cards ON cards.db_uuid = card_related_cards.related_uuid
        WHERE card_related_cards.card_uuid = :cardUuid
        ORDER BY cards.name ASC
    """)
    suspend fun getRelatedCards(cardUuid: String): List<Card>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelatedFinish(relatedFinish: CardRelatedFinish)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelatedFinishes(relatedFinishes: List<CardRelatedFinish>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelatedCard(relatedCard: CardRelatedCard)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelatedCards(relatedCards: List<CardRelatedCard>)
}

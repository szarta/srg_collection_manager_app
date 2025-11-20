package com.srg.inventory.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Spectacle type for a deck
 */
enum class SpectacleType {
    NEWMAN,
    VALIANT
}

/**
 * Slot type for cards in a deck
 */
enum class DeckSlotType {
    ENTRANCE,
    COMPETITOR,
    DECK,        // Deck cards 1-30
    FINISH,
    ALTERNATE
}

/**
 * A deck within a deck folder
 */
@Entity(
    tableName = "decks",
    foreignKeys = [
        ForeignKey(
            entity = DeckFolder::class,
            parentColumns = ["id"],
            childColumns = ["folder_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("folder_id")]
)
data class Deck(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "folder_id")
    val folderId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "spectacle_type")
    val spectacleType: SpectacleType = SpectacleType.VALIANT,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "modified_at")
    val modifiedAt: Long = System.currentTimeMillis()
)

/**
 * A card slot in a deck
 */
@Entity(
    tableName = "deck_cards",
    primaryKeys = ["deck_id", "slot_type", "slot_number"],
    foreignKeys = [
        ForeignKey(
            entity = Deck::class,
            parentColumns = ["id"],
            childColumns = ["deck_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("deck_id"), Index("card_uuid")]
)
data class DeckCard(
    @ColumnInfo(name = "deck_id")
    val deckId: String,

    @ColumnInfo(name = "card_uuid")
    val cardUuid: String,

    @ColumnInfo(name = "slot_type")
    val slotType: DeckSlotType,

    @ColumnInfo(name = "slot_number")
    val slotNumber: Int = 0  // 1-30 for DECK type, 0 for others, increments for multiple FINISH/ALTERNATE
)

/**
 * Deck with its card count
 */
data class DeckWithCardCount(
    @Embedded val deck: Deck,
    @ColumnInfo(name = "card_count") val cardCount: Int
)

/**
 * Card with its deck slot info
 */
data class DeckCardWithDetails(
    @Embedded val card: Card,
    @ColumnInfo(name = "slot_type") val slotType: DeckSlotType,
    @ColumnInfo(name = "slot_number") val slotNumber: Int
)

@Dao
interface DeckDao {
    @Query("SELECT * FROM decks WHERE folder_id = :folderId ORDER BY name ASC")
    fun getDecksInFolder(folderId: String): Flow<List<Deck>>

    @Query("""
        SELECT d.*, COUNT(dc.card_uuid) as card_count
        FROM decks d
        LEFT JOIN deck_cards dc ON d.id = dc.deck_id
        WHERE d.folder_id = :folderId
        GROUP BY d.id
        ORDER BY d.name ASC
    """)
    fun getDecksWithCardCount(folderId: String): Flow<List<DeckWithCardCount>>

    @Query("SELECT * FROM decks WHERE id = :deckId")
    suspend fun getDeckById(deckId: String): Deck?

    @Query("SELECT * FROM decks WHERE id = :deckId")
    fun getDeckByIdFlow(deckId: String): Flow<Deck?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: Deck)

    @Update
    suspend fun updateDeck(deck: Deck)

    @Delete
    suspend fun deleteDeck(deck: Deck)

    @Query("DELETE FROM decks WHERE id = :deckId")
    suspend fun deleteDeckById(deckId: String)
}

@Dao
interface DeckCardDao {
    @Query("""
        SELECT c.*, dc.slot_type, dc.slot_number
        FROM deck_cards dc
        INNER JOIN cards c ON dc.card_uuid = c.db_uuid
        WHERE dc.deck_id = :deckId
        ORDER BY
            CASE dc.slot_type
                WHEN 'ENTRANCE' THEN 1
                WHEN 'COMPETITOR' THEN 2
                WHEN 'DECK' THEN 3
                WHEN 'FINISH' THEN 4
                WHEN 'ALTERNATE' THEN 5
            END,
            dc.slot_number ASC
    """)
    fun getCardsInDeck(deckId: String): Flow<List<DeckCardWithDetails>>

    @Query("SELECT * FROM deck_cards WHERE deck_id = :deckId")
    suspend fun getDeckCards(deckId: String): List<DeckCard>

    @Query("SELECT * FROM deck_cards WHERE deck_id = :deckId AND slot_type = :slotType AND slot_number = :slotNumber")
    suspend fun getDeckCard(deckId: String, slotType: DeckSlotType, slotNumber: Int): DeckCard?

    @Query("SELECT card_uuid FROM deck_cards WHERE deck_id = :deckId")
    suspend fun getCardUuidsInDeck(deckId: String): List<String>

    @Query("SELECT MAX(slot_number) FROM deck_cards WHERE deck_id = :deckId AND slot_type = :slotType")
    suspend fun getMaxSlotNumber(deckId: String, slotType: DeckSlotType): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeckCard(deckCard: DeckCard)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeckCards(deckCards: List<DeckCard>)

    @Delete
    suspend fun deleteDeckCard(deckCard: DeckCard)

    @Query("DELETE FROM deck_cards WHERE deck_id = :deckId AND slot_type = :slotType AND slot_number = :slotNumber")
    suspend fun deleteDeckCardBySlot(deckId: String, slotType: DeckSlotType, slotNumber: Int)

    @Query("DELETE FROM deck_cards WHERE deck_id = :deckId")
    suspend fun clearDeck(deckId: String)

    @Query("SELECT COUNT(*) FROM deck_cards WHERE deck_id = :deckId")
    fun getCardCountInDeck(deckId: String): Flow<Int>
}

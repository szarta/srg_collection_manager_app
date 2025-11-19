package com.srg.inventory.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for managing many-to-many relationship between folders and cards
 */
@Dao
interface FolderCardDao {

    // Get all cards in a specific folder
    @Query("""
        SELECT c.* FROM cards c
        INNER JOIN folder_cards fc ON c.db_uuid = fc.card_uuid
        WHERE fc.folder_id = :folderId
        ORDER BY c.name ASC
    """)
    fun getCardsInFolder(folderId: String): Flow<List<Card>>

    // Get all folders containing a specific card
    @Query("""
        SELECT f.* FROM folders f
        INNER JOIN folder_cards fc ON f.id = fc.folder_id
        WHERE fc.card_uuid = :cardUuid
        ORDER BY f.display_order ASC
    """)
    fun getFoldersForCard(cardUuid: String): Flow<List<Folder>>

    // Get card with quantity in specific folder
    @Query("""
        SELECT c.*, fc.quantity, fc.added_at FROM cards c
        INNER JOIN folder_cards fc ON c.db_uuid = fc.card_uuid
        WHERE fc.folder_id = :folderId AND fc.card_uuid = :cardUuid
    """)
    suspend fun getCardInFolder(folderId: String, cardUuid: String): CardWithQuantity?

    // Get all cards with quantities for a folder
    @Query("""
        SELECT c.*, fc.quantity, fc.added_at FROM cards c
        INNER JOIN folder_cards fc ON c.db_uuid = fc.card_uuid
        WHERE fc.folder_id = :folderId
        ORDER BY c.name ASC
    """)
    fun getCardsWithQuantitiesInFolder(folderId: String): Flow<List<CardWithQuantity>>

    // Count cards in folder
    @Query("SELECT COUNT(*) FROM folder_cards WHERE folder_id = :folderId")
    fun getCardCountInFolder(folderId: String): Flow<Int>

    // Check if card exists in folder
    @Query("SELECT COUNT(*) > 0 FROM folder_cards WHERE folder_id = :folderId AND card_uuid = :cardUuid")
    suspend fun isCardInFolder(folderId: String, cardUuid: String): Boolean

    // Get quantity of card in folder
    @Query("SELECT quantity FROM folder_cards WHERE folder_id = :folderId AND card_uuid = :cardUuid")
    suspend fun getQuantityInFolder(folderId: String, cardUuid: String): Int?

    // Add card to folder
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolderCard(folderCard: FolderCard)

    // Add multiple cards to folder
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolderCards(folderCards: List<FolderCard>)

    // Update quantity
    @Query("UPDATE folder_cards SET quantity = :quantity WHERE folder_id = :folderId AND card_uuid = :cardUuid")
    suspend fun updateQuantity(folderId: String, cardUuid: String, quantity: Int)

    // Remove card from folder
    @Query("DELETE FROM folder_cards WHERE folder_id = :folderId AND card_uuid = :cardUuid")
    suspend fun removeCardFromFolder(folderId: String, cardUuid: String)

    // Remove all cards from folder
    @Query("DELETE FROM folder_cards WHERE folder_id = :folderId")
    suspend fun removeAllCardsFromFolder(folderId: String)

    // Remove card from all folders
    @Query("DELETE FROM folder_cards WHERE card_uuid = :cardUuid")
    suspend fun removeCardFromAllFolders(cardUuid: String)

    @Delete
    suspend fun deleteFolderCard(folderCard: FolderCard)
}

/**
 * Data class representing a card with its quantity in a folder
 */
data class CardWithQuantity(
    @Embedded val card: Card,
    val quantity: Int,
    @ColumnInfo(name = "added_at") val addedAt: Long
)

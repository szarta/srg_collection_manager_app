package com.srg.inventory.data

import kotlinx.coroutines.flow.Flow
import android.util.Log

/**
 * Repository for managing folder-based card collections
 */
class CollectionRepository(
    private val folderDao: FolderDao,
    private val cardDao: CardDao,
    private val folderCardDao: FolderCardDao
) {
    companion object {
        private const val TAG = "CollectionRepository"
    }

    // ==================== Folder Operations ====================

    fun getAllFolders(): Flow<List<Folder>> = folderDao.getAllFolders()

    fun getDefaultFolders(): Flow<List<Folder>> = folderDao.getDefaultFolders()

    fun getCustomFolders(): Flow<List<Folder>> = folderDao.getCustomFolders()

    suspend fun getFolderById(folderId: String): Folder? = folderDao.getFolderById(folderId)

    suspend fun getFolderByName(name: String): Folder? = folderDao.getFolderByName(name)

    suspend fun createFolder(name: String, displayOrder: Int? = null): Folder {
        val order = displayOrder ?: (folderDao.getCustomFolderCount() + 3) // After 3 defaults
        val folder = Folder(
            name = name,
            isDefault = false,
            displayOrder = order
        )
        folderDao.insertFolder(folder)
        return folder
    }

    suspend fun updateFolder(folder: Folder) = folderDao.updateFolder(folder)

    suspend fun deleteFolder(folder: Folder) {
        if (folder.isDefault) {
            Log.w(TAG, "Cannot delete default folder: ${folder.name}")
            return
        }
        folderDao.deleteFolder(folder)
    }

    suspend fun ensureDefaultFolders() {
        val owned = folderDao.getFolderById("owned")
        val wanted = folderDao.getFolderById("wanted")
        val trade = folderDao.getFolderById("trade")

        val foldersToCreate = mutableListOf<Folder>()

        if (owned == null) {
            foldersToCreate.add(Folder(id = "owned", name = "Owned", isDefault = true, displayOrder = 0))
        }
        if (wanted == null) {
            foldersToCreate.add(Folder(id = "wanted", name = "Wanted", isDefault = true, displayOrder = 1))
        }
        if (trade == null) {
            foldersToCreate.add(Folder(id = "trade", name = "Trade", isDefault = true, displayOrder = 2))
        }

        if (foldersToCreate.isNotEmpty()) {
            folderDao.insertFolders(foldersToCreate)
        }
    }

    // ==================== Card Operations ====================

    fun getAllCards(): Flow<List<Card>> = cardDao.getAllCards()

    suspend fun getCardByUuid(uuid: String): Card? = cardDao.getCardByUuid(uuid)

    suspend fun getCardByName(name: String): Card? = cardDao.getCardByName(name)

    fun searchCards(query: String): Flow<List<Card>> = cardDao.searchCards(query)

    fun getCardsByType(cardType: String): Flow<List<Card>> = cardDao.getCardsByType(cardType)

    fun searchCardsWithFilters(
        searchQuery: String?,
        searchScope: String = "all",
        cardType: String?,
        atkType: String?,
        playOrder: String?,
        division: String?,
        releaseSet: String?,
        isBanned: Boolean?,
        inCollectionFolderId: String? = null,
        limit: Int = 100
    ): Flow<List<Card>> = cardDao.searchCardsWithFilters(
        searchQuery, searchScope, cardType, atkType, playOrder, division, releaseSet, isBanned, inCollectionFolderId, limit
    )

    suspend fun getAllCardTypes(): List<String> = cardDao.getAllCardTypes()

    suspend fun getAllReleaseSets(): List<String> = cardDao.getAllReleaseSets()

    suspend fun getAllDivisions(): List<String> = cardDao.getAllDivisions()

    suspend fun insertCard(card: Card) = cardDao.insertCard(card)

    suspend fun insertCards(cards: List<Card>) = cardDao.insertCards(cards)

    suspend fun getCardCount(): Int = cardDao.getCardCount()

    suspend fun getLastSyncTime(): Long? = cardDao.getLastSyncTime()

    // ==================== Folder-Card Operations ====================

    fun getCardsInFolder(folderId: String): Flow<List<Card>> =
        folderCardDao.getCardsInFolder(folderId)

    fun getCardsWithQuantitiesInFolder(folderId: String): Flow<List<CardWithQuantity>> =
        folderCardDao.getCardsWithQuantitiesInFolder(folderId)

    fun getFoldersForCard(cardUuid: String): Flow<List<Folder>> =
        folderCardDao.getFoldersForCard(cardUuid)

    fun getCardCountInFolder(folderId: String): Flow<Int> =
        folderCardDao.getCardCountInFolder(folderId)

    suspend fun addCardToFolder(folderId: String, cardUuid: String, quantity: Int = 1) {
        val existingQuantity = folderCardDao.getQuantityInFolder(folderId, cardUuid)
        if (existingQuantity != null) {
            // Card already in folder, update quantity
            folderCardDao.updateQuantity(folderId, cardUuid, existingQuantity + quantity)
        } else {
            // Add new card to folder
            val folderCard = FolderCard(
                folderId = folderId,
                cardUuid = cardUuid,
                quantity = quantity
            )
            folderCardDao.insertFolderCard(folderCard)
        }
    }

    suspend fun setCardQuantityInFolder(folderId: String, cardUuid: String, quantity: Int) {
        if (quantity <= 0) {
            folderCardDao.removeCardFromFolder(folderId, cardUuid)
        } else {
            val existsInFolder = folderCardDao.isCardInFolder(folderId, cardUuid)
            if (existsInFolder) {
                folderCardDao.updateQuantity(folderId, cardUuid, quantity)
            } else {
                val folderCard = FolderCard(
                    folderId = folderId,
                    cardUuid = cardUuid,
                    quantity = quantity
                )
                folderCardDao.insertFolderCard(folderCard)
            }
        }
    }

    suspend fun removeCardFromFolder(folderId: String, cardUuid: String) =
        folderCardDao.removeCardFromFolder(folderId, cardUuid)

    suspend fun moveCardBetweenFolders(
        fromFolderId: String,
        toFolderId: String,
        cardUuid: String,
        quantity: Int? = null
    ) {
        val currentQuantity = quantity ?: folderCardDao.getQuantityInFolder(fromFolderId, cardUuid) ?: 1

        // Remove from source folder
        folderCardDao.removeCardFromFolder(fromFolderId, cardUuid)

        // Add to destination folder
        addCardToFolder(toFolderId, cardUuid, currentQuantity)
    }

    suspend fun isCardInFolder(folderId: String, cardUuid: String): Boolean =
        folderCardDao.isCardInFolder(folderId, cardUuid)

    suspend fun getQuantityInFolder(folderId: String, cardUuid: String): Int? =
        folderCardDao.getQuantityInFolder(folderId, cardUuid)
}

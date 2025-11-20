package com.srg.inventory.data

import android.util.Log
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing decks
 */
class DeckRepository(
    private val deckFolderDao: DeckFolderDao,
    private val deckDao: DeckDao,
    private val deckCardDao: DeckCardDao,
    private val cardDao: CardDao
) {
    companion object {
        private const val TAG = "DeckRepository"
    }

    // ==================== Deck Folder Operations ====================

    fun getAllDeckFolders(): Flow<List<DeckFolder>> = deckFolderDao.getAllDeckFolders()

    fun getDefaultDeckFolders(): Flow<List<DeckFolder>> = deckFolderDao.getDefaultDeckFolders()

    fun getCustomDeckFolders(): Flow<List<DeckFolder>> = deckFolderDao.getCustomDeckFolders()

    suspend fun getDeckFolderById(folderId: String): DeckFolder? = deckFolderDao.getDeckFolderById(folderId)

    suspend fun getDeckFolderByName(name: String): DeckFolder? = deckFolderDao.getDeckFolderByName(name)

    suspend fun createDeckFolder(name: String, displayOrder: Int? = null): DeckFolder {
        val order = displayOrder ?: (deckFolderDao.getCustomDeckFolderCount() + 4) // After 4 defaults
        val folder = DeckFolder(
            name = name,
            isDefault = false,
            displayOrder = order
        )
        deckFolderDao.insertDeckFolder(folder)
        return folder
    }

    suspend fun updateDeckFolder(folder: DeckFolder) = deckFolderDao.updateDeckFolder(folder)

    suspend fun deleteDeckFolder(folder: DeckFolder) {
        if (folder.isDefault) {
            Log.w(TAG, "Cannot delete default deck folder: ${folder.name}")
            return
        }
        deckFolderDao.deleteDeckFolder(folder)
    }

    fun getDeckCountInFolder(folderId: String): Flow<Int> = deckFolderDao.getDeckCountInFolder(folderId)

    suspend fun ensureDefaultDeckFolders() {
        val singles = deckFolderDao.getDeckFolderById("singles")
        val tornado = deckFolderDao.getDeckFolderById("tornado")
        val trios = deckFolderDao.getDeckFolderById("trios")
        val tag = deckFolderDao.getDeckFolderById("tag")

        val foldersToCreate = mutableListOf<DeckFolder>()

        if (singles == null) {
            foldersToCreate.add(DeckFolder(id = "singles", name = "Singles", isDefault = true, displayOrder = 0))
        }
        if (tornado == null) {
            foldersToCreate.add(DeckFolder(id = "tornado", name = "Tornado", isDefault = true, displayOrder = 1))
        }
        if (trios == null) {
            foldersToCreate.add(DeckFolder(id = "trios", name = "Trios", isDefault = true, displayOrder = 2))
        }
        if (tag == null) {
            foldersToCreate.add(DeckFolder(id = "tag", name = "Tag", isDefault = true, displayOrder = 3))
        }

        if (foldersToCreate.isNotEmpty()) {
            deckFolderDao.insertDeckFolders(foldersToCreate)
        }
    }

    // ==================== Deck Operations ====================

    fun getDecksInFolder(folderId: String): Flow<List<Deck>> = deckDao.getDecksInFolder(folderId)

    fun getDecksWithCardCount(folderId: String): Flow<List<DeckWithCardCount>> = deckDao.getDecksWithCardCount(folderId)

    suspend fun getDeckById(deckId: String): Deck? = deckDao.getDeckById(deckId)

    fun getDeckByIdFlow(deckId: String): Flow<Deck?> = deckDao.getDeckByIdFlow(deckId)

    suspend fun createDeck(folderId: String, name: String, spectacleType: SpectacleType = SpectacleType.VALIANT): Deck {
        val deck = Deck(
            folderId = folderId,
            name = name,
            spectacleType = spectacleType
        )
        deckDao.insertDeck(deck)
        return deck
    }

    suspend fun updateDeck(deck: Deck) {
        val updated = deck.copy(modifiedAt = System.currentTimeMillis())
        deckDao.updateDeck(updated)
    }

    suspend fun deleteDeck(deck: Deck) = deckDao.deleteDeck(deck)

    suspend fun deleteDeckById(deckId: String) = deckDao.deleteDeckById(deckId)

    // ==================== Deck Card Operations ====================

    fun getCardsInDeck(deckId: String): Flow<List<DeckCardWithDetails>> = deckCardDao.getCardsInDeck(deckId)

    suspend fun getDeckCards(deckId: String): List<DeckCard> = deckCardDao.getDeckCards(deckId)

    suspend fun getCardUuidsInDeck(deckId: String): List<String> = deckCardDao.getCardUuidsInDeck(deckId)

    fun getCardCountInDeck(deckId: String): Flow<Int> = deckCardDao.getCardCountInDeck(deckId)

    suspend fun setEntrance(deckId: String, cardUuid: String) {
        val deckCard = DeckCard(
            deckId = deckId,
            cardUuid = cardUuid,
            slotType = DeckSlotType.ENTRANCE,
            slotNumber = 0
        )
        deckCardDao.insertDeckCard(deckCard)
        markDeckModified(deckId)
    }

    suspend fun setCompetitor(deckId: String, cardUuid: String) {
        val deckCard = DeckCard(
            deckId = deckId,
            cardUuid = cardUuid,
            slotType = DeckSlotType.COMPETITOR,
            slotNumber = 0
        )
        deckCardDao.insertDeckCard(deckCard)
        markDeckModified(deckId)
    }

    suspend fun setDeckCard(deckId: String, cardUuid: String, slotNumber: Int) {
        if (slotNumber < 1 || slotNumber > 30) {
            Log.w(TAG, "Invalid deck card slot number: $slotNumber")
            return
        }
        val deckCard = DeckCard(
            deckId = deckId,
            cardUuid = cardUuid,
            slotType = DeckSlotType.DECK,
            slotNumber = slotNumber
        )
        deckCardDao.insertDeckCard(deckCard)
        markDeckModified(deckId)
    }

    suspend fun addFinish(deckId: String, cardUuid: String) {
        val maxSlot = deckCardDao.getMaxSlotNumber(deckId, DeckSlotType.FINISH) ?: 0
        val deckCard = DeckCard(
            deckId = deckId,
            cardUuid = cardUuid,
            slotType = DeckSlotType.FINISH,
            slotNumber = maxSlot + 1
        )
        deckCardDao.insertDeckCard(deckCard)
        markDeckModified(deckId)
    }

    suspend fun addAlternate(deckId: String, cardUuid: String) {
        val maxSlot = deckCardDao.getMaxSlotNumber(deckId, DeckSlotType.ALTERNATE) ?: 0
        val deckCard = DeckCard(
            deckId = deckId,
            cardUuid = cardUuid,
            slotType = DeckSlotType.ALTERNATE,
            slotNumber = maxSlot + 1
        )
        deckCardDao.insertDeckCard(deckCard)
        markDeckModified(deckId)
    }

    suspend fun removeCardFromDeck(deckId: String, slotType: DeckSlotType, slotNumber: Int) {
        deckCardDao.deleteDeckCardBySlot(deckId, slotType, slotNumber)
        markDeckModified(deckId)
    }

    suspend fun clearDeck(deckId: String) {
        deckCardDao.clearDeck(deckId)
        markDeckModified(deckId)
    }

    private suspend fun markDeckModified(deckId: String) {
        val deck = deckDao.getDeckById(deckId)
        deck?.let {
            deckDao.updateDeck(it.copy(modifiedAt = System.currentTimeMillis()))
        }
    }

    // ==================== Helper Operations ====================

    suspend fun getCardByUuid(uuid: String): Card? = cardDao.getCardByUuid(uuid)

    suspend fun getCardByName(name: String): Card? = cardDao.getCardByName(name)

    /**
     * Get finishes that are linked to a specific competitor
     * Note: This requires the related_finishes data to be in the database
     * For now, returns empty list - will be implemented when finish linking is added
     */
    suspend fun getLinkedFinishes(competitorUuid: String): List<Card> {
        // TODO: Implement when related_finishes is added to Card entity
        return emptyList()
    }

    /**
     * Get all unlinked finishes (finishes without a specific competitor)
     * For now, returns all finish-type cards
     */
    suspend fun getUnlinkedFinishes(): List<Card> {
        // TODO: Filter to only unlinked finishes
        return emptyList()
    }
}

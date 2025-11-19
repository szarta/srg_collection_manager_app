package com.srg.inventory.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing user card collection
 */
class CardRepository(
    private val userCardDao: UserCardDao
) {
    // User collection operations
    fun getAllUserCards(): Flow<List<UserCard>> = userCardDao.getAllCards()

    fun getOwnedCards(): Flow<List<UserCard>> = userCardDao.getOwnedCards()

    fun getWantedCards(): Flow<List<UserCard>> = userCardDao.getWantedCards()

    suspend fun getUserCard(cardId: String): UserCard? = userCardDao.getCardById(cardId)

    suspend fun addOrUpdateCard(card: UserCard) = userCardDao.insertCard(card)

    suspend fun deleteCard(card: UserCard) = userCardDao.deleteCard(card)

    // Search operations
    suspend fun searchCardNames(query: String): List<String> {
        return try {
            userCardDao.searchCardNames(query)
        } catch (e: Exception) {
            android.util.Log.e("CardRepository", "Error searching user cards: ${e.message}", e)
            emptyList()
        }
    }
}

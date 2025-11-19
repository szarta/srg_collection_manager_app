package com.srg.inventory.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for managing user's card collection and wishlist
 */
@Dao
interface UserCardDao {

    @Query("SELECT * FROM user_cards ORDER BY card_name ASC")
    fun getAllCards(): Flow<List<UserCard>>

    @Query("SELECT * FROM user_cards WHERE quantity_owned > 0 ORDER BY card_name ASC")
    fun getOwnedCards(): Flow<List<UserCard>>

    @Query("SELECT * FROM user_cards WHERE quantity_wanted > 0 ORDER BY card_name ASC")
    fun getWantedCards(): Flow<List<UserCard>>

    @Query("SELECT * FROM user_cards WHERE card_id = :cardId")
    suspend fun getCardById(cardId: String): UserCard?

    @Query("SELECT DISTINCT card_name FROM user_cards WHERE card_name LIKE '%' || :searchQuery || '%' COLLATE NOCASE ORDER BY card_name LIMIT 50")
    suspend fun searchCardNames(searchQuery: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: UserCard)

    @Update
    suspend fun updateCard(card: UserCard)

    @Delete
    suspend fun deleteCard(card: UserCard)

    @Query("DELETE FROM user_cards WHERE card_id = :cardId")
    suspend fun deleteCardById(cardId: String)
}

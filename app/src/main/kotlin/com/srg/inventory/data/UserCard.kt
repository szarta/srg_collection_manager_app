package com.srg.inventory.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a card in the user's collection or wishlist
 */
@Entity(tableName = "user_cards")
data class UserCard(
    @PrimaryKey
    @ColumnInfo(name = "card_id")
    val cardId: String,  // UUID from card_hashes or custom identifier

    @ColumnInfo(name = "card_name")
    val cardName: String,  // Display name (from db_uuid or custom)

    @ColumnInfo(name = "quantity_owned")
    val quantityOwned: Int = 0,

    @ColumnInfo(name = "quantity_wanted")
    val quantityWanted: Int = 0,

    @ColumnInfo(name = "is_custom")
    val isCustom: Boolean = false,  // True if not found in card_hashes.db

    @ColumnInfo(name = "added_timestamp")
    val addedTimestamp: Long = System.currentTimeMillis()
)

package com.srg.inventory.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Junction table representing many-to-many relationship between folders and cards
 * A card can exist in multiple folders with different quantities
 */
@Entity(
    tableName = "folder_cards",
    primaryKeys = ["folder_id", "card_uuid"],
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folder_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Card::class,
            parentColumns = ["db_uuid"],
            childColumns = ["card_uuid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["folder_id"]),
        Index(value = ["card_uuid"])
    ]
)
data class FolderCard(
    @ColumnInfo(name = "folder_id")
    val folderId: String,

    @ColumnInfo(name = "card_uuid")
    val cardUuid: String,

    @ColumnInfo(name = "quantity")
    val quantity: Int = 1,

    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis()
)

package com.srg.inventory.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Entity representing a collection folder (Owned, Wanted, Trade, or custom)
 */
@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,  // true for Owned, Wanted, Trade

    @ColumnInfo(name = "display_order")
    val displayOrder: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

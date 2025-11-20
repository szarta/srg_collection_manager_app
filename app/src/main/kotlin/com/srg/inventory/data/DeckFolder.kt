package com.srg.inventory.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Folder for organizing decks by type (Singles, Tornado, Trios, Tag, custom)
 */
@Entity(tableName = "deck_folders")
data class DeckFolder(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,

    @ColumnInfo(name = "display_order")
    val displayOrder: Int = 0
)

@Dao
interface DeckFolderDao {
    @Query("SELECT * FROM deck_folders ORDER BY display_order ASC, name ASC")
    fun getAllDeckFolders(): Flow<List<DeckFolder>>

    @Query("SELECT * FROM deck_folders WHERE is_default = 1 ORDER BY display_order ASC")
    fun getDefaultDeckFolders(): Flow<List<DeckFolder>>

    @Query("SELECT * FROM deck_folders WHERE is_default = 0 ORDER BY display_order ASC, name ASC")
    fun getCustomDeckFolders(): Flow<List<DeckFolder>>

    @Query("SELECT * FROM deck_folders WHERE id = :folderId")
    suspend fun getDeckFolderById(folderId: String): DeckFolder?

    @Query("SELECT * FROM deck_folders WHERE name = :name LIMIT 1")
    suspend fun getDeckFolderByName(name: String): DeckFolder?

    @Query("SELECT COUNT(*) FROM deck_folders WHERE is_default = 0")
    suspend fun getCustomDeckFolderCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeckFolder(folder: DeckFolder)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeckFolders(folders: List<DeckFolder>)

    @Update
    suspend fun updateDeckFolder(folder: DeckFolder)

    @Delete
    suspend fun deleteDeckFolder(folder: DeckFolder)

    @Query("SELECT COUNT(*) FROM decks WHERE folder_id = :folderId")
    fun getDeckCountInFolder(folderId: String): Flow<Int>
}

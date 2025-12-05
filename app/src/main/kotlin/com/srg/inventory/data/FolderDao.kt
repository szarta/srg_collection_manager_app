package com.srg.inventory.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for managing collection folders
 */
@Dao
interface FolderDao {

    @Query("SELECT * FROM folders ORDER BY display_order ASC, created_at ASC")
    fun getAllFolders(): Flow<List<Folder>>

    @Query("SELECT * FROM folders ORDER BY display_order ASC, created_at ASC")
    suspend fun getAllFoldersList(): List<Folder>

    @Query("SELECT * FROM folders WHERE is_default = 1 ORDER BY display_order ASC")
    fun getDefaultFolders(): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE is_default = 0 ORDER BY display_order ASC")
    fun getCustomFolders(): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE id = :folderId")
    suspend fun getFolderById(folderId: String): Folder?

    @Query("SELECT * FROM folders WHERE name = :name LIMIT 1")
    suspend fun getFolderByName(name: String): Folder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<Folder>)

    @Update
    suspend fun updateFolder(folder: Folder)

    @Delete
    suspend fun deleteFolder(folder: Folder)

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteFolderById(folderId: String)

    @Query("SELECT COUNT(*) FROM folders WHERE is_default = 0")
    suspend fun getCustomFolderCount(): Int
}

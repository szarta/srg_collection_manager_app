package com.srg.inventory.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database for user's card collection and wishlist
 * Version 2: Added folder-based collection system
 */
@Database(
    entities = [
        UserCard::class,  // Legacy table (kept for migration)
        Folder::class,
        Card::class,
        FolderCard::class
    ],
    version = 2,
    exportSchema = false
)
abstract class UserCardDatabase : RoomDatabase() {
    abstract fun userCardDao(): UserCardDao
    abstract fun folderDao(): FolderDao
    abstract fun cardDao(): CardDao
    abstract fun folderCardDao(): FolderCardDao

    companion object {
        @Volatile
        private var INSTANCE: UserCardDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create folders table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS folders (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        is_default INTEGER NOT NULL DEFAULT 0,
                        display_order INTEGER NOT NULL DEFAULT 0,
                        created_at INTEGER NOT NULL
                    )
                """)

                // Create cards table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS cards (
                        db_uuid TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        card_type TEXT NOT NULL,
                        rules_text TEXT,
                        errata_text TEXT,
                        is_banned INTEGER NOT NULL DEFAULT 0,
                        release_set TEXT,
                        srg_url TEXT,
                        srgpc_url TEXT,
                        comments TEXT,
                        tags TEXT,
                        power INTEGER,
                        agility INTEGER,
                        strike INTEGER,
                        submission INTEGER,
                        grapple INTEGER,
                        technique INTEGER,
                        division TEXT,
                        gender TEXT,
                        deck_card_number INTEGER,
                        atk_type TEXT,
                        play_order TEXT,
                        synced_at INTEGER NOT NULL
                    )
                """)

                // Create folder_cards junction table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS folder_cards (
                        folder_id TEXT NOT NULL,
                        card_uuid TEXT NOT NULL,
                        quantity INTEGER NOT NULL DEFAULT 1,
                        added_at INTEGER NOT NULL,
                        PRIMARY KEY (folder_id, card_uuid),
                        FOREIGN KEY (folder_id) REFERENCES folders(id) ON DELETE CASCADE,
                        FOREIGN KEY (card_uuid) REFERENCES cards(db_uuid) ON DELETE CASCADE
                    )
                """)

                // Create indices for foreign keys
                database.execSQL("CREATE INDEX IF NOT EXISTS index_folder_cards_folder_id ON folder_cards(folder_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_folder_cards_card_uuid ON folder_cards(card_uuid)")

                // Insert default folders
                val timestamp = System.currentTimeMillis()
                database.execSQL("""
                    INSERT INTO folders (id, name, is_default, display_order, created_at)
                    VALUES
                        ('owned', 'Owned', 1, 0, $timestamp),
                        ('wanted', 'Wanted', 1, 1, $timestamp),
                        ('trade', 'Trade', 1, 2, $timestamp)
                """)

                // Migrate existing user_cards data
                // For cards with quantity_owned > 0, create Card entry and add to Owned folder
                database.execSQL("""
                    INSERT INTO cards (db_uuid, name, card_type, is_banned, synced_at)
                    SELECT
                        card_id,
                        card_name,
                        'MainDeckCard',
                        0,
                        added_timestamp
                    FROM user_cards
                    WHERE quantity_owned > 0 OR quantity_wanted > 0
                """)

                // Add owned cards to Owned folder
                database.execSQL("""
                    INSERT INTO folder_cards (folder_id, card_uuid, quantity, added_at)
                    SELECT
                        'owned',
                        card_id,
                        quantity_owned,
                        added_timestamp
                    FROM user_cards
                    WHERE quantity_owned > 0
                """)

                // Add wanted cards to Wanted folder
                database.execSQL("""
                    INSERT INTO folder_cards (folder_id, card_uuid, quantity, added_at)
                    SELECT
                        'wanted',
                        card_id,
                        quantity_wanted,
                        added_timestamp
                    FROM user_cards
                    WHERE quantity_wanted > 0
                """)
            }
        }

        fun getDatabase(context: Context): UserCardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UserCardDatabase::class.java,
                    "user_cards.db"
                )
                .createFromAsset("cards_initial.db")  // Pre-populate with bundled cards (3922 cards + default folders)
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()

                INSTANCE = instance
                instance
            }
        }
    }
}

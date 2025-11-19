package com.srg.inventory.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Database for user's card collection and wishlist
 */
@Database(entities = [UserCard::class], version = 1, exportSchema = false)
abstract class UserCardDatabase : RoomDatabase() {
    abstract fun userCardDao(): UserCardDao

    companion object {
        @Volatile
        private var INSTANCE: UserCardDatabase? = null

        fun getDatabase(context: Context): UserCardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UserCardDatabase::class.java,
                    "user_cards.db"
                )
                .fallbackToDestructiveMigration()
                .build()

                INSTANCE = instance
                instance
            }
        }
    }
}

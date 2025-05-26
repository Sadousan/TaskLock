package com.example.tasklock.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.tasklock.data.dao.BlockedAppsDao
import com.example.tasklock.data.model.AppUsageEntity
import com.example.tasklock.data.model.BlockedAppEntity

@Database(
    entities = [AppUsageEntity::class, BlockedAppEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppUsageDatabase : RoomDatabase() {

    abstract fun appUsageDao(): AppUsageDao
    abstract fun blockedAppsDao(): BlockedAppsDao

    companion object {
        @Volatile
        private var INSTANCE: AppUsageDatabase? = null

        fun getInstance(context: Context): AppUsageDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppUsageDatabase::class.java,
                    "app_usage_db"
                )
                    .fallbackToDestructiveMigration() // ✔️ (Futuramente podemos fazer migrations controladas)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

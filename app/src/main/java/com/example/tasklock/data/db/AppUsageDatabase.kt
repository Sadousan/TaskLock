package com.example.tasklock.data.db

import com.example.tasklock.data.db.AppUsageDao
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.tasklock.data.model.AppUsageEntity

@Database(entities = [AppUsageEntity::class], version = 1)
abstract class AppUsageDatabase : RoomDatabase() {
    abstract fun appUsageDao(): AppUsageDao

    companion object {
        @Volatile private var INSTANCE: AppUsageDatabase? = null

        fun getInstance(context: Context): AppUsageDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppUsageDatabase::class.java,
                    "app_usage_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
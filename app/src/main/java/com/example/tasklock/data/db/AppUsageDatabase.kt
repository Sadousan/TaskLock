package com.example.tasklock.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.tasklock.data.db.AppUsageDao
import com.example.tasklock.data.dao.BlockedAppsDao
import com.example.tasklock.data.dao.TarefaDao
import com.example.tasklock.data.model.AppUsageEntity
import com.example.tasklock.data.model.BlockedAppEntity
import com.example.tasklock.data.model.TarefaEntity

@Database(
    entities = [AppUsageEntity::class, BlockedAppEntity::class, TarefaEntity::class],
    version = 3, // <-- aumente a versão para refletir a nova entidade
    exportSchema = false
)
abstract class AppUsageDatabase : RoomDatabase() {

    abstract fun appUsageDao(): AppUsageDao
    abstract fun blockedAppsDao(): BlockedAppsDao
    abstract fun tarefaDao(): TarefaDao // <-- novo DAO

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
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

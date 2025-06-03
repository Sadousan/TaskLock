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
import com.example.tasklock.data.model.BonusDiarioEntity
import com.example.tasklock.data.model.TarefaEntity
import com.example.tasklock.data.model.UsuarioEntity
@Database(
    entities = [
        AppUsageEntity::class,
        BlockedAppEntity::class,
        TarefaEntity::class,
        BonusDiarioEntity::class,
        UsuarioEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppUsageDatabase : RoomDatabase() {
    abstract fun appUsageDao(): AppUsageDao
    abstract fun blockedAppsDao(): BlockedAppsDao
    abstract fun tarefaDao(): TarefaDao
    abstract fun bonusDiarioDao(): BonusDiarioDao
    abstract fun usuarioDao(): UsuarioDao

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

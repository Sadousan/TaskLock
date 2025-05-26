package com.example.tasklock.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_apps")
data class BlockedAppEntity(
    @PrimaryKey val packageName: String,
    val dailyLimitMs: Long,
    val usedTodayMs: Long = 0L
    // val bonusTimeMs: Long = 0L //  uso futuro com tarefas
)


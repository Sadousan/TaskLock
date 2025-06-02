package com.example.tasklock.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_apps")
data class BlockedAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val iconBase64: String?,
    val dailyLimitMs: Long, // <
    val usedTodayMs: Long,
    val bonusMs: Long = 0L
)



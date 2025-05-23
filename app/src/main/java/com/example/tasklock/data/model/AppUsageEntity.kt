package com.example.tasklock.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_usage")
data class AppUsageEntity(
    @PrimaryKey val packageName: String,
    val lastUsed: Long,
    val totalTimeMs: Long = 0L
)

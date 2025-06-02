package com.example.tasklock.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bonus_diario")
data class BonusDiarioEntity(
    @PrimaryKey val data: String,
    val usadoMs: Long
)

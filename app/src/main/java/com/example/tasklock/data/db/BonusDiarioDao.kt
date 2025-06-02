package com.example.tasklock.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.tasklock.data.model.BonusDiarioEntity

@Dao
interface BonusDiarioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirOuAtualizar(bonus: BonusDiarioEntity)

    @Query("SELECT * FROM bonus_diario WHERE data = :hoje LIMIT 1")
    suspend fun getBonusDoDia(hoje: String): BonusDiarioEntity?

    @Query("DELETE FROM bonus_diario")
    suspend fun limparTudo()
}


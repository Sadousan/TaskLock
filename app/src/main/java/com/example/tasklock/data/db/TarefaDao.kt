package com.example.tasklock.data.dao

import androidx.room.*
import com.example.tasklock.data.model.TarefaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TarefaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirTarefa(tarefa: TarefaEntity)

    @Update
    suspend fun atualizarTarefa(tarefa: TarefaEntity)

    @Delete
    suspend fun deletarTarefa(tarefa: TarefaEntity)

    @Query("SELECT * FROM tarefas ORDER BY id DESC")
    fun listarTarefas(): Flow<List<TarefaEntity>>

    @Query("SELECT * FROM tarefas WHERE concluida = 1")
    fun listarTarefasConcluidas(): Flow<List<TarefaEntity>>

    @Query("SELECT * FROM tarefas WHERE concluida = 0")
    fun listarTarefasPendentes(): Flow<List<TarefaEntity>>
}

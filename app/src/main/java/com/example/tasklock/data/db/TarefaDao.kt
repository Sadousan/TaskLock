package com.example.tasklock.data.dao

import androidx.room.*
import com.example.tasklock.data.model.TarefaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TarefaDao {

    @Query("SELECT * FROM tarefas WHERE emailUsuario = :emailUsuario ORDER BY id DESC")
    fun listarTarefasPorUsuario(emailUsuario: String): Flow<List<TarefaEntity>>

    @Query("SELECT * FROM tarefas WHERE emailUsuario = :emailUsuario AND concluida = 1")
    fun listarTarefasConcluidasPorUsuario(emailUsuario: String): Flow<List<TarefaEntity>>

    @Query("SELECT * FROM tarefas WHERE emailUsuario = :emailUsuario AND concluida = 0")
    fun listarTarefasPendentesPorUsuario(emailUsuario: String): Flow<List<TarefaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirTarefa(tarefa: TarefaEntity)

    @Update
    suspend fun atualizarTarefa(tarefa: TarefaEntity)

    @Delete
    suspend fun deletarTarefa(tarefa: TarefaEntity)

    @Query("UPDATE tarefas SET concluida = 0 WHERE recorrente = 1 AND concluida = 1 AND emailUsuario = :emailUsuario")
    fun resetarTarefasRecorrentesPorUsuario(emailUsuario: String)
}

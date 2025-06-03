    package com.example.tasklock.data.model

    import androidx.room.Entity
    import androidx.room.PrimaryKey

    @Entity(tableName = "tarefas")
    data class TarefaEntity(
        @PrimaryKey(autoGenerate = true) val id: Int = 0,
        val nome: String,
        val tipo: String,
        val prioridade: String,
        val data: String?, // null se for tarefa recorrente (diária)
        val recorrente: Boolean,
        val concluida: Boolean = false,
        val bonusMs: Long = 0L,
        val emailUsuario: String
    )


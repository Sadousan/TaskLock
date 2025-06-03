package com.example.tasklock.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.tasklock.data.model.UsuarioEntity

@Dao
interface UsuarioDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserir(usuario: UsuarioEntity)

    @Query("SELECT * FROM usuarios WHERE email = :email AND senha = :senha LIMIT 1")
    suspend fun autenticar(email: String, senha: String): UsuarioEntity?

    @Query("SELECT * FROM usuarios WHERE email = :email LIMIT 1")
    suspend fun buscarPorEmail(email: String): UsuarioEntity?

    @Update
    suspend fun atualizar(usuario: UsuarioEntity)
}

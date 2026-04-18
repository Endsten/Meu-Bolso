package com.example.gestorgastos.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirUsuario(usuario: Usuario)

    @Query("SELECT * FROM tabela_usuarios WHERE nome = :nome AND senha = :senha LIMIT 1")
    suspend fun login(nome: String, senha: String): Usuario?

    @Query("SELECT * FROM tabela_usuarios WHERE emailOuTelefone = :contato LIMIT 1")
    suspend fun buscarPorContato(contato: String): Usuario?

    @Query("SELECT COUNT(*) FROM tabela_usuarios")
    suspend fun contarUsuarios(): Int

    @Query("SELECT * FROM tabela_usuarios LIMIT 1")
    fun buscarPrimeiroUsuario(): Flow<Usuario?>

    @Query("SELECT * FROM tabela_usuarios WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Int): Usuario?
}

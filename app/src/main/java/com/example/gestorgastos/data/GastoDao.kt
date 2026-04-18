package com.example.gestorgastos.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GastoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirGasto(gasto: Gasto)

    @Update
    suspend fun atualizarGasto(gasto: Gasto)

    @Delete
    suspend fun deletarGasto(gasto: Gasto)

    @Query("SELECT * FROM tabela_gastos WHERE usuarioId = :usuarioId ORDER BY data DESC")
    fun buscarGastosPorUsuario(usuarioId: Int): Flow<List<Gasto>>

    @Query("SELECT * FROM tabela_gastos WHERE usuarioId = :usuarioId AND data BETWEEN :inicio AND :fim")
    fun buscarGastosPorPeriodo(usuarioId: Int, inicio: Long, fim: Long): Flow<List<Gasto>>

    @Query("SELECT SUM(valor) FROM tabela_gastos WHERE usuarioId = :usuarioId AND data BETWEEN :inicio AND :fim")
    fun buscarTotalDoPeriodo(usuarioId: Int, inicio: Long, fim: Long): Flow<Double?>

    @Query("SELECT categoria, SUM(valor) as total FROM tabela_gastos WHERE usuarioId = :usuarioId GROUP BY categoria")
    fun buscarGastosPorCategoria(usuarioId: Int): Flow<List<CategoriaTotal>>
}

data class CategoriaTotal(
    val categoria: String,
    val total: Double
)

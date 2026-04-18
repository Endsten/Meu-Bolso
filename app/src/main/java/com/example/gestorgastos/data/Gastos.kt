package com.example.gestorgastos.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabela_gastos")
data class Gasto(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val descricao: String,
    val valor: Double,
    val categoria: String,
    val metodoPagamento: String,
    val data: Long,
    val usuarioId: Int // Vínculo com o usuário que criou o gasto
)
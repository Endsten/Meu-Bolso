package com.example.gestorgastos.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabela_usuarios")
data class Usuario(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nome: String,
    val emailOuTelefone: String,
    val senha: String,
    val limiteSemanal: Double = 0.0
)

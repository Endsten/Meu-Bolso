package com.example.gestorgastos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestorgastos.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class GastoViewModel(
    private val gastoDao: GastoDao,
    private val usuarioDao: UsuarioDao,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _usuarioLogado = MutableStateFlow<Usuario?>(null)
    val usuarioLogado: StateFlow<Usuario?> = _usuarioLogado.asStateFlow()

    val temUsuarioCadastrado: StateFlow<Boolean> = usuarioDao.buscarPrimeiroUsuario()
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        loginAutomatico()
    }

    fun loginAutomatico(onSucesso: () -> Unit = {}) {
        viewModelScope.launch {
            sessionManager.userId.first()?.let { userId ->
                val usuario = usuarioDao.buscarPorId(userId)
                if (usuario != null) {
                    _usuarioLogado.value = usuario
                    onSucesso()
                }
            }
        }
    }

    val listaGastos: StateFlow<List<Gasto>> = _usuarioLogado
        .filterNotNull()
        .flatMapLatest { usuario ->
            gastoDao.buscarGastosPorUsuario(usuario.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalGastoSemanal: StateFlow<Double> = _usuarioLogado
        .filterNotNull()
        .flatMapLatest { usuario ->
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val inicioDaSemana = calendar.timeInMillis
            
            gastoDao.buscarTotalDoPeriodo(usuario.id, inicioDaSemana, System.currentTimeMillis())
        }
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun fazerLogin(nome: String, senha: String, lembrar: Boolean, onSucesso: () -> Unit, onErro: () -> Unit) {
        viewModelScope.launch {
            val usuario = usuarioDao.login(nome, senha)
            if (usuario != null) {
                if (lembrar) {
                    sessionManager.saveUserId(usuario.id)
                }
                _usuarioLogado.value = usuario
                onSucesso()
            } else {
                onErro()
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionManager.clearSession()
            _usuarioLogado.value = null
        }
    }

    fun atualizarUsuario(nome: String, contato: String, senha: String) {
        val usuario = _usuarioLogado.value ?: return
        viewModelScope.launch {
            val usuarioAtualizado = usuario.copy(nome = nome, emailOuTelefone = contato, senha = senha)
            usuarioDao.inserirUsuario(usuarioAtualizado)
            _usuarioLogado.value = usuarioAtualizado
        }
    }

    fun adicionarGasto(descricao: String, valor: Double, categoria: String, data: Long) {
        val usuarioId = _usuarioLogado.value?.id ?: return
        viewModelScope.launch {
            val novoGasto = Gasto(
                descricao = descricao,
                valor = valor,
                categoria = categoria,
                metodoPagamento = "Dinheiro",
                data = data,
                usuarioId = usuarioId
            )
            gastoDao.inserirGasto(novoGasto)
        }
    }

    fun atualizarGasto(gasto: Gasto) {
        viewModelScope.launch {
            gastoDao.atualizarGasto(gasto)
        }
    }

    fun deletarGasto(gasto: Gasto) {
        viewModelScope.launch {
            gastoDao.deletarGasto(gasto)
        }
    }

    fun cadastrarUsuario(nome: String, emailOuTelefone: String, senha: String, onSucesso: () -> Unit) {
        viewModelScope.launch {
            val novoUsuario = Usuario(
                nome = nome,
                emailOuTelefone = emailOuTelefone,
                senha = senha
            )
            usuarioDao.inserirUsuario(novoUsuario)
            _usuarioLogado.value = novoUsuario
            sessionManager.saveUserId(novoUsuario.id)
            onSucesso()
        }
    }
    
    fun atualizarLimiteSemanal(novoLimite: Double) {
        val usuario = _usuarioLogado.value ?: return
        viewModelScope.launch {
            val usuarioAtualizado = usuario.copy(limiteSemanal = novoLimite)
            usuarioDao.inserirUsuario(usuarioAtualizado)
            _usuarioLogado.value = usuarioAtualizado
        }
    }
}

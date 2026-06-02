package com.example.gestorgastos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gestorgastos.data.AppDatabase
import com.example.gestorgastos.data.Gasto
import com.example.gestorgastos.data.SessionManager
import com.example.gestorgastos.data.Usuario
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.getDatabase(this)
        val sessionManager = SessionManager(this)
        val viewModel = GastoViewModel(db.gastoDao(), db.usuarioDao(), sessionManager)

        setContent {
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val lightColors = lightColorScheme(
        primary = Color(0xFF1976D2),
        secondary = Color(0xFF00897B),
        background = Color(0xFFF8F9FA),
        surface = Color.White,
        onPrimary = Color.White
    )
    MaterialTheme(colorScheme = lightColors, content = content)
}

@Composable
fun MainApp(viewModel: GastoViewModel) {
    val usuarioLogado by viewModel.usuarioLogado.collectAsStateWithLifecycle()
    val temUsuario by viewModel.temUsuarioCadastrado.collectAsStateWithLifecycle()
    var telaAtual by remember { mutableStateOf("gastos") }

    if (usuarioLogado == null) {
        if (!temUsuario) TelaCadastroInicial(viewModel) else TelaLogin(viewModel)
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
                    NavigationBarItem(
                        selected = telaAtual == "gastos",
                        onClick = { telaAtual = "gastos" },
                        icon = { Icon(imageVector = Icons.Default.Add, contentDescription = "Lançar") },
                        label = { Text("Lançar") }
                    )
                    NavigationBarItem(
                        selected = telaAtual == "relatorios",
                        onClick = { telaAtual = "relatorios" },
                        icon = { Icon(imageVector = Icons.Default.Dashboard, contentDescription = "Relatórios") },
                        label = { Text("Dashboard") }
                    )
                    NavigationBarItem(
                        selected = telaAtual == "perfil",
                        onClick = { telaAtual = "perfil" },
                        icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Perfil") },
                        label = { Text("Perfil") }
                    )
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (telaAtual) {
                    "gastos" -> TelaGastos(viewModel)
                    "relatorios" -> TelaRelatorios(viewModel)
                    "perfil" -> TelaPerfil(viewModel)
                    else -> TelaGastos(viewModel)
                }
            }
        }
    }
}

@Composable
fun TelaLogin(viewModel: GastoViewModel) {
    var nome by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var lembrar by remember { mutableStateOf(true) }
    var erro by remember { mutableStateOf(false) }
    val context = LocalContext.current as FragmentActivity

    fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(context, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                Toast.makeText(context, "Acesso confirmado!", Toast.LENGTH_SHORT).show()
            }
        })
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Login Biométrico").setSubtitle("Use sua digital").setNegativeButtonText("Usar Senha").build()
        biometricPrompt.authenticate(promptInfo)
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Text("Meu Bolso", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Usuário") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = senha, onValueChange = { senha = it }, label = { Text("Senha") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = lembrar, onCheckedChange = { lembrar = it })
            Text("Permanecer logado", style = MaterialTheme.typography.bodySmall)
        }
        if (erro) Text("Dados incorretos!", color = Color.Red)
        Button(onClick = { viewModel.fazerLogin(nome, senha, lembrar, {}, { erro = true }) }, modifier = Modifier.padding(top = 16.dp).fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) { Text("Entrar") }
        IconButton(onClick = { showBiometricPrompt() }, modifier = Modifier.padding(top = 16.dp)) { Icon(imageVector = Icons.Default.Fingerprint, "Digital", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaGastos(viewModel: GastoViewModel) {
    val lista by viewModel.listaGastos.collectAsStateWithLifecycle()
    val usuario by viewModel.usuarioLogado.collectAsStateWithLifecycle()
    val totalSemanal by viewModel.totalGastoSemanal.collectAsStateWithLifecycle()
    var gastoSendoEditado by remember { mutableStateOf<Gasto?>(null) }
    var descricao by remember { mutableStateOf("") }
    var valor by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("Alimentação") }
    var dataSelecionada by remember { mutableStateOf(System.currentTimeMillis()) }
    var expandedCat by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    val categorias = listOf("Alimentação", "Transporte", "Moradia", "Saúde", "Lazer", "Vestuário", "Educação", "Investimentos", "Outros")

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dataSelecionada)
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = { TextButton(onClick = { dataSelecionada = datePickerState.selectedDateMillis ?: System.currentTimeMillis(); showDatePicker = false }) { Text("OK") } }) { DatePicker(state = datePickerState) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Olá, ${usuario?.nome}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (usuario?.limiteSemanal != null && usuario!!.limiteSemanal > 0) {
            val progresso = (totalSemanal / usuario!!.limiteSemanal).coerceIn(0.0, 1.0)
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (progresso >= 0.9) Color(0xFFFFEBEE) else Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Uso do Salário", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("R$ ${String.format("%.2f", totalSemanal)}", fontWeight = FontWeight.Bold, fontSize = 20.sp); Text("Salário: R$ ${String.format("%.2f", usuario!!.limiteSemanal)}", color = Color.Gray) }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { progresso.toFloat() }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = if (progresso >= 0.9) Color.Red else MaterialTheme.colorScheme.primary)
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(value = descricao, onValueChange = { descricao = it }, label = { Text("O que você comprou?") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = valor, onValueChange = { valor = it }, label = { Text("Valor R$") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(value = categoria, onValueChange = {}, readOnly = true, label = { Text("Categoria") }, modifier = Modifier.fillMaxWidth().clickable { expandedCat = true }, shape = RoundedCornerShape(12.dp), trailingIcon = { Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null) }, enabled = false)
                        DropdownMenu(expanded = expandedCat, onDismissRequest = { expandedCat = false }) { categorias.forEach { cat -> DropdownMenuItem(text = { Text(cat) }, onClick = { categoria = cat; expandedCat = false }) } }
                    }
                }
                OutlinedTextField(value = sdf.format(Date(dataSelecionada)), onValueChange = {}, readOnly = true, label = { Text("Data") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable { showDatePicker = true }, shape = RoundedCornerShape(12.dp), trailingIcon = { Icon(imageVector = Icons.Default.DateRange, contentDescription = null) }, enabled = false)
                Button(onClick = {
                    val v = valor.replace(",", ".").toDoubleOrNull()
                    if (descricao.isNotEmpty() && v != null) {
                        if (gastoSendoEditado != null) viewModel.atualizarGasto(gastoSendoEditado!!.copy(descricao = descricao, valor = v, categoria = categoria, data = dataSelecionada))
                        else viewModel.adicionarGasto(descricao, v, categoria, dataSelecionada)
                        descricao = ""; valor = ""; categoria = "Alimentação"; gastoSendoEditado = null
                    }
                }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp), shape = RoundedCornerShape(12.dp)) { Text(if (gastoSendoEditado != null) "Atualizar" else "Salvar Gasto") }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Últimos Lançamentos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(lista.take(15)) { gasto ->
                ListItem(
                    headlineContent = { Text(gasto.descricao, fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text("${gasto.categoria} • ${sdf.format(Date(gasto.data))}") },
                    trailingContent = { Row(verticalAlignment = Alignment.CenterVertically) { Text("R$ ${String.format("%.2f", gasto.valor)}", color = Color.Red, fontWeight = FontWeight.Bold); IconButton(onClick = { viewModel.deletarGasto(gasto) }) { Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color.LightGray) } } },
                    modifier = Modifier.clickable { gastoSendoEditado = gasto; descricao = gasto.descricao; valor = gasto.valor.toString(); categoria = gasto.categoria; dataSelecionada = gasto.data }
                )
                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaRelatorios(viewModel: GastoViewModel) {
    val lista by viewModel.listaGastos.collectAsStateWithLifecycle()
    val usuario by viewModel.usuarioLogado.collectAsStateWithLifecycle()
    var filtro by remember { mutableStateOf("Mês") }
    var categoriaSelecionada by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val listaFiltrada = when (filtro) {
        "Semana" -> { val cal = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, firstDayOfWeek) }; lista.filter { it.data >= cal.timeInMillis } }
        "Mês" -> { val cal = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }; lista.filter { it.data >= cal.timeInMillis } }
        else -> lista
    }
    
    val totalGastos = listaFiltrada.sumOf { it.valor }
    val salario = usuario?.limiteSemanal ?: 0.0
    val saldo = (salario - totalGastos).coerceAtLeast(0.0)
    val porCat = listaFiltrada.groupBy { it.categoria }.mapValues { it.value.sumOf { g -> g.valor } }
    val cores = listOf(Color(0xFF1976D2), Color(0xFF388E3C), Color(0xFFFBC02D), Color(0xFFD32F2F), Color(0xFF7B1FA2), Color(0xFF0097A7), Color(0xFFE64A19), Color(0xFF5D4037))

    fun exportarParaExcel() {
        val csv = "Data,Descricao,Categoria,Valor\n" + listaFiltrada.joinToString("\n") { "${sdf.format(Date(it.data))},${it.descricao},${it.categoria},${String.format("%.2f", it.valor)}" }
        try {
            val file = File(context.cacheDir, "relatorio_${System.currentTimeMillis()}.csv").apply { writeText(csv) }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/csv"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Exportar"))
        } catch (e: Exception) { Toast.makeText(context, "Erro ao exportar!", Toast.LENGTH_SHORT).show() }
    }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Dashboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            IconButton(onClick = { exportarParaExcel() }) { Icon(imageVector = Icons.Default.Share, contentDescription = "Exportar", tint = MaterialTheme.colorScheme.primary) }
        }
        
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Semana", "Mês", "Tudo").forEach { f -> FilterChip(selected = filtro == f, onClick = { filtro = f; categoriaSelecionada = null }, label = { Text(f) }) }
        }

        if (categoriaSelecionada == null) {
            // DASHBOARD CARDS
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF2E7D32))
                        Text("Saldo", style = MaterialTheme.typography.labelMedium, color = Color(0xFF2E7D32))
                        Text("R$ ${String.format("%.2f", saldo)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }
                }
                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.TrendingDown, contentDescription = null, tint = Color(0xFFC62828))
                        Text("Gastos", style = MaterialTheme.typography.labelMedium, color = Color(0xFFC62828))
                        Text("R$ ${String.format("%.2f", totalGastos)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Renda Configurada", style = MaterialTheme.typography.labelMedium)
                        Text("R$ ${String.format("%.2f", salario)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (totalGastos > 0) {
                Text("Gastos por Categoria", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(porCat.toList().sortedByDescending { it.second }) { (cat, valor) ->
                        val percent = (valor / totalGastos).toFloat()
                        val cor = cores[porCat.keys.toList().indexOf(cat) % cores.size]
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { categoriaSelecionada = cat }) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(cat, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("R$ ${String.format("%.2f", valor)} (${String.format("%.0f", percent * 100)}%)", fontWeight = FontWeight.Bold, color = cor)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(progress = { percent }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = cor, trackColor = Color(0xFFEEEEEE))
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) { Text("Nenhum gasto registrado", color = Color.Gray) }
            }
        } else {
            // TABELA DETALHADA
            Column(modifier = Modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { categoriaSelecionada = null }.padding(bottom = 16.dp)) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Voltar", tint = MaterialTheme.colorScheme.primary)
                    Text(" Voltar", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Text("Detalhes: $categoriaSelecionada", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(8.dp)) {
                    Text("Data", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("Descrição", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("Valor", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
                }
                val gastosCategoria = listaFiltrada.filter { it.categoria == categoriaSelecionada }.sortedByDescending { it.data }
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(gastosCategoria) { gasto ->
                        Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                            Text(sdf.format(Date(gasto.data)), modifier = Modifier.weight(1f), fontSize = 12.sp)
                            Text(gasto.descricao, modifier = Modifier.weight(2f), fontSize = 12.sp)
                            Text("R$ ${String.format("%.2f", gasto.valor)}", modifier = Modifier.weight(1f), fontSize = 12.sp, textAlign = TextAlign.End, fontWeight = FontWeight.Bold, color = Color.Red)
                        }
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun TelaPerfil(viewModel: GastoViewModel) {
    val usuario by viewModel.usuarioLogado.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var nome by remember { mutableStateOf(usuario?.nome ?: "") }
    var contato by remember { mutableStateOf(usuario?.emailOuTelefone ?: "") }
    var senha by remember { mutableStateOf(usuario?.senha ?: "") }
    var limite by remember { mutableStateOf(usuario?.limiteSemanal?.toString() ?: "0.0") }
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Configurações do Perfil", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = contato, onValueChange = { contato = it }, label = { Text("Contato") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = senha, onValueChange = { senha = it }, label = { Text("Senha") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = limite, onValueChange = { limite = it }, label = { Text("Salário Mensal R$") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(12.dp))
                Button(onClick = { viewModel.atualizarUsuario(nome, contato, senha); viewModel.atualizarLimiteSemanal(limite.replace(",", ".").toDoubleOrNull() ?: 0.0); Toast.makeText(context, "Salvo!", Toast.LENGTH_SHORT).show() }, modifier = Modifier.padding(top = 24.dp).fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) { Text("Salvar Alterações") }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = { viewModel.logout() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE), contentColor = Color.Red), shape = RoundedCornerShape(12.dp)) { Icon(imageVector = Icons.Default.Logout, contentDescription = "Sair") ; Spacer(modifier = Modifier.width(8.dp)) ; Text("Sair") }
    }
}

@Composable
fun TelaCadastroInicial(viewModel: GastoViewModel) {
    var nome by remember { mutableStateOf("") }
    var contato by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.primary)
        Text("Bem-vindo!", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        OutlinedTextField(value = contato, onValueChange = { contato = it }, label = { Text("Contato") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(12.dp))
        OutlinedTextField(value = senha, onValueChange = { senha = it }, label = { Text("Senha") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(12.dp))
        Button(onClick = { if (nome.isNotEmpty() && contato.isNotEmpty() && senha.isNotEmpty()) viewModel.cadastrarUsuario(nome, contato, senha, {}) }, modifier = Modifier.padding(top = 32.dp).fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) { Text("Criar Conta") }
    }
}

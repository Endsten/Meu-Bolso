package com.example.gestorgastos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import kotlinx.coroutines.launch
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
                        icon = { Icon(Icons.Default.Add, "Lançar") },
                        label = { Text("Lançar") }
                    )
                    NavigationBarItem(
                        selected = telaAtual == "relatorios",
                        onClick = { telaAtual = "relatorios" },
                        icon = { Icon(Icons.Default.List, "Relatórios") },
                        label = { Text("Relatórios") }
                    )
                    NavigationBarItem(
                        selected = telaAtual == "perfil",
                        onClick = { telaAtual = "perfil" },
                        icon = { Icon(Icons.Default.Person, "Perfil") },
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
    var mostrarRecuperacao by remember { mutableStateOf(false) }
    val context = LocalContext.current as FragmentActivity

    fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(context, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                Toast.makeText(context, "Bem-vindo de volta!", Toast.LENGTH_SHORT).show()
            }
        })
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Login Biométrico").setSubtitle("Use sua digital para entrar").setNegativeButtonText("Senha").build()
        biometricPrompt.authenticate(promptInfo)
    }

    if (mostrarRecuperacao) {
        TelaRecuperarSenha(viewModel, onVoltar = { mostrarRecuperacao = false })
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Home, null, Modifier.size(80.dp), MaterialTheme.colorScheme.primary)
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
            IconButton(onClick = { showBiometricPrompt() }, modifier = Modifier.padding(top = 16.dp)) { Icon(Icons.Default.Lock, "Digital", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary) }
            TextButton(onClick = { mostrarRecuperacao = true }) { Text("Esqueci minha senha") }
        }
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
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (progresso >= 1.0) Color(0xFFFFEBEE) else Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Gasto Semanal", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("R$ ${String.format("%.2f", totalSemanal)}", fontWeight = FontWeight.Bold, fontSize = 20.sp); Text("Meta: R$ ${String.format("%.2f", usuario!!.limiteSemanal)}", color = Color.Gray) }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { progresso.toFloat() }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = if (progresso >= 1.0) Color.Red else MaterialTheme.colorScheme.primary)
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(value = descricao, onValueChange = { descricao = it }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = valor, onValueChange = { valor = it }, label = { Text("Valor R$") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(value = categoria, onValueChange = {}, readOnly = true, label = { Text("Categoria") }, modifier = Modifier.fillMaxWidth().clickable { expandedCat = true }, shape = RoundedCornerShape(12.dp), trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }, enabled = false)
                        DropdownMenu(expanded = expandedCat, onDismissRequest = { expandedCat = false }) { categorias.forEach { cat -> DropdownMenuItem(text = { Text(cat) }, onClick = { categoria = cat; expandedCat = false }) } }
                    }
                }
                OutlinedTextField(value = sdf.format(Date(dataSelecionada)), onValueChange = {}, readOnly = true, label = { Text("Data") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable { showDatePicker = true }, shape = RoundedCornerShape(12.dp), trailingIcon = { Icon(Icons.Default.DateRange, null) }, enabled = false)
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
                    trailingContent = { Row(verticalAlignment = Alignment.CenterVertically) { Text("R$ ${String.format("%.2f", gasto.valor)}", color = Color.Red, fontWeight = FontWeight.Bold); IconButton(onClick = { viewModel.deletarGasto(gasto) }) { Icon(Icons.Default.Delete, null, tint = Color.LightGray) } } },
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
    var filtro by remember { mutableStateOf("Semana") }
    val context = LocalContext.current
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val listaFiltrada = when (filtro) {
        "Semana" -> { val cal = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, firstDayOfWeek) }; lista.filter { it.data >= cal.timeInMillis } }
        "Mês" -> { val cal = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }; lista.filter { it.data >= cal.timeInMillis } }
        else -> lista
    }
    val total = listaFiltrada.sumOf { it.valor }
    val porCat = listaFiltrada.groupBy { it.categoria }.mapValues { it.value.sumOf { g -> g.valor } }

    fun exportarParaExcel() {
        val cabecalho = "Data,Descricao,Categoria,Valor\n"
        val corpo = listaFiltrada.joinToString("\n") { gasto ->
            "${sdf.format(Date(gasto.data))},${gasto.descricao},${gasto.categoria},${String.format("%.2f", gasto.valor)}"
        }
        val csv = cabecalho + corpo
        try {
            val fileName = "relatorio_meu_bolso_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            file.writeText(csv)
            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Relatório - Meu Bolso")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Exportar Relatório"))
        } catch (e: Exception) {
            Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Relatórios", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            IconButton(onClick = { exportarParaExcel() }) { Icon(Icons.Default.Share, "Exportar", tint = MaterialTheme.colorScheme.primary) }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Semana", "Mês", "Tudo").forEach { f -> FilterChip(selected = filtro == f, onClick = { filtro = f }, label = { Text(f) }) }
        }
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total no Período", color = Color.White.copy(alpha = 0.8f))
                Text("R$ ${String.format("%.2f", total)}", style = MaterialTheme.typography.displaySmall, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        if (total > 0) {
            Text(if (filtro == "Semana") "Divisão por Categorias" else "Evolução Diária", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                if (filtro == "Semana") GraficoPizza(porCat, total) else GraficoBarras(listaFiltrada)
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(porCat.toList().sortedByDescending { it.second }) { (cat, valor) ->
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(cat, fontWeight = FontWeight.Medium)
                            Text("R$ ${String.format("%.2f", valor)}", fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(progress = { (valor / total).toFloat() }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape))
                    }
                }
            }
        }
    }
}

@Composable
fun GraficoPizza(dados: Map<String, Double>, total: Double) {
    val cores = listOf(Color(0xFF1976D2), Color(0xFF388E3C), Color(0xFFFBC02D), Color(0xFFD32F2F), Color(0xFF7B1FA2), Color(0xFF0097A7), Color(0xFFE64A19), Color(0xFF5D4037))
    Canvas(modifier = Modifier.size(140.dp)) {
        var startAngle = 0f
        dados.values.forEachIndexed { index, valor ->
            val sweepAngle = (valor / total * 360).toFloat()
            drawArc(color = cores[index % cores.size], startAngle = startAngle, sweepAngle = sweepAngle, useCenter = true)
            startAngle += sweepAngle
        }
    }
}

@Composable
fun GraficoBarras(lista: List<Gasto>) {
    val gastosPorDia = mutableMapOf<Int, Double>()
    for (i in 0..6) {
        val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
        gastosPorDia[c.get(Calendar.DAY_OF_YEAR)] = 0.0
    }
    lista.forEach { g ->
        val c = Calendar.getInstance().apply { timeInMillis = g.data }
        val d = c.get(Calendar.DAY_OF_YEAR)
        if (gastosPorDia.containsKey(d)) gastosPorDia[d] = gastosPorDia[d]!! + g.valor
    }
    val maxGasto = (gastosPorDia.values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
        gastosPorDia.keys.sorted().forEach { dia ->
            val altura = (gastosPorDia[dia]!! / maxGasto * 120).dp
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.width(18.dp).height(altura).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                Text(SimpleDateFormat("dd", Locale.getDefault()).format(Calendar.getInstance().apply { set(Calendar.DAY_OF_YEAR, dia) }.time), fontSize = 9.sp)
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
        Text("Perfil e Configurações", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = contato, onValueChange = { contato = it }, label = { Text("Contato") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = senha, onValueChange = { senha = it }, label = { Text("Senha") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = limite, onValueChange = { limite = it }, label = { Text("Meta R$") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(12.dp))
                Button(onClick = { viewModel.atualizarUsuario(nome, contato, senha); viewModel.atualizarLimiteSemanal(limite.replace(",", ".").toDoubleOrNull() ?: 0.0); Toast.makeText(context, "Salvo!", Toast.LENGTH_SHORT).show() }, modifier = Modifier.padding(top = 24.dp).fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) { Text("Salvar Alterações") }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = { viewModel.logout() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE), contentColor = Color.Red), shape = RoundedCornerShape(12.dp)) { Icon(Icons.Default.ExitToApp, null); Spacer(modifier = Modifier.width(8.dp)); Text("Sair") }
    }
}

@Composable
fun TelaCadastroInicial(viewModel: GastoViewModel) {
    var nome by remember { mutableStateOf("") }
    var contato by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Home, null, Modifier.size(60.dp), MaterialTheme.colorScheme.primary)
        Text("Bem-vindo!", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        OutlinedTextField(value = contato, onValueChange = { contato = it }, label = { Text("Contato") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(12.dp))
        OutlinedTextField(value = senha, onValueChange = { senha = it }, label = { Text("Senha") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(12.dp))
        Button(onClick = { if (nome.isNotEmpty() && contato.isNotEmpty() && senha.isNotEmpty()) viewModel.cadastrarUsuario(nome, contato, senha, {}) }, modifier = Modifier.padding(top = 32.dp).fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) { Text("Criar Conta") }
    }
}

@Composable
fun TelaRecuperarSenha(viewModel: GastoViewModel, onVoltar: () -> Unit) {
    var etapa by remember { mutableStateOf(1) }
    var contatoBusca by remember { mutableStateOf("") }
    var usuarioEncontrado by remember { mutableStateOf<Usuario?>(null) }
    var codigoGerado by remember { mutableStateOf("") }
    var codigoDigitado by remember { mutableStateOf("") }
    var novaSenha by remember { mutableStateOf("") }
    var erroMsg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Recuperar", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        when (etapa) {
            1 -> {
                OutlinedTextField(value = contatoBusca, onValueChange = { contatoBusca = it }, label = { Text("Contato") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Button(onClick = { scope.launch { val user = viewModel.buscarUsuarioPorContato(contatoBusca)
                    if (user != null) { usuarioEncontrado = user; codigoGerado = (1000..9999).random().toString()
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$contatoBusca")).apply { putExtra(Intent.EXTRA_SUBJECT, "Recuperação"); putExtra(Intent.EXTRA_TEXT, "Código: $codigoGerado") }
                        try { context.startActivity(intent); etapa = 2 } catch (e: Exception) { erroMsg = "Código: $codigoGerado"; etapa = 2 } } else erroMsg = "Não encontrado" } }, modifier = Modifier.padding(top = 8.dp).fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Próximo") }
            }
            2 -> {
                OutlinedTextField(value = codigoDigitado, onValueChange = { codigoDigitado = it }, label = { Text("Código") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Button(onClick = { if (codigoDigitado == codigoGerado) etapa = 3 else erroMsg = "Incorreto" }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(12.dp)) { Text("Verificar") }
            }
            3 -> {
                OutlinedTextField(value = novaSenha, onValueChange = { novaSenha = it }, label = { Text("Nova Senha") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Button(onClick = { if (novaSenha.length >= 3) { viewModel.resetarSenha(usuarioEncontrado!!, novaSenha); onVoltar() } }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(12.dp)) { Text("Redefinir") }
            }
        }
        TextButton(onClick = onVoltar) { Text("Cancelar") }
    }
}
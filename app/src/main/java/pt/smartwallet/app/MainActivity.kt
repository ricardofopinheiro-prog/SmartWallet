package pt.smartwallet.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import pt.smartwallet.app.data.SmartWalletDatabase
import pt.smartwallet.app.data.Transaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: WalletViewModel by viewModels {
        val dao = SmartWalletDatabase.get(applicationContext).transactionDao()
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                WalletViewModel(dao) as T
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SmartWalletApp(viewModel) }
    }
}

class WalletViewModel(
    private val dao: pt.smartwallet.app.data.TransactionDao
) : ViewModel() {
    val transactions = dao.observeAll().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    fun add(description: String, euros: String, type: String, category: String) {
        val normalized = euros.trim().replace(",", ".")
        val value = normalized.toBigDecimalOrNull() ?: return
        if (value <= java.math.BigDecimal.ZERO) return
        val cents = value.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact()
        viewModelScope.launch {
            dao.insert(Transaction(
                description = description.ifBlank { if (type == "INCOME") "Rendimento" else "Despesa" },
                amountCents = cents,
                type = type,
                category = category
            ))
        }
    }

    fun delete(id: Long) { viewModelScope.launch { dao.deleteById(id) } }
}

private val ptLocale = Locale("pt", "PT")
private fun money(cents: Long): String =
    NumberFormat.getCurrencyInstance(ptLocale).format(cents / 100.0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartWalletApp(vm: WalletViewModel) {
    val transactions by vm.transactions.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    val income = transactions.filter { it.type == "INCOME" }.sumOf { it.amountCents }
    val expenses = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amountCents }
    val balance = income - expenses

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF176B57),
            secondary = androidx.compose.ui.graphics.Color(0xFF75A995),
            background = androidx.compose.ui.graphics.Color(0xFFF5F7F6),
            surface = androidx.compose.ui.graphics.Color.White
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("SmartWallet", fontWeight = FontWeight.Bold)
                            Text("A tua carteira financeira", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAdd = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar movimento")
                }
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.AccountBalanceWallet, null) },
                        label = { Text("Resumo") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.History, null) },
                        label = { Text("Movimentos") }
                    )
                }
            }
        ) { padding ->
            if (selectedTab == 0) {
                Dashboard(
                    modifier = Modifier.padding(padding),
                    balance = balance,
                    income = income,
                    expenses = expenses,
                    transactions = transactions
                )
            } else {
                MovementList(
                    modifier = Modifier.padding(padding),
                    transactions = transactions,
                    onDelete = vm::delete
                )
            }
        }

        if (showAdd) {
            AddMovementDialog(
                onDismiss = { showAdd = false },
                onSave = { description, amount, type, category ->
                    vm.add(description, amount, type, category)
                    showAdd = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dashboard(
    modifier: Modifier,
    balance: Long,
    income: Long,
    expenses: Long,
    transactions: List<Transaction>
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(22.dp)) {
                    Text("Saldo registado", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .8f))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        money(balance),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Com base nos movimentos que adicionaste", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .8f))
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("Rendimentos", money(income), Modifier.weight(1f), MaterialTheme.colorScheme.primary)
                SummaryCard("Despesas", money(expenses), Modifier.weight(1f), MaterialTheme.colorScheme.error)
            }
        }
        item {
            Text("Movimentos recentes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        if (transactions.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Ainda não tens movimentos.")
                        Text("Carrega no botão + para registar o primeiro rendimento ou despesa.",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            items(transactions.take(5), key = { it.id }) { tx ->
                TransactionRow(tx)
            }
        }
    }
}

@Composable
fun SummaryCard(title: String, value: String, modifier: Modifier, color: androidx.compose.ui.graphics.Color) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Text(value, fontWeight = FontWeight.Bold, color = color, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun MovementList(modifier: Modifier, transactions: List<Transaction>, onDelete: (Long) -> Unit) {
    if (transactions.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Ainda não existem movimentos.")
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(transactions, key = { it.id }) { tx ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(tx.description, fontWeight = FontWeight.SemiBold)
                            Text("${tx.category} • ${SimpleDateFormat("dd/MM/yyyy", ptLocale).format(Date(tx.dateMillis))}",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            (if (tx.type == "INCOME") "+" else "−") + money(tx.amountCents),
                            color = if (tx.type == "INCOME") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { onDelete(tx.id) }) { Text("Apagar") }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionRow(tx: Transaction) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(tx.description, fontWeight = FontWeight.SemiBold)
                Text(tx.category, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                (if (tx.type == "INCOME") "+" else "−") + money(tx.amountCents),
                color = if (tx.type == "INCOME") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMovementDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("EXPENSE") }
    var category by remember { mutableStateOf("Outros") }
    val categories = if (type == "INCOME")
        listOf("Salário", "Subsídios", "Trabalho adicional", "Investimentos", "Outros")
    else
        listOf("Alimentação", "Supermercado", "Habitação", "Transportes", "Automóvel", "Saúde", "Lazer", "Créditos", "Outros")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo movimento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == "EXPENSE", onClick = { type = "EXPENSE"; category = "Outros" }, label = { Text("Despesa") })
                    FilterChip(selected = type == "INCOME", onClick = { type = "INCOME"; category = "Outros" }, label = { Text("Rendimento") })
                }
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Descrição") }, singleLine = true
                )
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it },
                    label = { Text("Valor (€)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = category, onValueChange = {}, readOnly = true,
                        label = { Text("Categoria") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categories.forEach { item ->
                            DropdownMenuItem(text = { Text(item) }, onClick = { category = item; expanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = amount.replace(",", ".").toBigDecimalOrNull()?.let { it > java.math.BigDecimal.ZERO } == true,
                onClick = { onSave(description, amount, type, category) }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

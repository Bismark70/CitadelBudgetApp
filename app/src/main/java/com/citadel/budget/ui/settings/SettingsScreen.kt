package com.citadel.budget.ui.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.citadel.budget.R
import com.citadel.budget.AppContainer
import com.citadel.budget.data.SettingsStore
import com.citadel.budget.data.ThemeMode
import com.citadel.budget.data.UserSettings
import com.citadel.budget.ui.components.ConfirmDialog
import com.citadel.budget.ui.components.SectionTitle
import com.citadel.budget.util.Fmt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

class SettingsViewModel(private val c: AppContainer) : ViewModel() {

    val settings = c.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    fun setName(name: String) = viewModelScope.launch { c.settings.setName(name) }
    fun setCurrency(code: String) = viewModelScope.launch { c.settings.setCurrency(code) }
    fun setTheme(mode: ThemeMode) = viewModelScope.launch { c.settings.setTheme(mode) }
    fun setPin(pin: String) = viewModelScope.launch { c.settings.setPin(pin) }
    fun clearPin() = viewModelScope.launch { c.settings.clearPin() }

    fun resetMonth() = viewModelScope.launch {
        c.db.transactionDao().deleteMonth(Fmt.monthKeyOf())
    }

    fun eraseEverything(onDone: () -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        c.db.clearAllTables()
        c.settings.eraseAll()
        withContext(Dispatchers.Main) { onDone() }
    }

    fun exportCsv(context: Context, onMessage: (String) -> Unit) =
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val txs = c.db.transactionDao().observeRecent(1_000_000).first()
                val cats = c.db.categoryDao().observeAll().first().associateBy { it.id }
                val df = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val sb = StringBuilder("date,type,category,bucket,amount,note\n")
                txs.forEach { t ->
                    val cat = cats[t.tx.categoryId]
                    val date = LocalDate.ofEpochDay(t.tx.epochDay).format(df)
                    val amt = String.format(Locale.US, "%.2f", t.tx.amountCents / 100.0)
                    val note = t.tx.note.replace("\"", "\"\"")
                    sb.append("$date,${t.tx.type},${cat?.name ?: "Uncategorized"},${cat?.bucket?.name ?: "NEEDS"},$amt,\"$note\"\n")
                }
                val dir = context.getExternalFilesDir(null) ?: context.filesDir
                val file = File(dir, "CitadelBudget_Export_${LocalDate.now()}.csv")
                file.writeText(sb.toString())

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(send, "Share Citadel export"))
                "Saved to ${file.absolutePath}"
            }.onSuccess { onMessage(it) }
                .onFailure { onMessage("Export failed: ${it.message}") }
        }

    companion object {
        fun factory(c: AppContainer) = viewModelFactory { initializer { SettingsViewModel(c) } }
    }
}

private val currencyList = listOf(
    "USD", "EUR", "GBP", "CAD", "AUD", "JPY", "CNY",
    "INR", "NGN", "KES", "ZAR", "PHP", "BRL", "MXN", "IDR"
)

private data class BookRef(val title: String, val author: String, val took: String)

private val books = listOf(
    BookRef(
        "The Total Money Makeover", "Dave Ramsey",
        "The Baby Steps build order — your Steps tab."
    ),
    BookRef(
        "Get Good with Money", "Tiffany Aliche",
        "The Survival Number and money-leak alerts."
    ),
    BookRef(
        "I Will Teach You to Be Rich", "Ramit Sethi",
        "Conscious spending — custom splits, guilt-free wants."
    ),
    BookRef(
        "The Psychology of Money", "Morgan Housel",
        "The savings gap that powers your Citadel Score."
    ),
    BookRef(
        "Broke Millennial", "Erin Lowry",
        "Zero money-shame. Every empty state is kind."
    )
)

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(container: AppContainer, onWiped: () -> Unit, onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
    val s by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showCurrency by remember { mutableStateOf(false) }
    var showPinSet by remember { mutableStateOf(false) }
    var showPinVerify by remember { mutableStateOf(false) }
    var showNameEdit by remember { mutableStateOf(false) }
    var showResetMonth by remember { mutableStateOf(false) }
    var showWipe by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Profile
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            s.userName.ifBlank { "C" }.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(Modifier.size(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            s.userName.ifBlank { "Commander" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Keeper of the fortress",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { showNameEdit = true }) { Text("Rename") }
                }
            }

            // Preferences
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle("Preferences")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Currency", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                "${s.currency} · ${Fmt.money(123456, s.currency)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { showCurrency = true }) { Text("Change") }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(12.dp))
                        Text(
                            "Theme",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            ThemeMode.SYSTEM to "System",
                            ThemeMode.LIGHT to "Light",
                            ThemeMode.DARK to "Dark"
                        ).forEach { (mode, label) ->
                            FilterChip(
                                selected = s.themeMode == mode,
                                onClick = { vm.setTheme(mode) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            // Security — Citadel Gate
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Citadel Gate", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            if (s.pinEnabled) "PIN lock is ON — the gate guards your data."
                            else "Require a 4-digit PIN when opening the app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = s.pinEnabled,
                        onCheckedChange = { want ->
                            if (want) showPinSet = true else showPinVerify = true
                        }
                    )
                }
            }

            // Data
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Your data")
                    Text(
                        "Citadel is 100% offline. No internet permission exists in this app — your numbers physically cannot leave this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.size(4.dp))
                    Button(
                        onClick = { vm.exportCsv(context) { msg -> scope.launch { snackbar.showSnackbar(msg) } } },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.FileDownload, contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Export as CSV")
                    }
                    TextButton(onClick = { showResetMonth = true }) {
                        Text("Reset this month's transactions", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { showWipe = true }) {
                        Text("Erase everything", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // The Money Library
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp)) {
                    SectionTitle("The Money Library")
                    Spacer(Modifier.size(4.dp))
                    Text(
                        "Five bestsellers shaped this app:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.size(8.dp))
                    books.forEach { book ->
                        Row(Modifier.padding(vertical = 6.dp)) {
                            Text("📕", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.size(10.dp))
                            Column {
                                Text(
                                    "${book.title} — ${book.author}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    book.took,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // About
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(R.drawable.logo),
                        contentDescription = "Citadel Budget logo",
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("Citadel Budget", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Version 1.0.0 · 100% offline · build your money fortress",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    // Dialogs
    if (showCurrency) {
        AlertDialog(
            onDismissRequest = { showCurrency = false },
            title = { Text("Currency") },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    currencyList.forEach { code ->
                        TextButton(
                            onClick = {
                                vm.setCurrency(code)
                                showCurrency = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "$code · ${Fmt.money(123456, code)}",
                                color = if (code == s.currency) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (code == s.currency) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCurrency = false }) { Text("Cancel") }
            }
        )
    }

    if (showNameEdit) {
        var nameText by remember { mutableStateOf(s.userName) }
        AlertDialog(
            onDismissRequest = { showNameEdit = false },
            title = { Text("Your name") },
            text = {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it.take(30) },
                    singleLine = true,
                    label = { Text("Name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setName(nameText)
                    showNameEdit = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showNameEdit = false }) { Text("Cancel") }
            }
        )
    }

    if (showPinSet) {
        var pin1 by remember { mutableStateOf("") }
        var pin2 by remember { mutableStateOf("") }
        val ok = pin1.length == 4 && pin1 == pin2 && pin1.all { it.isDigit() }
        AlertDialog(
            onDismissRequest = { showPinSet = false },
            title = { Text("Set your PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = pin1,
                        onValueChange = { pin1 = it.filter { ch -> ch.isDigit() }.take(4) },
                        label = { Text("4-digit PIN") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = pin2,
                        onValueChange = { pin2 = it.filter { ch -> ch.isDigit() }.take(4) },
                        label = { Text("Repeat PIN") },
                        singleLine = true
                    )
                    if (pin2.isNotEmpty() && !ok) {
                        Text(
                            "PINs must match and be 4 digits.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = ok,
                    onClick = {
                        vm.setPin(pin1)
                        showPinSet = false
                    }
                ) { Text("Enable gate") }
            },
            dismissButton = {
                TextButton(onClick = { showPinSet = false }) { Text("Cancel") }
            }
        )
    }

    if (showPinVerify) {
        var pin by remember { mutableStateOf("") }
        val ok = s.pinSalt.isNotEmpty() && SettingsStore.hash(s.pinSalt, pin) == s.pinHash
        AlertDialog(
            onDismissRequest = { showPinVerify = false },
            title = { Text("Disable Citadel Gate") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter your current PIN to lower the gate.")
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(4) },
                        label = { Text("Current PIN") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = ok,
                    onClick = {
                        vm.clearPin()
                        showPinVerify = false
                    }
                ) { Text("Disable") }
            },
            dismissButton = {
                TextButton(onClick = { showPinVerify = false }) { Text("Cancel") }
            }
        )
    }

    if (showResetMonth) {
        ConfirmDialog(
            title = "Reset this month?",
            message = "All transactions logged in the current month will be deleted. Goals, bills and settings stay.",
            confirmLabel = "Delete month",
            onConfirm = {
                vm.resetMonth()
                showResetMonth = false
            },
            onDismiss = { showResetMonth = false }
        )
    }

    if (showWipe) {
        ConfirmDialog(
            title = "Erase everything?",
            message = "Citadel will delete ALL data on this device and return to first-run setup. There is no undo and no cloud copy — that's the price of total privacy.",
            confirmLabel = "Erase all",
            onConfirm = {
                showWipe = false
                vm.eraseEverything(onDone = onWiped)
            },
            onDismiss = { showWipe = false }
        )
    }
}

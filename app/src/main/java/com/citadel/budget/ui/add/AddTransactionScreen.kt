package com.citadel.budget.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.citadel.budget.AppContainer
import com.citadel.budget.data.local.Bucket
import com.citadel.budget.data.local.CategoryEntity
import com.citadel.budget.data.local.TransactionEntity
import com.citadel.budget.data.local.TxType
import com.citadel.budget.domain.MoneyMath
import com.citadel.budget.ui.components.AmountField
import com.citadel.budget.ui.components.CategoryIcon
import com.citadel.budget.ui.components.bucketLabel
import com.citadel.budget.ui.components.bucketTint
import com.citadel.budget.util.Fmt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

class AddTransactionViewModel(
    private val c: AppContainer,
    private val txId: Long
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = c.db.categoryDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currency: StateFlow<String> = c.settings.settings
        .map { it.currency }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "USD")

    var type = MutableStateFlow(TxType.EXPENSE)
        private set
    var amount = MutableStateFlow("")
        private set
    var categoryId = MutableStateFlow<Long?>(null)
        private set
    var note = MutableStateFlow("")
        private set
    var epochDay = MutableStateFlow(LocalDate.now().toEpochDay())
        private set
    val editing = MutableStateFlow<TransactionEntity?>(null)

    init {
        if (txId > 0) {
            viewModelScope.launch {
                c.db.transactionDao().byId(txId)?.let { t ->
                    editing.value = t
                    type.value = t.type
                    amount.value = String.format(java.util.Locale.US, "%.2f", t.amountCents / 100.0)
                    categoryId.value = t.categoryId
                    note.value = t.note
                    epochDay.value = t.epochDay
                }
            }
        }
    }

    fun onTypeChange(newType: TxType) {
        type.value = newType
        categoryId.value = null // force re-pick since the category pool changes
    }

    fun onAmountChange(v: String) { amount.value = v }
    fun onNoteChange(v: String) { note.value = v }
    fun onCategoryChange(id: Long) { categoryId.value = id }
    fun onDateChange(day: Long) { epochDay.value = day }

    fun valid(): Boolean =
        MoneyMath.parseCents(amount.value) != null && categoryId.value != null

    fun save(onSuccess: () -> Unit) {
        val cents = MoneyMath.parseCents(amount.value) ?: return
        val catId = categoryId.value ?: return
        viewModelScope.launch {
            val existing = editing.value
            val day = epochDay.value
            val tx = TransactionEntity(
                id = existing?.id ?: 0,
                type = type.value,
                amountCents = cents,
                categoryId = catId,
                note = note.value.trim(),
                epochDay = day,
                monthKey = Fmt.monthKeyOf(LocalDate.ofEpochDay(day)),
                createdAt = existing?.createdAt ?: System.currentTimeMillis()
            )
            if (existing == null) c.db.transactionDao().insert(tx)
            else c.db.transactionDao().update(tx)
            onSuccess()
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            editing.value?.let { c.db.transactionDao().delete(it) }
            onDone()
        }
    }

    companion object {
        fun factory(c: AppContainer, txId: Long) = viewModelFactory {
            initializer { AddTransactionViewModel(c, txId) }
        }
    }
}

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(container: AppContainer, txId: Long, onDone: () -> Unit) {
    val vm: AddTransactionViewModel = viewModel(
        key = "add_$txId",
        factory = AddTransactionViewModel.factory(container, txId)
    )

    val categories by vm.categories.collectAsStateWithLifecycle()
    val currency by vm.currency.collectAsStateWithLifecycle()
    val type by vm.type.collectAsStateWithLifecycle()
    val amount by vm.amount.collectAsStateWithLifecycle()
    val note by vm.note.collectAsStateWithLifecycle()
    val categoryId by vm.categoryId.collectAsStateWithLifecycle()
    val epochDay by vm.epochDay.collectAsStateWithLifecycle()
    val editing by vm.editing.collectAsStateWithLifecycle()

    val haptic = LocalHapticFeedback.current
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (editing != null) "Edit entry" else "New entry")
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (editing != null) {
                        IconButton(onClick = { vm.delete(onDone) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Type selector
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TypeChip("Expense", TxType.EXPENSE, type) { vm.onTypeChange(it) }
                TypeChip("Income", TxType.INCOME, type) { vm.onTypeChange(it) }
                TypeChip("To Vault", TxType.SAVINGS, type) { vm.onTypeChange(it) }
            }

            AmountField(
                value = amount,
                onValueChange = { vm.onAmountChange(it) },
                label = "Amount",
                symbol = Fmt.currencySymbol(currency)
            )

            // Categories grouped by bucket
            val pool: List<Pair<Bucket, List<CategoryEntity>>> = when (type) {
                TxType.INCOME -> listOf(
                    Bucket.INCOME to categories.filter { it.bucket == Bucket.INCOME }
                )
                TxType.SAVINGS -> listOf(
                    Bucket.SAVINGS to categories.filter { it.bucket == Bucket.SAVINGS }
                )
                TxType.EXPENSE -> listOf(
                    Bucket.NEEDS to categories.filter { it.bucket == Bucket.NEEDS },
                    Bucket.WANTS to categories.filter { it.bucket == Bucket.WANTS }
                )
            }
            pool.forEach { (bucket, list) ->
                if (list.isNotEmpty()) {
                    Column {
                        Text(
                            bucketLabel(bucket),
                            style = MaterialTheme.typography.labelLarge,
                            color = bucketTint(bucket),
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        list.chunked(3).forEach { rowCats ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                rowCats.forEach { cat ->
                                    val selected = categoryId == cat.id
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (selected) bucketTint(bucket).copy(alpha = 0.18f)
                                        else MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = if (selected) 2.dp else 1.dp,
                                            color = if (selected) bucketTint(bucket)
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                        ),
                                        onClick = { vm.onCategoryChange(cat.id) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CategoryIcon(
                                                icon = com.citadel.budget.ui.components.CategoryIcons.iconFor(cat.icon),
                                                tint = Color(cat.colorArgb),
                                                size = 26.dp
                                            )
                                            Spacer(Modifier.size(6.dp))
                                            Text(
                                                cat.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                                repeat(3 - rowCats.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Date
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(Fmt.dateTitle(epochDay))
            }

            OutlinedTextField(
                value = note,
                onValueChange = { vm.onNoteChange(it.take(80)) },
                label = { Text("Note (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.save(onSuccess = onDone)
                },
                enabled = vm.valid(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    if (editing != null) "Update entry" else "Save entry",
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = epochDay * 86_400_000L
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { ms ->
                        vm.onDateChange(
                            Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                        )
                    }
                    showDatePicker = false
                }) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun TypeChip(
    label: String,
    value: TxType,
    current: TxType,
    onSelect: (TxType) -> Unit
) {
    val selected = current == value
    FilterChip(
        selected = selected,
        onClick = { onSelect(value) },
        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}

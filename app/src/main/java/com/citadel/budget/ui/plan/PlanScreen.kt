package com.citadel.budget.ui.plan

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.citadel.budget.data.local.TxType
import com.citadel.budget.domain.MoneyMath
import com.citadel.budget.ui.components.AmountField
import com.citadel.budget.ui.components.CategoryIcon
import com.citadel.budget.ui.components.SectionTitle
import com.citadel.budget.ui.components.bucketLabel
import com.citadel.budget.ui.components.bucketTint
import com.citadel.budget.util.Fmt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// State + ViewModel
// ---------------------------------------------------------------------------

data class PlanUiState(
    val loaded: Boolean = false,
    val currency: String = "USD",
    val incomeCents: Long = 0,
    val needsPct: Int = 50,
    val wantsPct: Int = 30,
    val savingsPct: Int = 20,
    val needsSpent: Long = 0,
    val wantsSpent: Long = 0,
    val vaultSpent: Long = 0,
    val categories: List<CategoryEntity> = emptyList()
)

class PlanViewModel(private val c: AppContainer) : ViewModel() {

    private val monthKey = Fmt.monthKeyOf()

    val state: StateFlow<PlanUiState> = combine(
        c.settings.settings,
        c.db.transactionDao().observeMonth(monthKey),
        c.db.categoryDao().observeAll()
    ) { s, txs, cats ->
        var needs = 0L
        var wants = 0L
        var vault = 0L
        txs.forEach { t ->
            when (t.tx.type) {
                TxType.INCOME -> {}
                TxType.SAVINGS -> vault += t.tx.amountCents
                TxType.EXPENSE -> when (t.category?.bucket) {
                    Bucket.WANTS -> wants += t.tx.amountCents
                    Bucket.SAVINGS -> vault += t.tx.amountCents
                    else -> needs += t.tx.amountCents
                }
            }
        }
        PlanUiState(
            loaded = true,
            currency = s.currency,
            incomeCents = s.incomeCents,
            needsPct = s.needsPct,
            wantsPct = s.wantsPct,
            savingsPct = s.savingsPct,
            needsSpent = needs,
            wantsSpent = wants,
            vaultSpent = vault,
            categories = cats
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanUiState())

    fun setIncome(cents: Long) = viewModelScope.launch { c.settings.setIncome(cents) }

    fun setSplit(needs: Int, wants: Int, savings: Int) = viewModelScope.launch {
        c.settings.setSplit(needs, wants, savings)
    }

    fun setLimit(category: CategoryEntity, cents: Long) = viewModelScope.launch {
        c.db.categoryDao().update(category.copy(monthlyLimitCents = cents))
    }

    companion object {
        fun factory(c: AppContainer) = viewModelFactory { initializer { PlanViewModel(c) } }
    }
}

private data class Preset(val label: String, val n: Int, val w: Int, val s: Int)

private val presets = listOf(
    Preset("50 / 30 / 20", 50, 30, 20),
    Preset("60 / 20 / 20", 60, 20, 20),
    Preset("70 / 20 / 10", 70, 20, 10)
)

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@Composable
fun PlanScreen(container: AppContainer) {
    val vm: PlanViewModel = viewModel(factory = PlanViewModel.factory(container))
    val state by vm.state.collectAsStateWithLifecycle()
    val s = state

    if (!s.loaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var needs by remember(s.needsPct) { mutableStateOf(s.needsPct.toFloat()) }
    var wants by remember(s.wantsPct) { mutableStateOf(s.wantsPct.toFloat()) }
    var savings by remember(s.savingsPct) { mutableStateOf(s.savingsPct.toFloat()) }
    var incomeText by remember(s.incomeCents) {
        mutableStateOf(
            if (s.incomeCents > 0) String.format(java.util.Locale.US, "%.2f", s.incomeCents / 100.0) else ""
        )
    }
    var limitTarget by remember { mutableStateOf<CategoryEntity?>(null) }

    val sum = needs.roundToInt() + wants.roundToInt() + savings.roundToInt()
    val split = MoneyMath.split(s.incomeCents, needs.roundToInt(), wants.roundToInt(), savings.roundToInt())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "The Blueprint",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Shape how your fortress is built every month.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Income
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp)) {
                    SectionTitle("Monthly income")
                    Spacer(Modifier.size(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AmountField(
                            value = incomeText,
                            onValueChange = { incomeText = it },
                            label = "Take-home pay",
                            symbol = Fmt.currencySymbol(s.currency),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.size(10.dp))
                        Button(
                            onClick = { vm.setIncome(MoneyMath.parseCents(incomeText) ?: 0) },
                            enabled = (MoneyMath.parseCents(incomeText) ?: 0) != s.incomeCents
                        ) { Text("Save") }
                    }
                }
            }
        }

        // Split sliders
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp)) {
                    SectionTitle("Your split")
                    Spacer(Modifier.size(4.dp))
                    Text(
                        if (sum == 100) "Total: $sum% — ready to apply."
                        else "Total: $sum% — must equal 100% to apply.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (sum == 100) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.size(8.dp))

                    SplitSlider("Needs · The Walls", bucketTint(Bucket.NEEDS), needs, split.needsCents, s.currency) {
                        needs = it
                    }
                    SplitSlider("Wants · The Gardens", bucketTint(Bucket.WANTS), wants, split.wantsCents, s.currency) {
                        wants = it
                    }
                    SplitSlider("Savings · The Vault", bucketTint(Bucket.SAVINGS), savings, split.savingsCents, s.currency) {
                        savings = it
                    }

                    Spacer(Modifier.size(8.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { p ->
                            FilterChip(
                                selected = needs.roundToInt() == p.n && wants.roundToInt() == p.w && savings.roundToInt() == p.s,
                                onClick = {
                                    needs = p.n.toFloat()
                                    wants = p.w.toFloat()
                                    savings = p.s.toFloat()
                                },
                                label = { Text(p.label) }
                            )
                        }
                    }

                    Spacer(Modifier.size(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                vm.setSplit(needs.roundToInt(), wants.roundToInt(), savings.roundToInt())
                            },
                            enabled = sum == 100,
                            modifier = Modifier.weight(1f)
                        ) { Text("Apply split") }
                        OutlinedButton(
                            onClick = {
                                needs = 50f; wants = 30f; savings = 20f
                            }
                        ) { Text("Reset") }
                    }
                }
            }
        }

        // Bucket progress
        item {
            BucketProgressRow(Bucket.NEEDS, split.needsCents, s.needsSpent, s.currency)
            Spacer(Modifier.size(8.dp))
            BucketProgressRow(Bucket.WANTS, split.wantsCents, s.wantsSpent, s.currency)
            Spacer(Modifier.size(8.dp))
            BucketProgressRow(Bucket.SAVINGS, split.savingsCents, s.vaultSpent, s.currency)
        }

        // Category limits
        item { SectionTitle("Category limits") }
        item {
            Text(
                "Set a monthly cap per category. Citadel flags the wall when you cross it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val grouped = Bucket.entries.filter { it != Bucket.INCOME }
        grouped.forEach { bucket ->
            val cats = s.categories.filter { it.bucket == bucket }
            if (cats.isNotEmpty()) {
                item(key = "hdr_$bucket") {
                    Text(
                        bucketLabel(bucket),
                        style = MaterialTheme.typography.labelLarge,
                        color = bucketTint(bucket),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                items(cats.size, key = { cats[it].id }) { i ->
                    val cat = cats[i]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        CategoryIcon(category = cat, size = 34.dp)
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(cat.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                if (cat.monthlyLimitCents > 0) "Limit: ${Fmt.money(cat.monthlyLimitCents, s.currency, true)}"
                                else "No limit set",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { limitTarget = cat }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Set limit")
                        }
                    }
                }
            }
        }

        // How it works
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    SectionTitle("How 50/30/20 works")
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "🏠 50% Needs — rent, bills, groceries, transport, insurance\n" +
                            "🍔 30% Wants — dining out, shopping, entertainment, travel\n" +
                            "💵 20% Savings — emergency fund, investing, retirement, big goals",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "Prefer Ramit Sethi's way? Build a custom split that spends lavishly on what you love and cuts hard on what you don't — that's what the sliders are for.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }

    limitTarget?.let { target ->
        var limitText by remember(target.id) {
            mutableStateOf(
                if (target.monthlyLimitCents > 0)
                    String.format(java.util.Locale.US, "%.2f", target.monthlyLimitCents / 100.0)
                else ""
            )
        }
        AlertDialog(
            onDismissRequest = { limitTarget = null },
            title = { Text("Limit for ${target.name}") },
            text = {
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { raw ->
                        limitText = raw.filter { it.isDigit() || it == '.' || it == ',' }.take(12)
                    },
                    label = { Text("Monthly limit (empty = none)") },
                    prefix = { Text(Fmt.currencySymbol(s.currency)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setLimit(target, MoneyMath.parseCents(limitText) ?: 0)
                    limitTarget = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { limitTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SplitSlider(
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    value: Float,
    previewCents: Long,
    currency: String,
    onChange: (Float) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "${value.roundToInt()}% · ${Fmt.money(previewCents, currency, true)}",
                style = MaterialTheme.typography.labelMedium,
                color = tint,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = { onChange(it.roundToInt().toFloat()) },
            valueRange = 0f..100f,
            steps = 19
        )
    }
}

@Composable
private fun BucketProgressRow(bucket: Bucket, allocated: Long, spent: Long, currency: String) {
    val tint = bucketTint(bucket)
    val fraction = if (allocated > 0) (spent.toFloat() / allocated).coerceIn(0f, 1f) else 0f
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    bucketLabel(bucket),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${Fmt.money(spent, currency, true)} / ${Fmt.money(allocated, currency, true)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.size(8.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = if (allocated > 0 && spent > allocated) MaterialTheme.colorScheme.error else tint,
                trackColor = tint.copy(alpha = 0.15f)
            )
        }
    }
}

package com.citadel.budget.ui.reports

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.citadel.budget.data.local.TxType
import com.citadel.budget.ui.components.DonutChart
import com.citadel.budget.ui.components.SectionTitle
import com.citadel.budget.ui.components.TrendChart
import com.citadel.budget.util.Fmt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth

// ---------------------------------------------------------------------------
// State + ViewModel
// ---------------------------------------------------------------------------

data class CatStat(
    val name: String,
    val colorArgb: Long,
    val icon: String,
    val total: Long
)

data class MonthStats(
    val monthKey: String,
    val income: Long = 0,
    val expense: Long = 0,
    val savings: Long = 0,
    val needs: Long = 0,
    val wants: Long = 0,
    val cats: List<CatStat> = emptyList()
) {
    val outflow: Long get() = expense + savings
}

data class ReportsUiState(
    val loaded: Boolean = false,
    val currency: String = "USD",
    val months: List<MonthStats> = emptyList(),
    val selected: Int = 5
)

class ReportsViewModel(c: AppContainer) : ViewModel() {

    private val keys = Fmt.lastMonthKeys(6)
    private val selected = MutableStateFlow(5)

    fun setSelected(i: Int) { selected.value = i.coerceIn(0, keys.size - 1) }

    val state: StateFlow<ReportsUiState> = combine(
        c.settings.settings,
        c.db.transactionDao().observeMonths(keys),
        selected
    ) { s, txs, sel ->
        val months = keys.map { key ->
            val monthTxs = txs.filter { it.tx.monthKey == key }
            var income = 0L
            var expense = 0L
            var savings = 0L
            var needs = 0L
            var wants = 0L
            val byCat = LinkedHashMap<Long, Long>()
            monthTxs.forEach { t ->
                when (t.tx.type) {
                    TxType.INCOME -> income += t.tx.amountCents
                    TxType.SAVINGS -> savings += t.tx.amountCents
                    TxType.EXPENSE -> {
                        expense += t.tx.amountCents
                        when (t.category?.bucket) {
                            Bucket.WANTS -> wants += t.tx.amountCents
                            Bucket.SAVINGS -> savings += t.tx.amountCents
                            else -> needs += t.tx.amountCents
                        }
                        val key0 = t.category?.id ?: 0
                        byCat[key0] = (byCat[key0] ?: 0L) + t.tx.amountCents
                    }
                }
            }
            val cats = byCat.entries
                .sortedByDescending { it.value }
                .mapNotNull { (catId, total) ->
                    val cat = monthTxs.firstOrNull { it.tx.categoryId == catId }?.category
                    if (cat != null) CatStat(cat.name, cat.colorArgb, cat.icon, total) else null
                }
            MonthStats(key, income, expense, savings, needs, wants, cats)
        }
        ReportsUiState(loaded = true, currency = s.currency, months = months, selected = sel)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())

    companion object {
        fun factory(c: AppContainer) = viewModelFactory { initializer { ReportsViewModel(c) } }
    }
}

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(container: AppContainer, onBack: () -> Unit) {
    val vm: ReportsViewModel = viewModel(factory = ReportsViewModel.factory(container))
    val state by vm.state.collectAsStateWithLifecycle()
    val s = state

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (!s.loaded || s.months.isEmpty()) {
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        val month = s.months[s.selected]
        val prev = if (s.selected > 0) s.months[s.selected - 1] else null

        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Month switcher
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    IconButton(
                        onClick = { vm.setSelected(s.selected - 1) },
                        enabled = s.selected > 0
                    ) { Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month") }
                    Text(
                        Fmt.monthTitle(month.monthKey),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    IconButton(
                        onClick = { vm.setSelected(s.selected + 1) },
                        enabled = s.selected < s.months.size - 1
                    ) { Icon(Icons.Filled.ChevronRight, contentDescription = "Next month") }
                }
            }

            // Donut: where money went
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp)) {
                    SectionTitle("Where your money went")
                    Spacer(Modifier.size(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DonutChart(
                            values = listOf(
                                month.needs.toFloat(),
                                month.wants.toFloat(),
                                month.savings.toFloat()
                            ),
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary,
                                MaterialTheme.colorScheme.secondary
                            ),
                            centerTitle = "Outflow",
                            centerValue = Fmt.money(month.outflow, s.currency, true),
                            modifier = Modifier.size(140.dp)
                        )
                        Spacer(Modifier.size(20.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LegendRow("Needs", month.needs, MaterialTheme.colorScheme.primary, s.currency)
                            LegendRow("Wants", month.wants, MaterialTheme.colorScheme.tertiary, s.currency)
                            LegendRow("Savings", month.savings, MaterialTheme.colorScheme.secondary, s.currency)
                        }
                    }
                }
            }

            // Top categories
            if (month.cats.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        SectionTitle("Top categories")
                        Spacer(Modifier.size(12.dp))
                        val maxCat = month.cats.maxOf { it.total }.coerceAtLeast(1)
                        month.cats.take(7).forEach { cat ->
                            CategoryBar(cat, maxCat, s.currency)
                            Spacer(Modifier.size(10.dp))
                        }
                    }
                }
            }

            // 6-month trend
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp)) {
                    SectionTitle("6-month trend")
                    Spacer(Modifier.size(12.dp))
                    TrendChart(
                        income = s.months.map { it.income.toFloat() },
                        outflow = s.months.map { it.outflow.toFloat() },
                        labels = s.months.map { Fmt.monthShort(it.monthKey) },
                        incomeColor = MaterialTheme.colorScheme.secondary,
                        outflowColor = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.size(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LegendDot(MaterialTheme.colorScheme.secondary, "Income")
                        LegendDot(MaterialTheme.colorScheme.primary, "Committed")
                    }
                }
            }

            // Stats grid
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Income", Fmt.money(month.income, s.currency, true), Modifier.weight(1f))
                StatCard("Committed", Fmt.money(month.outflow, s.currency, true), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val rate = if (month.income > 0) {
                    ((month.income - month.expense) * 100 / month.income).toInt()
                } else 0
                StatCard("Savings rate", "$rate%", Modifier.weight(1f))
                val days = YearMonth.parse(month.monthKey).lengthOfMonth()
                StatCard("Avg / day", Fmt.money(month.outflow / days, s.currency, true), Modifier.weight(1f))
            }

            // Insight
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    SectionTitle("The truth, gently")
                    Spacer(Modifier.size(8.dp))
                    val biggest = month.cats.firstOrNull()
                    Text(
                        buildString {
                            if (biggest != null) {
                                append("Your biggest drain was ${biggest.name} at ")
                                append(Fmt.money(biggest.total, s.currency, true))
                                append(". ")
                            }
                            if (prev != null && prev.outflow > 0) {
                                val delta = ((month.outflow - prev.outflow) * 100 / prev.outflow).toInt()
                                append(
                                    if (delta >= 0) "You committed $delta% more than last month. "
                                    else "You committed ${-delta}% less than last month. "
                                )
                            }
                            append("No shame here — just facts. Adjust the plan, not your worth.")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun LegendRow(label: String, amount: Long, color: Color, currency: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(Modifier.size(8.dp))
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        Text(
            Fmt.money(amount, currency, true),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Spacer(Modifier.size(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CategoryBar(cat: CatStat, maxTotal: Long, currency: String) {
    val fraction = (cat.total.toFloat() / maxTotal).coerceIn(0.05f, 1f)
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                cat.name,
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                Fmt.money(cat.total, currency, true),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.size(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(cat.colorArgb))
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

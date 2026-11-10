package com.citadel.budget.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.citadel.budget.domain.MoneyMath
import com.citadel.budget.domain.Wisdom
import com.citadel.budget.domain.WisdomCard
import com.citadel.budget.ui.components.BucketCard
import com.citadel.budget.ui.components.SectionTitle
import com.citadel.budget.ui.components.TxRow
import com.citadel.budget.ui.components.WisdomCard
import com.citadel.budget.ui.components.bucketLabel
import com.citadel.budget.ui.theme.CitadelColors
import com.citadel.budget.ui.theme.scoreColor
import com.citadel.budget.util.Fmt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.max
import kotlin.math.min

// ---------------------------------------------------------------------------
// State + ViewModel
// ---------------------------------------------------------------------------

data class HomeUiState(
    val currency: String = "USD",
    val userName: String = "",
    val monthLabel: String = "",
    val incomeCents: Long = 0,
    val needsAllocated: Long = 0,
    val wantsAllocated: Long = 0,
    val savingsAllocated: Long = 0,
    val needsSpent: Long = 0,
    val wantsSpent: Long = 0,
    val vaultSpent: Long = 0,
    val recent: List<com.citadel.budget.data.local.TxWithCategory> = emptyList(),
    val score: Int = 0,
    val tier: String = "",
    val tip: String = "",
    val wisdom: WisdomCard = Wisdom.cards.first(),
    val dueBills: List<com.citadel.budget.data.local.BillEntity> = emptyList(),
    val leakBucket: Bucket? = null,
    val savingsRatePct: Int = 0
) {
    val outflow: Long get() = needsSpent + wantsSpent + vaultSpent
    val safeToSpend: Long get() = incomeCents - outflow
}

class HomeViewModel(private val c: AppContainer) : ViewModel() {

    private val today: LocalDate = LocalDate.now()
    private val monthKey = Fmt.monthKeyOf(today)

    val state: StateFlow<HomeUiState?> = combine(
        c.settings.settings,
        c.db.transactionDao().observeMonth(monthKey),
        c.db.goalDao().observeAll(),
        c.db.billDao().observeAll()
    ) { s, txs, goals, bills ->
        var needs = 0L
        var wants = 0L
        var vault = 0L
        var expense = 0L
        var income = 0L
        txs.forEach { t ->
            when (t.tx.type) {
                TxType.INCOME -> income += t.tx.amountCents
                TxType.SAVINGS -> vault += t.tx.amountCents
                TxType.EXPENSE -> {
                    expense += t.tx.amountCents
                    when (t.category?.bucket) {
                        Bucket.WANTS -> wants += t.tx.amountCents
                        Bucket.SAVINGS -> vault += t.tx.amountCents
                        else -> needs += t.tx.amountCents
                    }
                }
            }
        }

        val allocated = MoneyMath.split(s.incomeCents, s.needsPct, s.wantsPct, s.savingsPct)

        val savingsRate = if (income > 0) (income - expense).toDouble() / income else 0.0
        val totalAlloc = allocated.total
        val totalOut = expense + vault
        val adherence = when {
            totalAlloc <= 0 -> 0.0
            totalOut <= totalAlloc -> 1.0
            else -> max(0.0, 1.0 - (totalOut - totalAlloc).toDouble() / totalAlloc)
        }
        val efSaved = goals.filter { it.isEmergencyFund }.sumOf { it.savedCents }
        val efMonths = if (s.survivalCents > 0) efSaved.toDouble() / s.survivalCents else 0.0
        val goalProgress = if (goals.isEmpty()) 0.0
        else goals.map { g ->
            if (g.targetCents > 0) min(1.0, g.savedCents.toDouble() / g.targetCents) else 0.0
        }.average()

        val score = MoneyMath.citadelScore(savingsRate, adherence, efMonths, goalProgress)
        val tip = MoneyMath.tipFor(
            savingsWeak = savingsRate < 0.10,
            adherenceWeak = adherence < 0.95,
            efWeak = efMonths < 1.0,
            goalsWeak = goals.isNotEmpty() && goalProgress < 0.5
        )

        val leak = when {
            allocated.needsCents > 0 && needs > allocated.needsCents -> Bucket.NEEDS
            allocated.wantsCents > 0 && wants > allocated.wantsCents -> Bucket.WANTS
            allocated.savingsCents > 0 && vault > allocated.savingsCents -> Bucket.SAVINGS
            else -> null
        }

        val upcoming = bills
            .filter { it.lastPaidMonthKey != monthKey }
            .sortedBy { b ->
                val d = b.dueDay - today.dayOfMonth
                if (d >= 0) d else d - 100
            }
            .take(3)

        HomeUiState(
            currency = s.currency,
            userName = s.userName,
            monthLabel = Fmt.monthTitle(monthKey),
            incomeCents = s.incomeCents,
            needsAllocated = allocated.needsCents,
            wantsAllocated = allocated.wantsCents,
            savingsAllocated = allocated.savingsCents,
            needsSpent = needs,
            wantsSpent = wants,
            vaultSpent = vault,
            recent = txs.take(5),
            score = score,
            tier = MoneyMath.tierFor(score),
            tip = tip,
            wisdom = Wisdom.forToday(),
            dueBills = upcoming,
            leakBucket = leak,
            savingsRatePct = (savingsRate * 100).toInt()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        fun factory(c: AppContainer) = viewModelFactory { initializer { HomeViewModel(c) } }
    }
}

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    container: AppContainer,
    onOpenAdd: (Long) -> Unit,
    onOpenReports: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenActivity: () -> Unit,
    onOpenFortify: () -> Unit
) {
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))
    val state by vm.state.collectAsStateWithLifecycle()
    val s = state

    if (s == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        greeting(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        s.userName.ifBlank { "Commander" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onOpenReports) {
                    Icon(Icons.Filled.BarChart, contentDescription = "Reports")
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                }
            }
        }

        // Hero card
        item { HeroCard(s) }

        // Citadel Score
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.citadel.budget.ui.components.ScoreGauge(
                        score = s.score,
                        ringColor = scoreColor(
                            s.score,
                            dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                        ),
                        modifier = Modifier.size(104.dp)
                    )
                    Spacer(Modifier.size(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Citadel Score",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            s.tier,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.size(4.dp))
                        Text(
                            s.tip,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Leak nudge / savings win
        if (s.leakBucket != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(Modifier.padding(14.dp)) {
                        Text("🚨", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.size(10.dp))
                        Text(
                            "Money leak detected in ${bucketLabel(s.leakBucket)} — you're past the wall. Plug it before it sinks the ship.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        } else if (s.savingsRatePct >= 20 && s.incomeCents > 0) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(Modifier.padding(14.dp)) {
                        Text("🛡️", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.size(10.dp))
                        Text(
                            "The gap is real: you're saving ${s.savingsRatePct}% this month. Wealth is what you don't see.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        // 50/30/20 buckets
        item { SectionTitle("The 50 · 30 · 20 walls") }
        item {
            BucketCard(Bucket.NEEDS, s.needsAllocated, s.needsSpent, s.currency)
        }
        item {
            BucketCard(Bucket.WANTS, s.wantsAllocated, s.wantsSpent, s.currency)
        }
        item {
            BucketCard(Bucket.SAVINGS, s.savingsAllocated, s.vaultSpent, s.currency)
        }

        // Bills due soon
        if (s.dueBills.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(
                                "Bills coming up",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = onOpenFortify) { Text("Manage") }
                        }
                        s.dueBills.forEach { bill ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(bill.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        dueText(bill.dueDay),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    Fmt.money(bill.amountCents, s.currency, true),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Wisdom
        item { WisdomCard(card = s.wisdom) }

        // Recent activity
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Recent activity", Modifier.weight(1f))
                TextButton(onClick = onOpenActivity) { Text("See all") }
            }
        }
        if (s.recent.isEmpty()) {
            item {
                Text(
                    "No movement yet — no judgment here. Tap the + button and log your first entry.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                )
            }
        } else {
            items(s.recent.size) { i ->
                TxRow(tx = s.recent[i], currency = s.currency) { onOpenAdd(s.recent[i].tx.id) }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

// ---------------------------------------------------------------------------
// Pieces
// ---------------------------------------------------------------------------

@Composable
private fun HeroCard(s: HomeUiState) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(CitadelColors.heroGradient))
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.18f)
                ) {
                    Text(
                        s.monthLabel,
                        Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Filled.Shield,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f)
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "Monthly income",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.75f)
            )
            Text(
                Fmt.money(s.incomeCents, s.currency, compact = true),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "Safe to spend",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.75f)
            )
            Text(
                Fmt.money(s.safeToSpend, s.currency, compact = true),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = if (s.safeToSpend < 0) Color(0xFFFFB4AB) else Color.White
            )
            Text(
                "after ${Fmt.money(s.outflow, s.currency, compact = true)} already committed",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(14.dp))
            val fraction = if (s.incomeCents > 0) {
                (s.outflow.toFloat() / s.incomeCents).coerceIn(0f, 1f)
            } else 0f
            val progress by animateFloatAsState(fraction, tween(700), label = "heroProgress")
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.25f)
            )
        }
    }
}

private fun greeting(): String {
    return when (LocalTime.now().hour) {
        in 5..11 -> "Good morning,"
        in 12..16 -> "Good afternoon,"
        else -> "Good evening,"
    }
}

private fun dueText(dueDay: Int): String {
    val today = LocalDate.now().dayOfMonth
    return when {
        dueDay == today -> "Due today"
        dueDay > today -> "Due in ${dueDay - today} day${if (dueDay - today == 1) "" else "s"}"
        else -> "Overdue by ${today - dueDay} day${if (today - dueDay == 1) "" else "s"}"
    }
}

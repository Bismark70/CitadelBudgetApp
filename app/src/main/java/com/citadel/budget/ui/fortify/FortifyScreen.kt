package com.citadel.budget.ui.fortify

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.citadel.budget.AppContainer
import com.citadel.budget.data.local.BillEntity
import com.citadel.budget.data.local.Bucket
import com.citadel.budget.data.local.CategoryEntity
import com.citadel.budget.data.local.GoalEntity
import com.citadel.budget.data.local.TransactionEntity
import com.citadel.budget.data.local.TxType
import com.citadel.budget.domain.MoneyMath
import com.citadel.budget.ui.components.CategoryIcon
import com.citadel.budget.ui.components.EmptyState
import com.citadel.budget.ui.components.SectionTitle
import com.citadel.budget.util.Fmt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

// ---------------------------------------------------------------------------
// State + ViewModel
// ---------------------------------------------------------------------------

data class FortifyUiState(
    val loaded: Boolean = false,
    val currency: String = "USD",
    val survivalCents: Long = 0,
    val babyStepsDone: Set<Int> = emptySet(),
    val goals: List<GoalEntity> = emptyList(),
    val bills: List<BillEntity> = emptyList(),
    val billCategories: List<CategoryEntity> = emptyList()
)

class FortifyViewModel(private val c: AppContainer) : ViewModel() {

    val state: StateFlow<FortifyUiState> = combine(
        c.settings.settings,
        c.db.goalDao().observeAll(),
        c.db.billDao().observeAll(),
        c.db.categoryDao().observeAll()
    ) { s, goals, bills, cats ->
        FortifyUiState(
            loaded = true,
            currency = s.currency,
            survivalCents = s.survivalCents,
            babyStepsDone = s.babyStepsDone,
            goals = goals,
            bills = bills,
            billCategories = cats.filter { it.bucket == Bucket.NEEDS || it.bucket == Bucket.WANTS }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FortifyUiState())

    fun setSurvival(cents: Long) = viewModelScope.launch { c.settings.setSurvival(cents) }

    fun toggleStep(step: Int, done: Boolean) = viewModelScope.launch {
        c.settings.toggleBabyStep(step, done)
    }

    fun addGoal(name: String, targetCents: Long, isEmergencyFund: Boolean) = viewModelScope.launch {
        if (name.isBlank() || targetCents <= 0) return@launch
        c.db.goalDao().insert(
            GoalEntity(
                name = name.trim(),
                targetCents = targetCents,
                isEmergencyFund = isEmergencyFund
            )
        )
    }

    fun deleteGoal(goal: GoalEntity) = viewModelScope.launch { c.db.goalDao().delete(goal) }

    fun contribute(goal: GoalEntity, cents: Long) = viewModelScope.launch {
        if (cents <= 0) return@launch
        c.db.goalDao().update(goal.copy(savedCents = goal.savedCents + cents))
        // Log a vault transfer so the reports stay truthful.
        val cats = c.db.categoryDao().observeAll().first()
        val cat = if (goal.isEmergencyFund) {
            cats.firstOrNull { it.name == "Emergency Fund" }
                ?: cats.firstOrNull { it.bucket == Bucket.SAVINGS }
        } else {
            cats.firstOrNull { it.name == "Goal Transfer" }
                ?: cats.firstOrNull { it.bucket == Bucket.SAVINGS }
        }
        val today = LocalDate.now()
        c.db.transactionDao().insert(
            TransactionEntity(
                type = TxType.SAVINGS,
                amountCents = cents,
                categoryId = cat?.id ?: 0,
                note = "Goal: ${goal.name}",
                epochDay = today.toEpochDay(),
                monthKey = Fmt.monthKeyOf(today)
            )
        )
    }

    fun addBill(name: String, cents: Long, dueDay: Int, categoryId: Long, autoPay: Boolean) =
        viewModelScope.launch {
            if (name.isBlank() || cents <= 0) return@launch
            c.db.billDao().insert(
                BillEntity(
                    name = name.trim(),
                    amountCents = cents,
                    dueDay = dueDay.coerceIn(1, 31),
                    categoryId = categoryId,
                    autoPay = autoPay
                )
            )
        }

    fun deleteBill(bill: BillEntity) = viewModelScope.launch { c.db.billDao().delete(bill) }

    fun markPaid(bill: BillEntity) = viewModelScope.launch {
        val today = LocalDate.now()
        val mk = Fmt.monthKeyOf(today)
        if (bill.lastPaidMonthKey != mk) {
            c.db.transactionDao().insert(
                TransactionEntity(
                    type = TxType.EXPENSE,
                    amountCents = bill.amountCents,
                    categoryId = bill.categoryId,
                    note = "Bill: ${bill.name}",
                    epochDay = today.toEpochDay(),
                    monthKey = mk
                )
            )
            c.db.billDao().update(bill.copy(lastPaidMonthKey = mk))
        }
    }

    companion object {
        fun factory(c: AppContainer) = viewModelFactory { initializer { FortifyViewModel(c) } }
    }
}

private data class BabyStep(val n: Int, val title: String, val desc: String)

private val babySteps = listOf(
    BabyStep(1, "Starter emergency fund", "Save a small starter buffer (about \$1,000) before attacking anything else."),
    BabyStep(2, "Debt snowball", "List debts smallest to largest. Pay minimums on all, attack the smallest first."),
    BabyStep(3, "Full emergency fund", "3–6 months of expenses. Use the Survival Number tool on the Goals tab."),
    BabyStep(4, "Invest 15% of income", "Retirement first — steady investing beats hot picks."),
    BabyStep(5, "College fund", "Save for children's education once retirement is on track."),
    BabyStep(6, "Pay off the house", "Kill the mortgage early and own your walls outright."),
    BabyStep(7, "Build wealth & give", "Invest generously, live generously. Live like no one else.")
)

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FortifyScreen(container: AppContainer) {
    val vm: FortifyViewModel = viewModel(factory = FortifyViewModel.factory(container))
    val state by vm.state.collectAsStateWithLifecycle()
    val s = state

    if (!s.loaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    var tab by remember { mutableIntStateOf(0) }
    val titles = listOf("Goals", "Bills", "Baby Steps")

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.size(12.dp))
            Text(
                "Fortify the Fortress",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.size(8.dp))
        }
        TabRow(selectedTabIndex = tab) {
            titles.forEachIndexed { i, title ->
                Tab(
                    selected = tab == i,
                    onClick = { tab = i },
                    text = { Text(title) }
                )
            }
        }
        when (tab) {
            0 -> GoalsTab(vm, s)
            1 -> BillsTab(vm, s)
            else -> StepsTab(vm, s)
        }
    }
}

// ---------------------------------------------------------------------------
// Goals tab
// ---------------------------------------------------------------------------

@Composable
private fun GoalsTab(vm: FortifyViewModel, s: FortifyUiState) {
    var showNewGoal by remember { mutableStateOf(false) }
    var contributeTarget by remember { mutableStateOf<GoalEntity?>(null) }
    var showSurvival by remember { mutableStateOf(false) }

    val efGoals = s.goals.filter { it.isEmergencyFund }
    val efSaved = efGoals.sumOf { it.savedCents }
    val otherGoals = s.goals.filter { !it.isEmergencyFund }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Emergency fund / moat
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "Emergency Fund · The Moat",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { showSurvival = true }) { Text("Survival #") }
                    }
                    if (efGoals.isEmpty()) {
                        Spacer(Modifier.size(4.dp))
                        Text(
                            "Every fortress needs a moat. This is Ramsey's Baby Step 1–3 and Aliche's #1 rule: build your buffer before anything else.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.size(10.dp))
                        Button(onClick = { showNewGoal = true }) { Text("Create emergency fund") }
                    } else {
                        Spacer(Modifier.size(10.dp))
                        Text(
                            Fmt.money(efSaved, s.currency, true),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.size(10.dp))
                        listOf(
                            "1 month" to 1,
                            "3 months" to 3,
                            "6 months" to 6
                        ).forEach { (label, months) ->
                            val target = s.survivalCents * months
                            val frac = if (target > 0) (efSaved.toFloat() / target).coerceIn(0f, 1f) else 0f
                            Column(Modifier.padding(vertical = 4.dp)) {
                                Row {
                                    Text(
                                        label,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        "${Fmt.money(efSaved, s.currency, true)} / ${Fmt.money(target, s.currency, true)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { frac },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = MaterialTheme.colorScheme.secondary,
                                    trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.15f)
                                )
                            }
                        }
                        if (s.survivalCents == 0L) {
                            Text(
                                "Set your Survival Number to see real targets.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Savings goals", Modifier.weight(1f))
                TextButton(onClick = { showNewGoal = true }) { Text("New goal") }
            }
        }

        if (otherGoals.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.Flag,
                    title = "No goals yet",
                    message = "Dream a little: a trip, a laptop, a debt crushed. Name it, target it, feed it."
                )
            }
        } else {
            items(otherGoals.size, key = { otherGoals[it].id }) { i ->
                val goal = otherGoals[i]
                val frac = if (goal.targetCents > 0) {
                    (goal.savedCents.toFloat() / goal.targetCents).coerceIn(0f, 1f)
                } else 0f
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                goal.name,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { vm.deleteGoal(goal) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete goal", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        LinearProgressIndicator(
                            progress = { frac },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                        )
                        Spacer(Modifier.size(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${Fmt.money(goal.savedCents, s.currency, true)} of ${Fmt.money(goal.targetCents, s.currency, true)} · ${(frac * 100).toInt()}%",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = { contributeTarget = goal }) { Text("Add money") }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }

    if (showNewGoal) {
        NewGoalDialog(
            currency = s.currency,
            onDismiss = { showNewGoal = false },
            onConfirm = { name, cents, isEf ->
                vm.addGoal(name, cents, isEf)
                showNewGoal = false
            }
        )
    }

    contributeTarget?.let { goal ->
        var amountText by remember(goal.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { contributeTarget = null },
            title = { Text("Add to \"${goal.name}\"") },
            text = {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { raw -> amountText = raw.filter { it.isDigit() || it == '.' || it == ',' }.take(12) },
                    label = { Text("Amount") },
                    prefix = { Text(Fmt.currencySymbol(s.currency)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.contribute(goal, MoneyMath.parseCents(amountText) ?: 0)
                    contributeTarget = null
                }) { Text("Transfer") }
            },
            dismissButton = {
                TextButton(onClick = { contributeTarget = null }) { Text("Cancel") }
            }
        )
    }

    if (showSurvival) {
        var survivalText by remember {
            mutableStateOf(
                if (s.survivalCents > 0) String.format(java.util.Locale.US, "%.2f", s.survivalCents / 100.0) else ""
            )
        }
        AlertDialog(
            onDismissRequest = { showSurvival = false },
            title = { Text("Your Survival Number") },
            text = {
                Column {
                    Text(
                        "The true monthly cost of your bare-needs life: rent, food, utilities, transport, insurance. Aliche calls this knowing your numbers; Ramsey calls it building the moat.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.size(12.dp))
                    OutlinedTextField(
                        value = survivalText,
                        onValueChange = { raw -> survivalText = raw.filter { it.isDigit() || it == '.' || it == ',' }.take(12) },
                        label = { Text("Monthly survival cost") },
                        prefix = { Text(Fmt.currencySymbol(s.currency)) },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setSurvival(MoneyMath.parseCents(survivalText) ?: 0)
                    showSurvival = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSurvival = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun NewGoalDialog(
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var isEf by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New savings goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text("Goal name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { raw -> target = raw.filter { it.isDigit() || it == '.' || it == ',' }.take(12) },
                    label = { Text("Target amount") },
                    prefix = { Text(Fmt.currencySymbol(currency)) },
                    singleLine = true
                )
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isEf, onCheckedChange = { isEf = it })
                    Text("This is my emergency fund (the moat)", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(name, MoneyMath.parseCents(target) ?: 0, isEf)
            }) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ---------------------------------------------------------------------------
// Bills tab
// ---------------------------------------------------------------------------

private sealed interface BillStatus {
    data object Paid : BillStatus
    data class Upcoming(val days: Int) : BillStatus
    data object Overdue : BillStatus
}

private fun billStatus(bill: BillEntity): BillStatus {
    val today = LocalDate.now()
    val mk = Fmt.monthKeyOf(today)
    return when {
        bill.lastPaidMonthKey == mk -> BillStatus.Paid
        bill.dueDay == today.dayOfMonth -> BillStatus.Upcoming(0)
        bill.dueDay > today.dayOfMonth -> BillStatus.Upcoming(bill.dueDay - today.dayOfMonth)
        else -> BillStatus.Overdue
    }
}

@Composable
private fun BillsTab(vm: FortifyViewModel, s: FortifyUiState) {
    var showNewBill by remember { mutableStateOf(false) }

    val monthlyTotal = s.bills.sumOf { it.amountCents }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Committed monthly bills",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            Fmt.money(monthlyTotal, s.currency, true),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Never miss a payment. That's how walls stay standing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Button(onClick = { showNewBill = true }) { Text("New bill") }
                }
            }
        }

        if (s.bills.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.ReceiptLong,
                    title = "No bills tracked",
                    message = "Add rent, subscriptions, insurance — anything recurring. Citadel will keep watch."
                )
            }
        } else {
            items(s.bills.size, key = { s.bills[it].id }) { i ->
                val bill = s.bills[i]
                val status = billStatus(bill)
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.size(10.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        bill.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (bill.autoPay) {
                                        Spacer(Modifier.size(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.tertiaryContainer
                                        ) {
                                            Text(
                                                "⚡ Auto",
                                                Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                        }
                                    }
                                }
                                Text(
                                    "Due day ${bill.dueDay} · ${Fmt.money(bill.amountCents, s.currency)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val (label, container, content) = when (status) {
                                BillStatus.Paid -> Triple(
                                    "Paid",
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                is BillStatus.Upcoming -> if (status.days == 0) Triple(
                                    "Due today",
                                    MaterialTheme.colorScheme.tertiaryContainer,
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                ) else Triple(
                                    "In ${status.days}d",
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                BillStatus.Overdue -> Triple(
                                    "Overdue",
                                    MaterialTheme.colorScheme.errorContainer,
                                    MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Surface(shape = RoundedCornerShape(50), color = container) {
                                Text(
                                    label,
                                    Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = content,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        if (status != BillStatus.Paid) {
                            Spacer(Modifier.size(8.dp))
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { vm.markPaid(bill) }) { Text("Mark paid") }
                                IconButton(onClick = { vm.deleteBill(bill) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete bill", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }

    if (showNewBill) {
        NewBillDialog(
            s = s,
            onDismiss = { showNewBill = false },
            onConfirm = { name, cents, day, catId, auto ->
                vm.addBill(name, cents, day, catId, auto)
                showNewBill = false
            }
        )
    }
}

@Composable
private fun NewBillDialog(
    s: FortifyUiState,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Int, Long, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dueDay by remember { mutableStateOf("1") }
    var autoPay by remember { mutableStateOf(false) }
    var catId by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New bill") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text("Bill name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { raw -> amount = raw.filter { it.isDigit() || it == '.' || it == ',' }.take(12) },
                    label = { Text("Amount") },
                    prefix = { Text(Fmt.currencySymbol(s.currency)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = dueDay,
                    onValueChange = { raw -> dueDay = raw.filter { it.isDigit() }.take(2) },
                    label = { Text("Due day of month (1–31)") },
                    singleLine = true
                )
                Text("Category", style = MaterialTheme.typography.labelMedium)
                androidx.compose.foundation.layout.Column {
                    s.billCategories.chunked(3).forEach { row ->
                        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { cat ->
                                FilterChip(
                                    selected = catId == cat.id,
                                    onClick = { catId = cat.id },
                                    label = { Text(cat.name, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = autoPay, onCheckedChange = { autoPay = it })
                    Text("Auto-paid", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    name,
                    MoneyMath.parseCents(amount) ?: 0,
                    (dueDay.toIntOrNull() ?: 1).coerceIn(1, 31),
                    catId ?: 0,
                    autoPay
                )
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ---------------------------------------------------------------------------
// Baby Steps tab
// ---------------------------------------------------------------------------

@Composable
private fun StepsTab(vm: FortifyViewModel, s: FortifyUiState) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Dave Ramsey's Baby Steps",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        "The proven build order from The Total Money Makeover — check off each step as your fortress rises.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        items(babySteps.size, key = { babySteps[it].n }) { i ->
            val step = babySteps[i]
            val done = step.n in s.babyStepsDone
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = done,
                        onCheckedChange = { vm.toggleStep(step.n, it) }
                    )
                    Spacer(Modifier.size(6.dp))
                    Column {
                        Text(
                            "${step.n}. ${step.title}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            step.desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item {
            Text(
                "“Live like no one else, so later you can live like no one else.” — Dave Ramsey",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )
        }
    }
}

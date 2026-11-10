package com.citadel.budget.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.citadel.budget.data.local.TxType
import com.citadel.budget.data.local.TxWithCategory
import com.citadel.budget.ui.components.EmptyState
import com.citadel.budget.ui.components.TxRow
import com.citadel.budget.ui.components.bucketLabel
import com.citadel.budget.util.Fmt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

// ---------------------------------------------------------------------------
// State + ViewModel
// ---------------------------------------------------------------------------

data class ActivityUiState(
    val loaded: Boolean = false,
    val currency: String = "USD",
    val all: List<TxWithCategory> = emptyList(),
    val filter: String = "ALL",
    val monthIncome: Long = 0,
    val monthOut: Long = 0
)

class TransactionsViewModel(c: AppContainer) : ViewModel() {

    private val filter = MutableStateFlow("ALL")

    fun setFilter(f: String) { filter.value = f }

    val state: StateFlow<ActivityUiState> = combine(
        c.settings.settings,
        c.db.transactionDao().observeRecent(1_000),
        c.db.transactionDao().observeMonth(Fmt.monthKeyOf()),
        filter
    ) { s, all, month, f ->
        ActivityUiState(
            loaded = true,
            currency = s.currency,
            all = all,
            filter = f,
            monthIncome = month.filter { it.tx.type == TxType.INCOME }.sumOf { it.tx.amountCents },
            monthOut = month.filter { it.tx.type != TxType.INCOME }.sumOf { it.tx.amountCents }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActivityUiState())

    companion object {
        fun factory(c: AppContainer) = viewModelFactory { initializer { TransactionsViewModel(c) } }
    }
}

private val filterChips = listOf(
    "ALL" to "All",
    "NEEDS" to "Walls",
    "WANTS" to "Gardens",
    "SAVINGS" to "Vault",
    "INCOME" to "Income"
)

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@Composable
fun TransactionsScreen(container: AppContainer, onEdit: (Long) -> Unit) {
    val vm: TransactionsViewModel = viewModel(factory = TransactionsViewModel.factory(container))
    val state by vm.state.collectAsStateWithLifecycle()
    val s = state

    if (!s.loaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val query = remember { mutableStateOf("") }

    val filtered = s.all.filter { item ->
        val matchesFilter = when (s.filter) {
            "INCOME" -> item.tx.type == TxType.INCOME
            "NEEDS" -> item.category?.bucket == Bucket.NEEDS
            "WANTS" -> item.category?.bucket == Bucket.WANTS
            "SAVINGS" -> item.category?.bucket == Bucket.SAVINGS || item.tx.type == TxType.SAVINGS
            else -> true
        }
        val matchesQuery = query.value.isBlank() ||
            item.tx.note.contains(query.value, ignoreCase = true) ||
            (item.category?.name ?: "").contains(query.value, ignoreCase = true)
        matchesFilter && matchesQuery
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.size(12.dp))
        Text(
            "Money Activity",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.size(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        "In this month",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        "+${Fmt.money(s.monthIncome, s.currency, true)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.errorContainer) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        "Out this month",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        "−${Fmt.money(s.monthOut, s.currency, true)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        Spacer(Modifier.size(12.dp))
        OutlinedTextField(
            value = query.value,
            onValueChange = { query.value = it },
            placeholder = { Text("Search notes or categories") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.size(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterChips.forEach { (key, label) ->
                FilterChip(
                    selected = s.filter == key,
                    onClick = { vm.setFilter(key) },
                    label = { Text(label) }
                )
            }
        }

        if (filtered.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Search,
                title = "Nothing here",
                message = "No transactions match this view. Try another filter, or add one with the + button."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(filtered) { idx, item ->
                    if (idx == 0 || filtered[idx - 1].tx.epochDay != item.tx.epochDay) {
                        Text(
                            Fmt.dateTitle(item.tx.epochDay),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 14.dp, bottom = 4.dp, start = 4.dp)
                        )
                    }
                    TxRow(tx = item, currency = s.currency) { onEdit(item.tx.id) }
                }
            }
        }
    }
}

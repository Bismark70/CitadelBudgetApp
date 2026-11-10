package com.citadel.budget.ui.onboarding

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.citadel.budget.R
import com.citadel.budget.AppContainer
import com.citadel.budget.domain.MoneyMath
import com.citadel.budget.domain.Wisdom
import com.citadel.budget.ui.theme.CitadelColors
import com.citadel.budget.util.Fmt
import com.citadel.budget.util.SupportedCurrencies
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(container: AppContainer, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 3 })

    var name by remember { mutableStateOf("") }
    var incomeText by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("USD") }

    val incomeCents = MoneyMath.parseCents(incomeText) ?: 0L
    val split = MoneyMath.split(incomeCents, 50, 30, 20)

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> WelcomePage(name) { name = it }
                    1 -> IncomePage(
                        incomeText = incomeText,
                        currency = currency,
                        onIncomeChange = { incomeText = it },
                        onCurrencyChange = { currency = it }
                    )
                    else -> PreviewPage(split, currency, incomeCents)
                }
            }

            // Page dots
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(3) { i ->
                    val active = pagerState.currentPage == i
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (active) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            )
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (pagerState.currentPage > 0) {
                    OutlinedButton(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Back") }
                }
                Button(
                    onClick = {
                        when (pagerState.currentPage) {
                            0, 1 -> scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            else -> {
                                scope.launch {
                                    container.settings.finishOnboarding(
                                        name = name.ifBlank { "Commander" },
                                        incomeCents = incomeCents,
                                        currency = currency
                                    )
                                }
                                onDone()
                            }
                        }
                    },
                    enabled = pagerState.currentPage != 1 || incomeCents > 0,
                    modifier = Modifier.weight(2f)
                ) {
                    Text(
                        when (pagerState.currentPage) {
                            0 -> "Let's build"
                            1 -> "Continue"
                            else -> "Build my fortress"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomePage(name: String, onNameChange: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(48.dp))
                .background(Brush.linearGradient(CitadelColors.heroGradient)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = "Citadel Budget logo",
                modifier = Modifier.size(170.dp)
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(
            "Citadel Budget",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Build your money fortress with the 50/30/20 rule.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("What should we call you?") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "“${Wisdom.forToday().principle}”",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun IncomePage(
    incomeText: String,
    currency: String,
    onIncomeChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Monthly take-home pay",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Your fortress is built from what comes in. We'll never ask for bank logins — everything stays on this device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = incomeText,
            onValueChange = { raw ->
                onIncomeChange(raw.filter { it.isDigit() || it == '.' || it == ',' }.take(14))
            },
            label = { Text("Monthly income") },
            prefix = { Text(Fmt.currencySymbol(currency)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "Currency",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SupportedCurrencies.codes.forEach { code ->
                FilterChip(
                    selected = currency == code,
                    onClick = { onCurrencyChange(code) },
                    label = { Text(code) }
                )
            }
        }
        Text(
            "More currencies available in Settings later.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PreviewPage(split: com.citadel.budget.domain.BucketSplit, currency: String, incomeCents: Long) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Your 50 / 30 / 20 fortress",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Every month, Citadel splits your income into three strongholds:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))

        PreviewRow("🏠", "Needs · The Walls", "50%", split.needsCents, currency)
        PreviewRow("🍔", "Wants · The Gardens", "30%", split.wantsCents, currency)
        PreviewRow("💎", "Savings · The Vault", "20%", split.savingsCents, currency)

        Spacer(Modifier.height(20.dp))
        Text(
            if (incomeCents > 0) "You can change these percentages anytime in the Plan tab."
            else "Enter your income on the previous step to see your numbers.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PreviewRow(emoji: String, label: String, pct: String, amount: Long, currency: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(pct, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                Fmt.money(amount, currency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

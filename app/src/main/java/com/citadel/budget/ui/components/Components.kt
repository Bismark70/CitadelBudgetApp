package com.citadel.budget.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.citadel.budget.data.local.Bucket
import com.citadel.budget.data.local.CategoryEntity
import com.citadel.budget.data.local.TxType
import com.citadel.budget.data.local.TxWithCategory
import com.citadel.budget.domain.WisdomCard
import com.citadel.budget.util.Fmt

object CategoryIcons {
    fun iconFor(key: String): ImageVector = when (key) {
        "home" -> Icons.Filled.Home
        "groceries" -> Icons.Filled.ShoppingCart
        "utilities" -> Icons.Filled.Bolt
        "transport" -> Icons.Filled.DirectionsCar
        "insurance" -> Icons.Filled.Shield
        "health" -> Icons.Filled.LocalHospital
        "dining" -> Icons.Filled.Restaurant
        "shopping" -> Icons.Filled.ShoppingBag
        "fun" -> Icons.Filled.Movie
        "travel" -> Icons.Filled.Flight
        "hobby" -> Icons.Filled.Palette
        "subs" -> Icons.Filled.Subscriptions
        "ef" -> Icons.Filled.HealthAndSafety
        "invest" -> Icons.Filled.TrendingUp
        "retire" -> Icons.Filled.AccountBalance
        "debt" -> Icons.Filled.CreditCard
        "big" -> Icons.Filled.Star
        "goal" -> Icons.Filled.Flag
        "salary" -> Icons.Filled.Work
        "hustle" -> Icons.Filled.Handyman
        "other" -> Icons.Filled.AttachMoney
        "vault" -> Icons.Filled.Savings
        "drop" -> Icons.Filled.WaterDrop
        "heart" -> Icons.Filled.Favorite
        else -> Icons.Filled.Category
    }
}

@Composable
fun CategoryIcon(icon: ImageVector, tint: Color, size: Dp = 36.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}

@Composable
fun CategoryIcon(category: CategoryEntity?, size: Dp = 36.dp, modifier: Modifier = Modifier) {
    val tint = category?.let { Color(it.colorArgb) } ?: MaterialTheme.colorScheme.onSurfaceVariant
    CategoryIcon(
        icon = category?.let { CategoryIcons.iconFor(it.icon) } ?: Icons.Filled.HelpOutline,
        tint = tint,
        size = size,
        modifier = modifier
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
}

fun bucketLabel(bucket: Bucket): String = when (bucket) {
    Bucket.NEEDS -> "Needs · The Walls"
    Bucket.WANTS -> "Wants · The Gardens"
    Bucket.SAVINGS -> "Savings · The Vault"
    Bucket.INCOME -> "Income"
}

fun bucketIcon(bucket: Bucket): ImageVector = when (bucket) {
    Bucket.NEEDS -> Icons.Filled.Home
    Bucket.WANTS -> Icons.Filled.ShoppingBag
    Bucket.SAVINGS -> Icons.Filled.Savings
    Bucket.INCOME -> Icons.Filled.AttachMoney
}

@Composable
fun bucketTint(bucket: Bucket): Color = when (bucket) {
    Bucket.NEEDS -> MaterialTheme.colorScheme.primary
    Bucket.WANTS -> MaterialTheme.colorScheme.tertiary
    Bucket.SAVINGS -> MaterialTheme.colorScheme.secondary
    Bucket.INCOME -> MaterialTheme.colorScheme.secondary
}

@Composable
fun BucketCard(
    bucket: Bucket,
    allocated: Long,
    spent: Long,
    currency: String,
    modifier: Modifier = Modifier
) {
    val tint = bucketTint(bucket)
    val over = allocated > 0 && spent > allocated
    val fraction = if (allocated > 0) (spent.toFloat() / allocated).coerceIn(0f, 1f) else 0f
    val progress by animateFloatAsState(fraction, tween(700), label = "bucketProgress")

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryIcon(icon = bucketIcon(bucket), tint = tint, size = 34.dp)
                Spacer(Modifier.size(10.dp))
                Text(
                    bucketLabel(bucket),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${Fmt.money(spent, currency, true)} / ${Fmt.money(allocated, currency, true)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.size(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = if (over) MaterialTheme.colorScheme.error else tint,
                trackColor = tint.copy(alpha = 0.15f)
            )
            Spacer(Modifier.size(6.dp))
            Text(
                text = if (over) {
                    "Over by ${Fmt.money(spent - allocated, currency)} — plug the leak!"
                } else {
                    "${Fmt.money((allocated - spent).coerceAtLeast(0), currency, true)} left"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TxRow(tx: TxWithCategory, currency: String, onClick: () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        CategoryIcon(category = tx.category, size = 40.dp)
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                tx.category?.name ?: "Uncategorized",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            val sub = tx.tx.note.ifBlank { Fmt.dayShort(tx.tx.epochDay) }
            Text(
                sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        val prefix: String
        val color: Color
        when (tx.tx.type) {
            TxType.INCOME -> {
                prefix = "+"
                color = MaterialTheme.colorScheme.secondary
            }
            TxType.SAVINGS -> {
                prefix = "→ "
                color = MaterialTheme.colorScheme.tertiary
            }
            TxType.EXPENSE -> {
                prefix = "− "
                color = MaterialTheme.colorScheme.onSurface
            }
        }
        Text(
            prefix + Fmt.money(tx.tx.amountCents, currency),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
fun WisdomCard(card: WisdomCard, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp)) {
            Icon(
                Icons.Filled.FormatQuote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.size(12.dp))
            Column {
                Text(
                    card.principle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    "— ${card.source}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CategoryIcon(icon = icon, tint = MaterialTheme.colorScheme.primary, size = 64.dp)
        Spacer(Modifier.size(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.size(6.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "Confirm",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    symbol: String = "$",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            onValueChange(raw.filter { it.isDigit() || it == '.' || it == ',' }.take(14))
        },
        label = { Text(label) },
        prefix = { Text(symbol) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun RowSpacer(width: Dp = 8.dp) {
    Spacer(Modifier.size(width))
}

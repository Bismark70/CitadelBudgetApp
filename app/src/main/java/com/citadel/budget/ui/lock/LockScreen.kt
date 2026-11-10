package com.citadel.budget.ui.lock

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citadel.budget.AppContainer
import com.citadel.budget.data.SettingsStore
import com.citadel.budget.data.UserSettings
import com.citadel.budget.ui.theme.CitadelColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val keyRows = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf("ERASE", "0", "BACK")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockScreen(container: AppContainer, onUnlock: () -> Unit, onReset: () -> Unit) {
    val scope = rememberCoroutineScope()
    val settings by container.settings.settings
        .collectAsStateWithLifecycle(initialValue = UserSettings())

    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }

    fun submit(candidate: String) {
        val s = settings
        if (s.pinSalt.isNotEmpty() && SettingsStore.hash(s.pinSalt, candidate) == s.pinHash) {
            onUnlock()
        } else {
            error = true
            pin = ""
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(CitadelColors.heroGradient))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.Shield,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Citadel Gate",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "Enter your 4-digit PIN",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(4) { i ->
                    val filled = i < pin.length
                    Box(
                        Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    filled -> Color.White
                                    error -> Color(0xFFFFB4AB)
                                    else -> Color.White.copy(alpha = 0.35f)
                                }
                            )
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            if (error) {
                Text(
                    "Wrong PIN — try again.",
                    color = Color(0xFFFFB4AB),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(24.dp))

            keyRows.forEach { rowKeys ->
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    rowKeys.forEach { key ->
                        Surface(
                            onClick = {
                                error = false
                                when (key) {
                                    "BACK" -> if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                    "ERASE" -> pin = ""
                                    else -> if (pin.length < 4) {
                                        pin += key
                                        if (pin.length == 4) submit(pin)
                                    }
                                }
                            },
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier.size(66.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                when (key) {
                                    "BACK" -> Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Delete digit",
                                        tint = Color.White
                                    )
                                    "ERASE" -> Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Clear",
                                        tint = Color.White
                                    )
                                    else -> Text(
                                        key,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { showReset = true }) {
                Text("Forgot PIN? Reset the app", color = Color.White.copy(alpha = 0.7f))
            }
        }
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("Erase everything?") },
            text = {
                Text(
                    "To bypass a forgotten PIN, Citadel will erase all local data " +
                        "(transactions, goals, bills and settings). This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showReset = false
                    scope.launch(Dispatchers.IO) {
                        container.db.clearAllTables()
                        container.settings.eraseAll()
                    }
                    onReset()
                }) {
                    Text("Erase", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReset = false }) { Text("Cancel") }
            }
        )
    }
}

package com.citadel.budget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citadel.budget.data.UserSettings
import com.citadel.budget.ui.nav.CitadelNavGraph
import com.citadel.budget.ui.theme.CitadelTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CitadelBudgetApp).container

        setContent {
            val settings by container.settings.settings
                .collectAsStateWithLifecycle(initialValue = null as UserSettings?)

            val s = settings
            if (s == null) {
                LoadingScreen()
            } else {
                CitadelTheme(themeMode = s.themeMode) {
                    CitadelNavGraph(container = container, settings = s)
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

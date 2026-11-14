package com.citadel.budget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citadel.budget.data.UserSettings
import com.citadel.budget.ui.nav.CitadelNavGraph
import com.citadel.budget.ui.theme.CitadelTheme
import kotlinx.coroutines.delay

/**
 * Launch flow:
 * 1. System splash (Android 12+) shows the launcher ICON (shield only) on white.
 * 2. [BrandedSplashScreen] then shows the FULL logo with all labels
 *    ("Citadel Budget" + "Track. Plan. Grow.") while settings load, held for a
 *    minimum beat so the brand moment is never a random flash.
 * 3. The nav graph takes over (onboarding / lock / home).
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CitadelBudgetApp).container

        setContent {
            val settings by container.settings.settings
                .collectAsStateWithLifecycle(initialValue = null as UserSettings?)
            var minSplashDone by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(900)
                minSplashDone = true
            }

            val s = settings
            if (s == null || !minSplashDone) {
                BrandedSplashScreen()
            } else {
                CitadelTheme(themeMode = s.themeMode) {
                    CitadelNavGraph(container = container, settings = s)
                }
            }
        }
    }
}

/** Full-logo branded opening screen: white backdrop, gently breathing logo. */
@Composable
private fun BrandedSplashScreen() {
    val transition = rememberInfiniteTransition(label = "brand")
    val glow by transition.animateFloat(
        label = "logoPulse",
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.splash_logo),
            contentDescription = "Citadel Budget — Track. Plan. Grow.",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .alpha(glow)
        )
    }
}

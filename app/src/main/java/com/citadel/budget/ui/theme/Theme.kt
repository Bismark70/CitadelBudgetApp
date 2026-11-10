package com.citadel.budget.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.citadel.budget.data.ThemeMode

private val Navy = Color(0xFF0B1F3A)
private val Blue = Color(0xFF1D4ED8)
private val BlueLight = Color(0xFF5B8DEF)
private val Teal = Color(0xFF0D9488)
private val TealLight = Color(0xFF2DD4BF)
private val Gold = Color(0xFFD97706)
private val GoldLight = Color(0xFFFBBF24)

private val LightColors = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF0B1F3A),
    secondary = Teal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF042F2E),
    tertiary = Gold,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF451A03),
    background = Color(0xFFF4F7FB),
    onBackground = Color(0xFF10192B),
    surface = Color.White,
    onSurface = Color(0xFF10192B),
    surfaceVariant = Color(0xFFE7ECF5),
    onSurfaceVariant = Color(0xFF4A5568),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    outline = Color(0xFFC4CEDD)
)

private val DarkColors = darkColorScheme(
    primary = BlueLight,
    onPrimary = Color(0xFF0B1F3A),
    primaryContainer = Color(0xFF173365),
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = TealLight,
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF0F4C47),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = GoldLight,
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF5C3D0A),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFE6EBF5),
    surface = Color(0xFF111C30),
    onSurface = Color(0xFFE6EBF5),
    surfaceVariant = Color(0xFF1B2A44),
    onSurfaceVariant = Color(0xFF9AA8C2),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    outline = Color(0xFF33415C)
)

object CitadelColors {
    /** Gradient drawn from the Citadel Budget logo: navy -> blue -> teal. */
    val heroGradient = listOf(Navy, Blue, Teal)
    val gold = GoldLight
    val teal = TealLight
    val navy = Navy
}

fun scoreColor(score: Int, dark: Boolean): Color = when {
    score < 40 -> Color(0xFFDC2626)
    score < 60 -> if (dark) GoldLight else Gold
    score < 75 -> if (dark) BlueLight else Blue
    else -> if (dark) TealLight else Teal
}

@Composable
fun CitadelTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}

package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MatchoraColorScheme = darkColorScheme(
    primary = TurquoisePrimary,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF82F4FF),
    secondary = TurquoiseSecondary,
    onSecondary = Color(0xFF00382E),
    secondaryContainer = Color(0xFF005144),
    onSecondaryContainer = Color(0xFF66FCD3),
    tertiary = NeonLiveRed,
    onTertiary = Color.White,
    background = NavyBackground,
    onBackground = TextPrimary,
    surface = NavySurface,
    onSurface = TextPrimary,
    surfaceVariant = NavyCard,
    onSurfaceVariant = TextSecondary,
    outline = NavyBorder,
    outlineVariant = Color(0xFF18233C)
)

@Composable
fun MatchoraTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MatchoraColorScheme,
        typography = Typography,
        content = content
    )
}

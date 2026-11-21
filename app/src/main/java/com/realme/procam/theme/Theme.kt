package com.realme.procam.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = RealmeGold,
    secondary = AccentCyan,
    tertiary = FocusGreen,
    background = Black,
    surface = OledSurface,
    onPrimary = Black,
    onSecondary = Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun RealmeProCamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

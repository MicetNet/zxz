package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkMinColorScheme = darkColorScheme(
    primary = MintAccent,
    onPrimary = MidnightBg,
    primaryContainer = MidnightCard,
    onPrimaryContainer = MintSecondary,
    secondary = MintSecondary,
    onSecondary = MidnightBg,
    background = MidnightBg,
    onBackground = TextPrimary,
    surface = MidnightSurface,
    onSurface = TextPrimary,
    surfaceVariant = MidnightCard,
    onSurfaceVariant = TextSecondary,
    outline = BorderDark,
    error = ErrorRed
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkMinColorScheme,
        typography = Typography,
        content = content
    )
}

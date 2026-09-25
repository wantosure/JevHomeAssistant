package com.jev.assistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CyanAccent,
    secondary = BlueNeon,
    tertiary = EmeraldAccent,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = DarkBackground,
    onSecondary = DarkBackground,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun JevHomeAssistantTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

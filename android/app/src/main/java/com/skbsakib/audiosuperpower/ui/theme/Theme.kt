package com.skbsakib.audiosuperpower.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun SkbTheme(
    accent: AccentPalette = AccentPalette.CYAN,
    content: @Composable () -> Unit
) {
    val scheme = darkColorScheme(
        primary = accent.primary,
        onPrimary = Color(0xFF001318),
        secondary = Color(0xFF7A8FA6),
        background = Color(0xFF050810),
        onBackground = Color(0xFFE0F7FA),
        surface = Color(0xFF0A1220),
        onSurface = Color(0xFFE0F7FA)
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

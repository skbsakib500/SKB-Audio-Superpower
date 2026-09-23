package com.skbsakib.audiosuperpower.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * CompositionLocal for the current variant — screens can read background
 * gradient colors without threading them through every composable.
 */
val LocalThemeVariant = staticCompositionLocalOf { ThemeVariant.DARK }

@Composable
fun SkbTheme(
    accent: AccentPalette = AccentPalette.CYAN,
    variant: ThemeVariant = ThemeVariant.DARK,
    content: @Composable () -> Unit
) {
    val scheme = darkColorScheme(
        primary = accent.primary,
        onPrimary = Color(0xFF001318),
        secondary = variant.onBackground.copy(alpha = 0.6f),
        background = variant.background,
        onBackground = variant.onBackground,
        surface = variant.surface,
        onSurface = variant.onSurface,
        surfaceVariant = variant.surfaceElevated,
        onSurfaceVariant = variant.onBackground.copy(alpha = 0.8f)
    )
    CompositionLocalProvider(LocalThemeVariant provides variant) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

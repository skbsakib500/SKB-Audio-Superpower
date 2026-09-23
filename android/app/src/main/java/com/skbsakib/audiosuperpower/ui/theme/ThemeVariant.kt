package com.skbsakib.audiosuperpower.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Background / surface theme variants layered under the accent palette.
 * Accent color remains independent — user picks accent (cyan, magenta, ...)
 * AND variant (dark, midnight, OLED, cyber).
 */
enum class ThemeVariant(
    val id: String,
    val label: String,
    val background: Color,
    val backgroundAlt: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val onBackground: Color,
    val onSurface: Color
) {
    DARK(
        id = "dark",
        label = "DARK",
        background = Color(0xFF050810),
        backgroundAlt = Color(0xFF0A1220),
        surface = Color(0xFF0A1220),
        surfaceElevated = Color(0xFF0E1826),
        onBackground = Color(0xFFE0F7FA),
        onSurface = Color(0xFFE0F7FA)
    ),
    MIDNIGHT(
        id = "midnight",
        label = "MIDNIGHT",
        background = Color(0xFF020510),
        backgroundAlt = Color(0xFF061228),
        surface = Color(0xFF061228),
        surfaceElevated = Color(0xFF0A1A34),
        onBackground = Color(0xFFD8E8F5),
        onSurface = Color(0xFFD8E8F5)
    ),
    OLED(
        id = "oled",
        label = "OLED",
        background = Color(0xFF000000),
        backgroundAlt = Color(0xFF050505),
        surface = Color(0xFF050505),
        surfaceElevated = Color(0xFF0A0A0A),
        onBackground = Color(0xFFFFFFFF),
        onSurface = Color(0xFFE8E8E8)
    ),
    CYBER(
        id = "cyber",
        label = "CYBER",
        background = Color(0xFF0A0210),
        backgroundAlt = Color(0xFF160618),
        surface = Color(0xFF160618),
        surfaceElevated = Color(0xFF1F0A26),
        onBackground = Color(0xFFF0E6F8),
        onSurface = Color(0xFFF0E6F8)
    );

    companion object {
        fun from(id: String): ThemeVariant =
            entries.firstOrNull { it.id == id } ?: DARK
    }
}

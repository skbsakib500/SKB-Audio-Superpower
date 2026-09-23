package com.skbsakib.audiosuperpower.ui.theme

import androidx.compose.ui.graphics.Color

enum class AccentPalette(val id: String, val primary: Color, val glow: Color) {
    CYAN("cyan", Color(0xFF00E5FF), Color(0x2200E5FF)),
    MAGENTA("magenta", Color(0xFFFF2BD6), Color(0x22FF2BD6)),
    LIME("lime", Color(0xFFB4FF39), Color(0x22B4FF39)),
    AMBER("amber", Color(0xFFFFB300), Color(0x22FFB300)),
    VIOLET("violet", Color(0xFF9B6CFF), Color(0x229B6CFF));

    companion object {
        fun from(id: String): AccentPalette =
            entries.firstOrNull { it.id == id } ?: CYAN
    }
}

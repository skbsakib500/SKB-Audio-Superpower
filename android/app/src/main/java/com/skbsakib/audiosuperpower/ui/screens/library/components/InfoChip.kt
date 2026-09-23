package com.skbsakib.audiosuperpower.ui.screens.library.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

@Composable
fun InfoChip(label: String, value: String) {
    Column {
        Text(label, color = Color(0xFF4A6272), fontSize = 9.sp,
            letterSpacing = 2.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp,
            fontFamily = FontFamily.Monospace)
    }
}

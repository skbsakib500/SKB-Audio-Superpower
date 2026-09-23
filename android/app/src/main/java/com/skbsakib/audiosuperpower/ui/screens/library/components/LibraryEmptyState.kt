package com.skbsakib.audiosuperpower.ui.screens.library.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LibraryEmptyState(
    text: String,
    hint: String? = null,
    showSpinner: Boolean = false
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (showSpinner) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(14.dp))
            } else {
                Icon(Icons.Filled.MusicNote, null, tint = Color(0xFF3D5266),
                    modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(12.dp))
            }
            Text(text, color = Color(0xFF7A8FA6),
                fontSize = 12.sp, letterSpacing = 4.sp, fontFamily = FontFamily.Monospace)
            if (hint != null) {
                Spacer(Modifier.height(4.dp))
                Text(hint, color = Color(0xFF3D5266), fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace)
            }
        }
    }
}

package com.skbsakib.audiosuperpower.ui.shell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Global top bar. Title + subtitle + settings action. Consistent across
 * primary screens so navigation feels predictable.
 */
@Composable
fun SkbTopBar(
    title: String,
    subtitle: String,
    onOpenSettings: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("SKB AUDIO",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp, letterSpacing = 4.sp,
                fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(2.dp))
            Text(title,
                color = Color.White, fontSize = 22.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 3.sp,
                fontFamily = FontFamily.Monospace)
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(1.dp))
                Text(subtitle,
                    color = Color(0xFF7A8FA6),
                    fontSize = 10.sp, letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace)
            }
        }
        Icon(
            Icons.Filled.Settings, "Settings",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(24.dp)
                .clickable { onOpenSettings() }
        )
    }
}

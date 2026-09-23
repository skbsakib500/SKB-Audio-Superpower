package com.skbsakib.audiosuperpower.ui.screens.library.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LibraryTabChip(
    label: String,
    icon: ImageVector,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else Color(0x1100E5FF),
        shape = RoundedCornerShape(20.dp),
        border = if (active) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null,
                tint = if (active) MaterialTheme.colorScheme.primary else Color(0xFF7A8FA6),
                modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(label,
                color = if (active) MaterialTheme.colorScheme.primary else Color(0xFF7A8FA6),
                fontSize = 9.sp, letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
        }
    }
}

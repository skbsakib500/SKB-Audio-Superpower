package com.skbsakib.audiosuperpower.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.skbsakib.audiosuperpower.ui.navigation.AppRoute

/**
 * Bottom navigation with 5 primary items:
 *   HOME · LIBRARY · PLAYER · DSP · LAB
 *
 * PLAYER is special — tapping it opens the overlay instead of navigating.
 */
@Composable
fun SkbBottomBar(
    currentRoute: String?,
    onNavigate: (AppRoute) -> Unit,
    onOpenPlayer: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A1220))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomItem(
            label = "HOME",
            icon = Icons.Filled.Home,
            active = currentRoute == AppRoute.Home.route,
            onClick = { onNavigate(AppRoute.Home) },
            modifier = Modifier.weight(1f)
        )
        BottomItem(
            label = "LIBRARY",
            icon = Icons.Filled.LibraryMusic,
            active = currentRoute == AppRoute.Library.route,
            onClick = { onNavigate(AppRoute.Library) },
            modifier = Modifier.weight(1f)
        )
        BottomItem(
            label = "PLAYER",
            icon = Icons.Filled.PlayArrow,
            active = false,   // overlay — never "active" in nav sense
            onClick = onOpenPlayer,
            modifier = Modifier.weight(1f)
        )
        BottomItem(
            label = "DSP",
            icon = Icons.Filled.Equalizer,
            active = currentRoute == AppRoute.DspLab.route,
            onClick = { onNavigate(AppRoute.DspLab) },
            modifier = Modifier.weight(1f)
        )
        BottomItem(
            label = "LAB",
            icon = Icons.Filled.Psychology,
            active = currentRoute == AppRoute.AudioLab.route,
            onClick = { onNavigate(AppRoute.AudioLab) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BottomItem(
    label: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (active) MaterialTheme.colorScheme.primary else Color(0xFF7A8FA6)
    Column(
        modifier
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(3.dp))
        Text(label,
            color = color, fontSize = 9.sp,
            letterSpacing = 1.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            fontFamily = FontFamily.Monospace)
    }
}

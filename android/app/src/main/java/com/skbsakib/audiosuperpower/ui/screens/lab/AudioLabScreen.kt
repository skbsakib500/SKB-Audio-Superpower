package com.skbsakib.audiosuperpower.ui.screens.lab

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skbsakib.audiosuperpower.ui.theme.LocalThemeVariant

/**
 * Audio Lab — aggregator for all advanced audio screens.
 * Phase 3 may split these into separate bottom-nav roots.
 */
@Composable
fun AudioLabScreen(
    onOpenAutoEq: () -> Unit,
    onOpenSpatial: () -> Unit,
    onOpenAnalyzer: () -> Unit,
    onOpenDeviceLab: () -> Unit,
    onOpenGlobalEffect: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val variant = LocalThemeVariant.current

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(
                listOf(variant.background, variant.backgroundAlt)
            ))
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("SKB AUDIO",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp, letterSpacing = 4.sp,
                    fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(2.dp))
                Text("AUDIO LAB",
                    color = Color.White, fontSize = 22.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 3.sp,
                    fontFamily = FontFamily.Monospace)
            }
            Icon(Icons.Filled.Settings, "Settings",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp).clickable { onOpenSettings() })
        }

        Spacer(Modifier.height(8.dp))

        LabItem("AutoEQ", "per-headphone correction", Icons.Filled.Headphones, onOpenAutoEq)
        LabItem("Spatial", "3D → 10D virtual stage", Icons.Filled.SurroundSound, onOpenSpatial)
        LabItem("Analyzer", "real-time spectrum · meters", Icons.Filled.GraphicEq, onOpenAnalyzer)
        LabItem("Device Lab", "chipset · capability probe", Icons.Filled.PhoneAndroid, onOpenDeviceLab)
        LabItem("Global Effect", "session 0 · best-effort", Icons.Filled.Public, onOpenGlobalEffect)

        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun LabItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0x1A00E5FF),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable { onClick() }
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White,
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = Color(0xFF7A8FA6),
                    fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

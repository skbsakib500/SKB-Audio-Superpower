package com.skbsakib.audiosuperpower.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skbsakib.audiosuperpower.global.GlobalEffectController
import com.skbsakib.audiosuperpower.global.GlobalEffectController.Status

@Composable
fun GlobalEffectScreen(onClose: () -> Unit) {
    var state by remember { mutableStateOf(GlobalEffectController.current()) }

    // Auto-probe on first open
    LaunchedEffect(Unit) {
        if (!state.probed) {
            state = GlobalEffectController.probe()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF050810), Color(0xFF0A1220))))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp).clickable { onClose() })
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("GLOBAL EFFECT",
                    color = Color.White, fontSize = 20.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 4.sp,
                    fontFamily = FontFamily.Monospace)
                Text("SYSTEM-WIDE · BEST-EFFORT",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 9.sp, letterSpacing = 3.sp, fontFamily = FontFamily.Monospace)
            }
            Icon(Icons.Filled.Refresh, "Re-probe",
                tint = Color(0xFF7A8FA6),
                modifier = Modifier.size(22.dp).clickable {
                    state = GlobalEffectController.probe()
                })
        }

        Spacer(Modifier.height(20.dp))

        // Honest explanation
        Surface(
            color = Color(0x33FFB300),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0x66FFB300)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("HONEST SCOPE",
                    color = Color(0xFFFFB300), fontSize = 10.sp,
                    letterSpacing = 3.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Android's AudioEffect API is per-session. Session 0 is a hint " +
                    "that vendor implementations MAY treat as \"apply to all audio\". " +
                    "Many devices silently ignore it. This screen reports what your " +
                    "device ACTUALLY did — nothing is claimed that we didn't verify.",
                    color = Color(0xFFB0C2D0), fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace, lineHeight = 16.sp
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        if (!state.probed) {
            Box(
                Modifier.fillMaxWidth().height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            SectionLabel("PROBED EFFECTS")
            Spacer(Modifier.height(10.dp))

            state.effects.forEach { e ->
                EffectRow(e.name, e.status, e.detail)
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(20.dp))

            // Fallback info
            SectionLabel("FALLBACK")
            Spacer(Modifier.height(8.dp))
            Surface(
                color = Color(0x1A00E5FF),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("OUR PLAYBACK PATH",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp, letterSpacing = 3.sp,
                        fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "DSP / Spatial / AutoEQ / ReplayGain apply to all audio " +
                        "played THROUGH this app — regardless of what this screen " +
                        "reports. That path is guaranteed on every device.",
                        color = Color(0xFFB0C2D0), fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace, lineHeight = 15.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(30.dp))
        Text("engine · skb-core global-effect\ncreator · SKB Sakib",
            color = Color(0xFF3D5266), fontSize = 10.sp,
            letterSpacing = 2.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun EffectRow(name: String, status: Status, detail: String) {
    val (color, label) = when (status) {
        Status.SUPPORTED   -> Color(0xFFB4FF39) to "SUPPORTED"
        Status.LIMITED     -> Color(0xFFFFB300) to "LIMITED"
        Status.UNSUPPORTED -> Color(0xFFFF4D4D) to "UNSUPPORTED"
        Status.UNKNOWN     -> Color(0xFF7A8FA6) to "UNKNOWN"
    }
    Surface(
        color = Color(0x0D00E5FF),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, color = Color.White,
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace)
                if (detail.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(detail, color = Color(0xFF7A8FA6),
                        fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }
            Text(label, color = color,
                fontSize = 10.sp, letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = Color(0xFF4A6272), fontSize = 10.sp,
        letterSpacing = 4.sp, fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Monospace)
}

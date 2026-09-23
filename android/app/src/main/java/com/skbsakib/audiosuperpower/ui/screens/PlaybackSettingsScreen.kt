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
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skbsakib.audiosuperpower.playback.CrossfadeController
import com.skbsakib.audiosuperpower.playback.SleepTimer

@Composable
fun PlaybackSettingsScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val crossfadeMs by CrossfadeController.ms.collectAsStateWithLifecycle()
    val sleepRemaining by SleepTimer.remainingMs.collectAsStateWithLifecycle()
    val sleepActive by SleepTimer.active.collectAsStateWithLifecycle()

    // Live countdown label refresh
    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(sleepActive) {
        while (sleepActive) {
            tick = System.currentTimeMillis()
            kotlinx.coroutines.delay(500L)
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
                Text("PLAYBACK",
                    color = Color.White, fontSize = 20.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 4.sp,
                    fontFamily = FontFamily.Monospace)
                Text("CROSSFADE · SLEEP · TONE",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 9.sp, letterSpacing = 3.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Crossfade ──
        SectionLabel("CROSSFADE")
        Spacer(Modifier.height(6.dp))
        Text(
            if (crossfadeMs == 0) "OFF · hard cut (recommended for gapless albums)"
            else "$crossfadeMs ms equal-power blend",
            color = Color(0xFF7A8FA6), fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.height(6.dp))
        Slider(
            value = crossfadeMs.toFloat(),
            onValueChange = { CrossfadeController.set(ctx, it.toInt()) },
            valueRange = 0f..12000f,
            steps = 23,   // 500ms steps
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color(0x2200E5FF)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("OFF", color = Color(0xFF3D5266), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text("12 s", color = Color(0xFF3D5266), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }

        Spacer(Modifier.height(28.dp))

        // ── Sleep Timer ──
        SectionLabel("SLEEP TIMER")
        Spacer(Modifier.height(10.dp))

        if (sleepActive) {
            Surface(
                color = Color(0x1A00E5FF),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.NightlightRound, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("SLEEPING IN",
                            color = Color(0xFF7A8FA6), fontSize = 9.sp,
                            letterSpacing = 3.sp, fontFamily = FontFamily.Monospace)
                        Text(SleepTimer.formatted(),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace)
                    }
                    Text("CANCEL",
                        color = Color(0xFFFF4D4D),
                        fontSize = 11.sp, letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.clickable { SleepTimer.stop() }
                            .padding(8.dp))
                }
            }
        } else {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(15, 30, 45, 60, 90).forEach { min ->
                    Surface(
                        color = Color(0x1A00E5FF),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f).clickable {
                            SleepTimer.start(min)
                        }
                    ) {
                        Box(
                            Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${min}m",
                                color = Color(0xFFB0C2D0),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // ── ReplayGain (placeholder) ──
        SectionLabel("VOLUME NORMALIZATION")
        Spacer(Modifier.height(10.dp))
        Surface(color = Color(0x1A00E5FF), shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("REPLAYGAIN",
                        color = Color(0xFFB0C2D0), fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(2.dp))
                    Text("coming in Phase 10 · per-track loudness normalization",
                        color = Color(0xFF7A8FA6), fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace)
                }
                Text("PLANNED",
                    color = Color(0xFF3D5266), fontSize = 9.sp,
                    letterSpacing = 2.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(Modifier.height(30.dp))
        Text("engine · skb-core playback\ncreator · SKB Sakib",
            color = Color(0xFF3D5266), fontSize = 10.sp,
            letterSpacing = 2.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = Color(0xFF4A6272), fontSize = 10.sp,
        letterSpacing = 4.sp, fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Monospace)
}

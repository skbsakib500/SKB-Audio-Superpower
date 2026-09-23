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
import com.skbsakib.audiosuperpower.playback.ReplayGainController
import com.skbsakib.audiosuperpower.playback.RgMethod
import com.skbsakib.audiosuperpower.playback.RgMode
import com.skbsakib.audiosuperpower.playback.SleepTimer
import com.skbsakib.audiosuperpower.playback.SmartQueueController
import com.skbsakib.audiosuperpower.playback.SmartQueueMode

@Composable
fun PlaybackSettingsScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val crossfadeMs by CrossfadeController.ms.collectAsStateWithLifecycle()
    val rgState by ReplayGainController.state.collectAsStateWithLifecycle()
    val sqState by SmartQueueController.state.collectAsStateWithLifecycle()
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

        // ── ReplayGain ──
        SectionLabel("VOLUME NORMALIZATION · REPLAYGAIN")
        Spacer(Modifier.height(10.dp))

        Surface(color = Color(0x1A00E5FF), shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("REPLAYGAIN",
                        color = if (rgState.enabled) MaterialTheme.colorScheme.primary
                                else Color(0xFFB0C2D0),
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(2.dp))
                    Text("peak-based per-track normalization",
                        color = Color(0xFF7A8FA6), fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace)
                }
                Switch(
                    checked = rgState.enabled,
                    onCheckedChange = { ReplayGainController.setEnabled(ctx, it) }
                )
            }
        }

        if (rgState.enabled) {
            Spacer(Modifier.height(10.dp))

            // Gain computation method toggle
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RgMethod.entries.forEach { method ->
                    val active = rgState.method == method
                    Surface(
                        color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else Color(0x1A00E5FF),
                        shape = RoundedCornerShape(20.dp),
                        border = if (active) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.weight(1f).clickable {
                            ReplayGainController.setMethod(ctx, method)
                        }
                    ) {
                        Box(
                            Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (method == RgMethod.PEAK) "PEAK" else "LUFS · BS.1770",
                                color = if (active) MaterialTheme.colorScheme.primary else Color(0xFF7A8FA6),
                                fontSize = 10.sp, letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Mode (track/album)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RgMode.entries.forEach { mode ->
                    val active = rgState.mode == mode
                    Surface(
                        color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else Color(0x1A00E5FF),
                        shape = RoundedCornerShape(20.dp),
                        border = if (active) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.weight(1f).clickable {
                            ReplayGainController.setMode(ctx, mode)
                        }
                    ) {
                        Box(
                            Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(mode.name,
                                color = if (active) MaterialTheme.colorScheme.primary else Color(0xFF7A8FA6),
                                fontSize = 10.sp, letterSpacing = 2.sp,
                                fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (rgState.method == RgMethod.PEAK) {
                Text("TARGET PEAK: %+.1f dBFS".format(rgState.targetDb),
                    color = Color(0xFF7A8FA6), fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace)
                Slider(
                    value = rgState.targetDb,
                    onValueChange = { ReplayGainController.setTargetDb(ctx, it) },
                    valueRange = -12f..0f,
                    steps = 23,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color(0x2200E5FF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text("TARGET LOUDNESS: %.1f LUFS".format(rgState.targetLufs),
                    color = Color(0xFF7A8FA6), fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace)
                Slider(
                    value = rgState.targetLufs,
                    onValueChange = { ReplayGainController.setTargetLufs(ctx, it) },
                    valueRange = -30f..-5f,
                    steps = 24,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color(0x2200E5FF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Spotify/YouTube ≈ -14 LUFS · EBU R128 ≈ -23 LUFS",
                    color = Color(0xFF3D5266), fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace)
            }

            // Preamp slider
            Text("PREAMP: %+.1f dB".format(rgState.preampDb),
                color = Color(0xFF7A8FA6), fontSize = 10.sp,
                fontFamily = FontFamily.Monospace)
            Slider(
                value = rgState.preampDb,
                onValueChange = { ReplayGainController.setPreampDb(ctx, it) },
                valueRange = -6f..6f,
                steps = 23,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color(0x2200E5FF)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Live readout
            Surface(color = Color(0x0D00E5FF), shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            if (rgState.method == RgMethod.PEAK) "MEASURED PEAK" else "MEASURED LUFS",
                            color = Color(0xFF4A6272), fontSize = 9.sp,
                            letterSpacing = 2.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            if (rgState.method == RgMethod.PEAK) "%+.1f dB".format(rgState.lastMeasuredPeakDb)
                            else "%.1f LUFS".format(rgState.lastMeasuredLufs),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                    Column {
                        Text("APPLIED GAIN",
                            color = Color(0xFF4A6272), fontSize = 9.sp,
                            letterSpacing = 2.sp, fontFamily = FontFamily.Monospace)
                        Text("%+.1f dB".format(rgState.lastAppliedGainDb),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        SectionLabel("SMART QUEUE")
        Spacer(Modifier.height(10.dp))
        Surface(color = Color(0x1A00E5FF), shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("AUTO-DJ",
                        color = if (sqState.mode != SmartQueueMode.OFF)
                            MaterialTheme.colorScheme.primary else Color(0xFFB0C2D0),
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(2.dp))
                    Text("BPM + energy aware next-track selection",
                        color = Color(0xFF7A8FA6), fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace)
                }
                Text(
                    when (sqState.mode) {
                        SmartQueueMode.OFF -> "OFF"
                        SmartQueueMode.TEMPO -> "TEMPO"
                        SmartQueueMode.ENERGY -> "ENERGY"
                        SmartQueueMode.MIXED -> "MIXED"
                    },
                    color = Color(0xFF3D5266), fontSize = 9.sp,
                    letterSpacing = 2.sp, fontFamily = FontFamily.Monospace)
            }
        }
        Spacer(Modifier.height(10.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SmartQueueMode.entries.forEach { mode ->
                val active = sqState.mode == mode
                Surface(
                    color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else Color(0x1A00E5FF),
                    shape = RoundedCornerShape(16.dp),
                    border = if (active) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier.weight(1f).clickable {
                        SmartQueueController.setMode(ctx, mode)
                    }
                ) {
                    Box(
                        Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(mode.name,
                            color = if (active) MaterialTheme.colorScheme.primary else Color(0xFF7A8FA6),
                            fontSize = 9.sp, letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("first analysis on play · cached afterwards",
            color = Color(0xFF3D5266), fontSize = 9.sp,
            fontFamily = FontFamily.Monospace)

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

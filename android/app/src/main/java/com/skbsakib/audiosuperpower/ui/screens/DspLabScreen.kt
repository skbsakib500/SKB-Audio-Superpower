package com.skbsakib.audiosuperpower.ui.screens

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skbsakib.audiosuperpower.dsp.DspController

@Composable
fun DspLabScreen(onClose: () -> Unit) {
    val dsp by DspController.state.collectAsStateWithLifecycle()

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
                Text("DSP LAB",
                    color = Color.White, fontSize = 20.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 4.sp,
                    fontFamily = FontFamily.Monospace)
                Text("2080 AUDIO ENGINE",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 9.sp, letterSpacing = 3.sp, fontFamily = FontFamily.Monospace)
            }
            Icon(Icons.Filled.Refresh, "Reset",
                tint = Color(0xFF7A8FA6),
                modifier = Modifier.size(22.dp).clickable { DspController.reset() })
        }

        Spacer(Modifier.height(20.dp))

        // Master toggle
        Surface(color = Color(0x1A00E5FF), shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("DSP ENGINE",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp, letterSpacing = 3.sp, fontFamily = FontFamily.Monospace)
                    Text(if (dsp.enabled) "ACTIVE" else "BYPASSED",
                        color = if (dsp.enabled) Color.White else Color(0xFF7A8FA6),
                        fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace)
                }
                Switch(
                    checked = dsp.enabled,
                    onCheckedChange = { DspController.setEnabled(it) }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Presets
        SectionLabel("PRESETS")
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DspController.presets.forEach { p ->
                val active = dsp.activePreset == p.name
                Surface(
                    color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else Color(0x1A00E5FF),
                    shape = RoundedCornerShape(20.dp),
                    border = if (active) androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier.clickable { DspController.applyPreset(p.name) }
                ) {
                    Text(p.name,
                        color = if (active) MaterialTheme.colorScheme.primary else Color(0xFF7A8FA6),
                        fontSize = 10.sp, letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Preamp
        SectionLabel("PREAMP")
        Spacer(Modifier.height(6.dp))
        DspSlider(
            value = dsp.preampDb,
            range = -12f..12f,
            onChange = { DspController.setPreamp(it) }
        )

        Spacer(Modifier.height(20.dp))

        // 5-band EQ
        SectionLabel("PARAMETRIC EQ · 5 BAND")
        Spacer(Modifier.height(6.dp))
        dsp.bandGainsDb.forEachIndexed { i, gain ->
            DspSlider(
                label = "${DspController.BAND_FREQS[i]} Hz",
                value = gain,
                range = -12f..12f,
                onChange = { DspController.setBand(i, it) }
            )
        }

        Spacer(Modifier.height(20.dp))

        // Bass / Treble
        SectionLabel("TONE")
        Spacer(Modifier.height(6.dp))
        DspSlider(label = "BASS (100 Hz)", value = dsp.bassDb,
            range = -12f..12f, onChange = { DspController.setBass(it) })
        DspSlider(label = "TREBLE (8 kHz)", value = dsp.trebleDb,
            range = -12f..12f, onChange = { DspController.setTreble(it) })

        Spacer(Modifier.height(20.dp))

        // Limiter
        SectionLabel("LIMITER")
        Spacer(Modifier.height(10.dp))
        Surface(color = Color(0x1A00E5FF), shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("CEILING",
                    color = Color(0xFF7A8FA6), fontSize = 10.sp,
                    letterSpacing = 3.sp, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f))
                Text("%.1f dB".format(dsp.limiterCeilingDb),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = dsp.limiterEnabled,
                    onCheckedChange = { DspController.setLimiterEnabled(it) }
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        DspSlider(
            label = "",
            value = dsp.limiterCeilingDb,
            range = -6f..0f,
            onChange = { DspController.setLimiterCeiling(it) }
        )

        Spacer(Modifier.height(30.dp))
        Text("engine · skb-core dsp-chain\ncreator · SKB Sakib",
            color = Color(0xFF3D5266), fontSize = 10.sp,
            letterSpacing = 2.sp, fontFamily = FontFamily.Monospace)

        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = Color(0xFF4A6272), fontSize = 10.sp,
        letterSpacing = 4.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
}

@Composable
private fun DspSlider(
    label: String = "",
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Color(0xFF7A8FA6), fontSize = 10.sp,
                fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
            Text("%+.1f".format(value),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color(0x2200E5FF)
            ),
            modifier = Modifier.fillMaxWidth().height(30.dp)
        )
    }
}

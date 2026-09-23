package com.skbsakib.audiosuperpower.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import com.skbsakib.audiosuperpower.spatial.RoomType
import com.skbsakib.audiosuperpower.spatial.SpatialController
import com.skbsakib.audiosuperpower.spatial.SpatialMode

@Composable
fun SpatialLabScreen(onClose: () -> Unit) {
    val st by SpatialController.state.collectAsStateWithLifecycle()

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
                Text("SPATIAL LAB",
                    color = Color.White, fontSize = 20.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 4.sp,
                    fontFamily = FontFamily.Monospace)
                Text("3D · 4D · ... · 10D",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 9.sp, letterSpacing = 3.sp, fontFamily = FontFamily.Monospace)
            }
            Icon(Icons.Filled.Refresh, "Reset",
                tint = Color(0xFF7A8FA6),
                modifier = Modifier.size(22.dp).clickable { SpatialController.reset() })
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
                    Text("SPATIAL ENGINE",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp, letterSpacing = 3.sp, fontFamily = FontFamily.Monospace)
                    Text(if (st.enabled) st.mode.label else "BYPASSED",
                        color = if (st.enabled) Color.White else Color(0xFF7A8FA6),
                        fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace)
                }
                Switch(
                    checked = st.enabled,
                    onCheckedChange = { SpatialController.setEnabled(it) }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Mode picker (grid of chips)
        SectionLabel("MODE · 3D → 10D")
        Spacer(Modifier.height(10.dp))
        SpatialMode.entries.forEach { m ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val active = st.mode == m
                Surface(
                    color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else Color(0x1A00E5FF),
                    shape = RoundedCornerShape(12.dp),
                    border = if (active) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier.clickable { SpatialController.setMode(m) }
                ) {
                    Box(
                        Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            .width(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(m.label,
                            color = if (active) MaterialTheme.colorScheme.primary else Color(0xFF7A8FA6),
                            fontSize = 12.sp, letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Text(m.desc,
                    color = if (active) Color.White else Color(0xFF7A8FA6),
                    fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(20.dp))

        // Intensity
        SectionLabel("INTENSITY")
        Spacer(Modifier.height(6.dp))
        SpatialSlider(
            label = "AMOUNT",
            value = st.intensity,
            range = 0f..1f,
            display = "%.0f%%".format(st.intensity * 100),
            onChange = { SpatialController.setIntensity(it) }
        )

        Spacer(Modifier.height(20.dp))

        // Height
        SectionLabel("VERTICAL HEIGHT")
        Spacer(Modifier.height(6.dp))
        SpatialSlider(
            label = "ELEVATION",
            value = st.height,
            range = 0f..1f,
            display = "%.0f%%".format(st.height * 100),
            onChange = { SpatialController.setHeight(it) }
        )
        Spacer(Modifier.height(4.dp))
        Text("approximate HRTF — pinna notch + spectral tilt",
            color = Color(0xFF3D5266), fontSize = 9.sp,
            fontFamily = FontFamily.Monospace)

        Spacer(Modifier.height(20.dp))

        // Room
        SectionLabel("ROOM ACOUSTICS")
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RoomType.entries.forEach { r ->
                val active = st.room == r
                Surface(
                    color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else Color(0x1A00E5FF),
                    shape = RoundedCornerShape(20.dp),
                    border = if (active) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier.clickable { SpatialController.setRoom(r) }
                ) {
                    Text(r.label,
                        color = if (active) MaterialTheme.colorScheme.primary else Color(0xFF7A8FA6),
                        fontSize = 10.sp, letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                }
            }
        }

        Spacer(Modifier.height(30.dp))
        Text("engine · skb-core spatial\n3D-10D are product modes, not physical dimensions\ncreator · SKB Sakib",
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
private fun SpatialSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: String,
    onChange: (Float) -> Unit
) {
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Color(0xFF7A8FA6), fontSize = 10.sp,
                fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
            Text(display,
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

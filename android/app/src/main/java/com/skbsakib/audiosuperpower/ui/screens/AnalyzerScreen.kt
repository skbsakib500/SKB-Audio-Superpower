package com.skbsakib.audiosuperpower.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skbsakib.audiosuperpower.analyzer.AnalyzerController

@Composable
fun AnalyzerScreen(onClose: () -> Unit) {
    val st by AnalyzerController.state.collectAsStateWithLifecycle()

    // Auto-enable on screen open; disable on close
    DisposableEffect(Unit) {
        AnalyzerController.setEnabled(true)
        onDispose { AnalyzerController.setEnabled(false) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF050810), Color(0xFF0A1220))))
            .padding(20.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp).clickable { onClose() })
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("ANALYZER",
                    color = Color.White, fontSize = 20.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 4.sp,
                    fontFamily = FontFamily.Monospace)
                Text("REALTIME SPECTRUM · METERS",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 9.sp, letterSpacing = 3.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(Modifier.height(20.dp))

        // Spectrum canvas
        Surface(
            color = Color(0x1100E5FF),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            SpectrumView(st.spectrum, st.bins)
        }

        Spacer(Modifier.height(18.dp))

        // Meters row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MeterCard(
                label = "PEAK",
                value = "%.2f".format(st.peak),
                secondary = if (st.peak >= 0.999f) "CLIP" else "OK",
                accent = if (st.peak >= 0.999f) Color(0xFFFF4D4D)
                         else MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            MeterCard(
                label = "RMS",
                value = "%.1f dB".format(st.rmsDb),
                secondary = "short-term",
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        MeterCard(
            label = "STATUS",
            value = if (st.clipping) "CLIPPING" else "CLEAN",
            secondary = "${st.bins} FFT bins · 1024-pt",
            accent = if (st.clipping) Color(0xFFFF4D4D) else Color(0xFFB4FF39),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        Text("Analyzer taps the DSP output stream.\nRMS dBFS, not full LUFS (K-weighting pending).\ncreator · SKB Sakib",
            color = Color(0xFF3D5266), fontSize = 9.sp,
            letterSpacing = 2.sp, fontFamily = FontFamily.Monospace,
            lineHeight = 14.sp)
    }
}

@Composable
private fun SpectrumView(spectrum: FloatArray, bins: Int) {
    Canvas(Modifier.fillMaxSize().padding(10.dp)) {
        val w = size.width
        val h = size.height
        val n = spectrum.size
        if (n <= 0) return@Canvas

        val barW = w / n.toFloat()

        // Build a filled path (bars as polygon)
        val path = Path()
        path.moveTo(0f, h)

        // Log-ish scaling: emphasize low frequencies perceptually
        for (i in 0 until n) {
            val m = spectrum[i].coerceIn(0f, 1f)
            // sqrt compression for visual dynamic range
            val amp = kotlin.math.sqrt(m)
            val x = i * barW
            val y = h - amp * h
            path.lineTo(x, y)
        }
        path.lineTo(w, h)
        path.close()

        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                listOf(
                    Color(0xFF00E5FF).copy(alpha = 0.85f),
                    Color(0xFF00E5FF).copy(alpha = 0.20f),
                    Color(0xFF00E5FF).copy(alpha = 0.05f)
                )
            )
        )

        // Outline
        drawPath(path = path, color = Color(0xFF00E5FF), style = Stroke(width = 1.5f))

        // Baseline
        drawLine(
            color = Color(0x3300E5FF),
            start = Offset(0f, h),
            end = Offset(w, h),
            strokeWidth = 1f
        )
    }
}

@Composable
private fun MeterCard(
    label: String,
    value: String,
    secondary: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0x1100E5FF),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(label, color = Color(0xFF4A6272),
                fontSize = 9.sp, letterSpacing = 3.sp,
                fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(4.dp))
            Text(value, color = accent,
                fontSize = 18.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(2.dp))
            Text(secondary, color = Color(0xFF7A8FA6),
                fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

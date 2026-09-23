package com.skbsakib.audiosuperpower.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
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
import com.skbsakib.audiosuperpower.device.DeviceProfileController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DeviceLabScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val st by DeviceProfileController.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        DeviceProfileController.refresh(ctx.applicationContext)
    }

    val snap = st.snapshot
    val tune = st.autoTune

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
                Text("DEVICE LAB",
                    color = Color.White, fontSize = 20.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 4.sp,
                    fontFamily = FontFamily.Monospace)
                Text("2080 CAPABILITY ENGINE",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 9.sp, letterSpacing = 3.sp, fontFamily = FontFamily.Monospace)
            }
            Icon(Icons.Filled.ContentCopy, "Copy",
                tint = Color(0xFF7A8FA6),
                modifier = Modifier.size(22.dp).clickable {
                    val text = buildString {
                        append("SKB AUDIO · DEVICE PROFILE\n")
                        append("==========================\n")
                        snap?.let { append(it.summary) }
                        append("\n\nAUTO-TUNE\n=========\n")
                        tune?.let {
                            append("Reason: ${it.reason}\n")
                            append("Preamp: ${it.preampDb} dB\n")
                            append("Bass:   ${it.bassDb} dB\n")
                            append("Treble: ${it.trebleDb} dB\n")
                            append("Limiter ceiling: ${it.limiterCeilingDb} dB\n")
                            append("Spatial mode: ${it.spatialModeId}\n")
                            append("Spatial intensity: ${it.spatialIntensity}\n")
                        }
                    }
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("SKB Device Profile", text))
                    Toast.makeText(ctx, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                })
            Spacer(Modifier.width(14.dp))
            Icon(Icons.Filled.Refresh, "Refresh",
                tint = Color(0xFF7A8FA6),
                modifier = Modifier.size(22.dp).clickable {
                    DeviceProfileController.refresh(ctx.applicationContext)
                })
        }

        Spacer(Modifier.height(20.dp))

        if (snap == null) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Text("DETECTING…",
                        color = Color(0xFF7A8FA6), fontSize = 11.sp,
                        letterSpacing = 4.sp, fontFamily = FontFamily.Monospace)
                }
            }
            return@Column
        }

        // Device card
        SectionLabel("DEVICE")
        Spacer(Modifier.height(8.dp))
        Card {
            KV("Model", "${snap.deviceManufacturer} ${snap.deviceModel}")
            KV("SoC", "${snap.socManufacturer} ${snap.socModel}")
            KV("Hardware", snap.hardware)
            KV("Board", snap.board)
            KV("ABI", snap.abi)
            KV("Android", "${snap.androidVersion} (API ${snap.apiLevel})")
        }

        Spacer(Modifier.height(18.dp))

        // Audio card
        SectionLabel("AUDIO HARDWARE")
        Spacer(Modifier.height(8.dp))
        Card {
            KV("Native sample rate", "${snap.nativeSampleRate} Hz")
            KV("Frames per buffer", snap.nativeFramesPerBuffer.toString())
            KV("Low latency", if (snap.lowLatencyCapable) "YES" else "NO")
            KV("Pro audio", if (snap.proAudioCapable) "YES" else "NO")
            KV("Output device", snap.outputDevice)
            KV("Max channels", if (snap.maxChannelCount > 0) snap.maxChannelCount.toString() else "—")
        }

        Spacer(Modifier.height(18.dp))

        // Auto-tune card
        SectionLabel("AUTO-TUNE")
        Spacer(Modifier.height(8.dp))
        Card {
            if (tune == null) {
                Text("no tune derived", color = Color(0xFF7A8FA6),
                    fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            } else {
                KV("DSP", if (tune.dspEnabled) "ON" else "OFF")
                KV("Preamp", "%+.1f dB".format(tune.preampDb))
                KV("Bass", "%+.1f dB".format(tune.bassDb))
                KV("Treble", "%+.1f dB".format(tune.trebleDb))
                KV("Limiter ceiling", "%.1f dB".format(tune.limiterCeilingDb))
                KV("Spatial mode", when (tune.spatialModeId) {
                    0 -> "OFF"
                    else -> "${tune.spatialModeId}D"
                })
                KV("Spatial intensity", "%.0f%%".format(tune.spatialIntensity * 100))
                Spacer(Modifier.height(6.dp))
                Text("REASON", color = Color(0xFF4A6272),
                    fontSize = 9.sp, letterSpacing = 3.sp, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(4.dp))
                Text(tune.reason, color = Color(0xFFB0C2D0),
                    fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(Modifier.height(14.dp))

        // Apply button
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().clickable {
                DeviceProfileController.reapply()
                Toast.makeText(ctx, "Auto-tune applied", Toast.LENGTH_SHORT).show()
            }
        ) {
            Box(
                Modifier.fillMaxWidth().padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("APPLY AUTO-TUNE",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp, letterSpacing = 3.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace)
            }
        }

        if (st.appliedAt > 0) {
            Spacer(Modifier.height(10.dp))
            val df = SimpleDateFormat("HH:mm:ss", Locale.US)
            Text("last applied · ${df.format(Date(st.appliedAt))}",
                color = Color(0xFF3D5266), fontSize = 10.sp,
                fontFamily = FontFamily.Monospace)
        }

        if (st.lastError != null) {
            Spacer(Modifier.height(10.dp))
            Text("error · ${st.lastError}",
                color = Color(0xFFFF8A8A), fontSize = 10.sp,
                fontFamily = FontFamily.Monospace)
        }

        Spacer(Modifier.height(28.dp))
        Text("engine · skb-core device-lab\ncreator · SKB Sakib",
            color = Color(0xFF3D5266), fontSize = 10.sp,
            letterSpacing = 2.sp, fontFamily = FontFamily.Monospace)

        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = Color(0x1100E5FF),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), content = content)
    }
}

@Composable
private fun KV(k: String, v: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(k, color = Color(0xFF7A8FA6), fontSize = 11.sp,
            fontFamily = FontFamily.Monospace)
        Text(v, color = Color.White, fontSize = 11.sp,
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = Color(0xFF4A6272), fontSize = 10.sp,
        letterSpacing = 4.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
}

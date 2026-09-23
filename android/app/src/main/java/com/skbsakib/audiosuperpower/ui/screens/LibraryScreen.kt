package com.skbsakib.audiosuperpower.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.skbsakib.audiosuperpower.library.MediaStoreScanner
import com.skbsakib.audiosuperpower.library.Track
import com.skbsakib.audiosuperpower.settings.SkbSettings

@Composable
fun LibraryScreen(
    settings: SkbSettings,
    onOpenSettings: () -> Unit
) {
    val ctx = LocalContext.current
    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loading = true
        tracks = MediaStoreScanner.scan(ctx.applicationContext)
        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF050810), Color(0xFF0A1220))))
    ) {
        // ── Header ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "SKB AUDIO · 2080 LAB",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp, letterSpacing = 3.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "LIBRARY",
                    color = Color.White, fontSize = 22.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 3.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onOpenSettings() }
            )
        }

        HorizontalDivider(color = Color(0x2200E5FF))

        // ── Status strip ──
        Surface(color = Color(0x1100E5FF), shape = RoundedCornerShape(0.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                InfoChip("TRACKS", tracks.size.toString())
                InfoChip("ACCENT", settings.accentColor.uppercase())
                InfoChip("HAPTICS", if (settings.hapticsEnabled) "ON" else "OFF")
            }
        }

        // ── List ──
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(14.dp))
                    Text("SCANNING DEVICE…",
                        color = Color(0xFF7A8FA6), fontSize = 11.sp,
                        letterSpacing = 4.sp, fontFamily = FontFamily.Monospace)
                }
            }

            tracks.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.MusicNote, null,
                        tint = Color(0xFF3D5266),
                        modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("NO AUDIO FILES",
                        color = Color(0xFF7A8FA6), fontSize = 12.sp,
                        letterSpacing = 4.sp, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(4.dp))
                    Text("copy music to /sdcard/Music",
                        color = Color(0xFF3D5266), fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace)
                }
            }

            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(tracks, key = { it.id }) { t ->
                    TrackRow(t)
                }
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Column {
        Text(label, color = Color(0xFF4A6272), fontSize = 9.sp,
            letterSpacing = 2.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp,
            fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun TrackRow(t: Track) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Phase 2C: play */ }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = Color(0x1100E5FF),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.MusicNote, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(t.title, color = Color.White, fontSize = 14.sp,
                fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace,
                maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text("${t.artist} · ${t.album}", color = Color(0xFF7A8FA6),
                fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(t.durationLabel, color = Color(0xFFB0C2D0),
                fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text(t.sizeLabel, color = Color(0xFF3D5266),
                fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }
    }
    HorizontalDivider(color = Color(0x1100E5FF), modifier = Modifier.padding(start = 74.dp))
}

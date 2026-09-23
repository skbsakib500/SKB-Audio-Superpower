package com.skbsakib.audiosuperpower.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.skbsakib.audiosuperpower.library.Recent
import com.skbsakib.audiosuperpower.player.NativePlayer
import com.skbsakib.audiosuperpower.ui.theme.LocalThemeVariant

/**
 * Home — quick entry point. Recently played + library CTA.
 * Phase 3 will expand this into a full dashboard.
 */
@Composable
fun HomeScreen(
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val ctx = LocalContext.current
    val variant = LocalThemeVariant.current
    val recent by remember { Recent.flow(ctx) }
        .collectAsStateWithLifecycle(initialValue = emptyList())

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(
                listOf(variant.background, variant.backgroundAlt)
            ))
    ) {
        // Header
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
                Text("2080 LAB",
                    color = Color.White, fontSize = 22.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 3.sp,
                    fontFamily = FontFamily.Monospace)
            }
            Icon(Icons.Filled.Settings, "Settings",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp).clickable { onOpenSettings() })
        }

        // Quick open library CTA
        Surface(
            color = Color(0x1A00E5FF),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clickable { onOpenLibrary() }
        ) {
            Row(
                Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.LibraryMusic, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("OPEN LIBRARY",
                        color = Color.White, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace)
                    Text("browse tracks · albums · folders",
                        color = Color(0xFF7A8FA6), fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace)
                }
                Icon(Icons.Filled.PlayArrow, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp))
            }
        }

        Spacer(Modifier.height(24.dp))

        Text("RECENTLY PLAYED",
            color = Color(0xFF4A6272), fontSize = 10.sp,
            letterSpacing = 4.sp, fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(10.dp))

        if (recent.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.MusicNote, null,
                        tint = Color(0xFF3D5266),
                        modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("no recent activity",
                        color = Color(0xFF3D5266), fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(recent.take(20), key = { it.path }) { t ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                NativePlayer.load(ctx.applicationContext, t, autoplay = true)
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0x1A00E5FF),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.MusicNote, null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.title, color = Color.White,
                                fontSize = 13.sp, fontFamily = FontFamily.Monospace,
                                maxLines = 1)
                            Text(t.artist, color = Color(0xFF7A8FA6),
                                fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                                maxLines = 1)
                        }
                    }
                }
                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }
}

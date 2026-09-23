package com.skbsakib.audiosuperpower.ui.shell

import android.content.Intent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skbsakib.audiosuperpower.playback.PlaybackService
import com.skbsakib.audiosuperpower.player.NativePlayer
import com.skbsakib.audiosuperpower.player.PlayerState

/**
 * Global mini-player. Visible on every primary screen when something is
 * loaded. Tap → opens the full player overlay. Swipe left/right → next/prev.
 */
@Composable
fun MiniPlayerBar(
    onOpenPlayer: () -> Unit
) {
    val ctx = LocalContext.current
    val snapshot by NativePlayer.snapshot.collectAsStateWithLifecycle()

    if (snapshot.state == PlayerState.IDLE || snapshot.title.isBlank()) return

    var dragValue by remember { mutableStateOf<Float?>(null) }
    val realProgress = if (snapshot.durationMs > 0)
        (snapshot.positionMs.toFloat() / snapshot.durationMs.toFloat()).coerceIn(0f, 1f)
    else 0f
    val animated by animateFloatAsState(
        targetValue = realProgress,
        animationSpec = tween(250, easing = LinearEasing),
        label = "mini-seek"
    )
    val display = dragValue ?: animated

    Surface(
        color = Color(0xFF0E1826),
        shadowElevation = 12.dp,
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, delta ->
                    if (delta > 40f) NativePlayer.previous()
                    else if (delta < -40f) NativePlayer.next()
                }
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dy ->
                    if (dy < -30f) onOpenPlayer()
                }
            }
            .clickable { onOpenPlayer() }
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0x1A00E5FF),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.MusicNote, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(snapshot.title, color = Color.White,
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace, maxLines = 1)
                    Spacer(Modifier.height(2.dp))
                    Text("${snapshot.artist} · ${snapshot.sourceInfo}",
                        color = Color(0xFF7A8FA6), fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace, maxLines = 1)
                }
                IconButton(onClick = {
                    NativePlayer.togglePlayPause()
                    val i = Intent(ctx, PlaybackService::class.java)
                    i.action = if (snapshot.state == PlayerState.PLAYING)
                        PlaybackService.ACTION_PAUSE else PlaybackService.ACTION_PLAY
                    ctx.startService(i)
                }) {
                    Icon(
                        imageVector = if (snapshot.state == PlayerState.PLAYING)
                            Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                IconButton(onClick = {
                    NativePlayer.stop()
                    PlaybackService.stopService(ctx)
                }) {
                    Icon(Icons.Filled.Stop, "Stop",
                        tint = Color(0xFF7A8FA6), modifier = Modifier.size(20.dp))
                }
            }
            Slider(
                value = display,
                onValueChange = { dragValue = it },
                onValueChangeFinished = {
                    dragValue?.let { NativePlayer.seekToFraction(it.toDouble()) }
                    dragValue = null
                },
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color(0x2200E5FF)
                ),
                modifier = Modifier.fillMaxWidth().height(22.dp)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatMs(if (dragValue != null)
                        (dragValue!! * snapshot.durationMs).toLong() else snapshot.positionMs),
                    color = Color(0xFF7A8FA6), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text(snapshot.deviceInfo, color = Color(0xFF3D5266),
                    fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text(formatMs(snapshot.durationMs), color = Color(0xFF7A8FA6),
                    fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

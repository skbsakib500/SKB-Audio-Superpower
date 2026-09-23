package com.skbsakib.audiosuperpower.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skbsakib.audiosuperpower.library.Favorites
import com.skbsakib.audiosuperpower.player.NativePlayer
import com.skbsakib.audiosuperpower.player.PlayerState
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val snap by NativePlayer.snapshot.collectAsStateWithLifecycle()

    val favFlow = remember { Favorites.keys(ctx) }
    val favKeys by favFlow.collectAsStateWithLifecycle(initialValue = emptySet())
    val currentKey = if (snap.title.isNotBlank()) "${snap.title}::${snap.artist}" else ""
    val isFav = currentKey in favKeys

    var dragValue by remember { mutableStateOf<Float?>(null) }
    val realProgress = if (snap.durationMs > 0)
        (snap.positionMs.toFloat() / snap.durationMs.toFloat()).coerceIn(0f, 1f)
    else 0f
    val animated by animateFloatAsState(
        targetValue = realProgress,
        animationSpec = tween(250, easing = LinearEasing),
        label = "seek"
    )
    val display = dragValue ?: animated

    val scope = rememberCoroutineScope()

    // Swipe-down to close
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A1220), Color(0xFF050810), Color(0xFF000000))
                )
            )
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dy ->
                    if (dy > 8f) onClose()
                }
            }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // Top bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp).clickable { onClose() })
                Spacer(Modifier.weight(1f))
                Text("NOW PLAYING",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp, letterSpacing = 4.sp,
                    fontFamily = FontFamily.Monospace)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Filled.Stop, "Stop",
                    tint = Color(0xFF7A8FA6),
                    modifier = Modifier.size(22.dp)
                        .clickable { NativePlayer.stop() })
            }

            Spacer(Modifier.height(30.dp))

            // Album placeholder
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                Color(0xFF0A1220)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Icon(Icons.Filled.MusicNote, null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(120.dp).align(Alignment.Center))
            }

            Spacer(Modifier.height(28.dp))

            // Track info
            Text(snap.title.ifBlank { "—" }, color = Color.White,
                fontSize = 22.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 2, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Text(snap.artist.ifBlank { "—" }, color = Color(0xFF7A8FA6),
                fontSize = 13.sp, fontFamily = FontFamily.Monospace,
                maxLines = 1, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(22.dp))

            // Seek slider
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
                modifier = Modifier.fillMaxWidth().height(28.dp)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatMs(if (dragValue != null)
                        (dragValue!! * snap.durationMs).toLong() else snap.positionMs),
                    color = Color(0xFF7A8FA6), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text(formatMs(snap.durationMs),
                    color = Color(0xFF7A8FA6), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }

            Spacer(Modifier.height(20.dp))

            // Transport
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { NativePlayer.previous() }) {
                    Icon(Icons.Filled.SkipPrevious, "Previous",
                        tint = Color.White, modifier = Modifier.size(42.dp))
                }
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.size(72.dp)
                        .clickable { NativePlayer.togglePlayPause() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (snap.state == PlayerState.PLAYING)
                                Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color(0xFF001318),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                IconButton(onClick = { NativePlayer.next() }) {
                    Icon(Icons.Filled.SkipNext, "Next",
                        tint = Color.White, modifier = Modifier.size(42.dp))
                }
            }

            Spacer(Modifier.height(24.dp))

            // Secondary row: favorite / shuffle / repeat / queue
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = {
                    if (currentKey.isNotBlank()) {
                        scope.launch { Favorites.toggle(ctx, currentKey) }
                    }
                }) {
                    Icon(
                        imageVector = if (isFav) Icons.Filled.Favorite
                                      else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFav) Color(0xFFFF4D7A)
                               else Color(0xFF7A8FA6),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Icon(Icons.Filled.Shuffle, "Shuffle",
                    tint = Color(0xFF3D5266),
                    modifier = Modifier.size(22.dp))
                Icon(Icons.Filled.Repeat, "Repeat",
                    tint = Color(0xFF3D5266),
                    modifier = Modifier.size(22.dp))
                Icon(Icons.Filled.QueueMusic, "Queue",
                    tint = Color(0xFF3D5266),
                    modifier = Modifier.size(22.dp))
            }

            Spacer(Modifier.weight(1f))

            // Bottom info
            Column(Modifier.fillMaxWidth()) {
                Text("SOURCE · ${snap.sourceInfo.ifBlank { "—" }}",
                    color = Color(0xFF4A6272), fontSize = 9.sp,
                    letterSpacing = 2.sp, fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(2.dp))
                Text("OUTPUT · ${snap.deviceInfo.ifBlank { "—" }}",
                    color = Color(0xFF4A6272), fontSize = 9.sp,
                    letterSpacing = 2.sp, fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(2.dp))
                Text("QUEUE · ${snap.queueSize}",
                    color = Color(0xFF4A6272), fontSize = 9.sp,
                    letterSpacing = 2.sp, fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

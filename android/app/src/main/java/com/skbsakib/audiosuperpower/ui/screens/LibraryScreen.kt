package com.skbsakib.audiosuperpower.ui.screens

import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skbsakib.audiosuperpower.library.Favorites
import com.skbsakib.audiosuperpower.library.FolderStore
import com.skbsakib.audiosuperpower.library.MediaStoreScanner
import com.skbsakib.audiosuperpower.library.Recent
import com.skbsakib.audiosuperpower.library.SafScanner
import com.skbsakib.audiosuperpower.library.Track
import com.skbsakib.audiosuperpower.playback.PlaybackService
import com.skbsakib.audiosuperpower.player.NativePlayer
import com.skbsakib.audiosuperpower.player.PlayerState
import com.skbsakib.audiosuperpower.settings.SkbSettings
import com.skbsakib.audiosuperpower.ui.components.TrackContextSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "SKB-Library"

enum class LibraryTab { SYSTEM, FOLDERS, FAVORITES, RECENT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    settings: SkbSettings,
    onOpenSettings: () -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenDspLab: () -> Unit,
    onOpenSpatialLab: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // ── Base lists ──
    var systemTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var folderTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var scanning by remember { mutableStateOf(true) }
    var tab by rememberSaveable { mutableStateOf(LibraryTab.SYSTEM) }

    // ── Selection / UI state ──
    var pendingDelete by remember { mutableStateOf<Track?>(null) }
    var contextTrack by remember { mutableStateOf<Track?>(null) }

    val snapshot by NativePlayer.snapshot.collectAsStateWithLifecycle()

    NativePlayer.attach(ctx.applicationContext)

    // ── Stores ──
    val folderStore = remember { FolderStore(ctx.applicationContext) }
    val folderUris by folderStore.folders.collectAsStateWithLifecycle(initialValue = emptySet())
    val favKeys by remember { Favorites.keys(ctx) }.collectAsStateWithLifecycle(initialValue = emptySet())
    val recentTracks by remember { Recent.flow(ctx) }.collectAsStateWithLifecycle(initialValue = emptyList())

    val listState: LazyListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    // ── SAF folder picker ──
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                ctx.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (t: Throwable) {
                Log.w(TAG, "persist permission failed: ${t.message}")
            }
            scope.launch { folderStore.add(uri.toString()) }
        }
    }

    // ── Initial scans ──
    LaunchedEffect(Unit) {
        scanning = true
        systemTracks = withContext(Dispatchers.IO) {
            runCatching { MediaStoreScanner.scan(ctx.applicationContext) }.getOrElse { emptyList() }
        }
        scanning = false
    }

    LaunchedEffect(folderUris) {
        if (folderUris.isEmpty()) { folderTracks = emptyList(); return@LaunchedEffect }
        scanning = true
        folderTracks = withContext(Dispatchers.IO) {
            runCatching { SafScanner.scan(ctx.applicationContext, folderUris) }.getOrElse { emptyList() }
        }
        scanning = false
    }

    // ── Feed active tab's list into player for next/prev ──
    val displayed: List<Track> = when (tab) {
        LibraryTab.SYSTEM -> systemTracks
        LibraryTab.FOLDERS -> folderTracks
        LibraryTab.FAVORITES -> systemTracks.filter { Favorites.keyOf(it) in favKeys }
        LibraryTab.RECENT -> recentTracks
    }
    LaunchedEffect(displayed) {
        NativePlayer.setLibrary(displayed, -1)
    }

    // ── Delete intent launcher ──
    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val t = pendingDelete
        if (result.resultCode == Activity.RESULT_OK && t != null) {
            systemTracks = systemTracks.filterNot { it.id == t.id }
            folderTracks = folderTracks.filterNot { it.id == t.id }
            if (snapshot.title == t.title && snapshot.artist == t.artist) NativePlayer.stop()
        }
        pendingDelete = null
    }

    // ── Delete confirm dialog ──
    if (pendingDelete != null) {
        val t = pendingDelete!!
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = Color(0xFF0E1826),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFB0C2D0),
            title = { Text("Delete track?",
                fontFamily = FontFamily.Monospace, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(t.title, color = Color.White, fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace, maxLines = 2)
                    Spacer(Modifier.height(6.dp))
                    Text(t.artist, color = Color(0xFF7A8FA6), fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(12.dp))
                    Text("This permanently removes the file.",
                        color = Color(0xFFFF8A8A), fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace, lineHeight = 16.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = pendingDelete
                    pendingDelete = null
                    if (target != null) {
                        performDelete(ctx, target,
                            onRequest = { sender ->
                                deleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
                            },
                            onDirectSuccess = {
                                systemTracks = systemTracks.filterNot { it.id == target.id }
                                folderTracks = folderTracks.filterNot { it.id == target.id }
                                if (snapshot.title == target.title) NativePlayer.stop()
                            })
                    }
                }) {
                    Text("DELETE", color = Color(0xFFFF4D4D),
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("CANCEL", color = Color(0xFF7A8FA6),
                        fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                }
            }
        )
    }

    // ── Context sheet ──
    contextTrack?.let { t ->
        val isFav = Favorites.keyOf(t) in favKeys
        TrackContextSheet(
            track = t,
            isFavorite = isFav,
            onDismiss = { contextTrack = null },
            onPlayNow = {
                val idx = displayed.indexOfFirst { it.id == t.id }
                NativePlayer.setLibrary(displayed, idx)
                NativePlayer.load(ctx.applicationContext, t, autoplay = true, libraryIndex = idx)
                PlaybackService.start(ctx.applicationContext)
                contextTrack = null
            },
            onPlayNext = { NativePlayer.enqueueNext(t); contextTrack = null },
            onAddToQueue = { NativePlayer.enqueue(t); contextTrack = null },
            onToggleFavorite = {
                scope.launch { Favorites.toggle(ctx, Favorites.keyOf(t)) }
                contextTrack = null
            },
            onDelete = { pendingDelete = t; contextTrack = null },
            onInfo = {
                Log.i(TAG, "info: ${t.path} (${t.sizeLabel}, ${t.durationLabel})")
                contextTrack = null
            }
        )
    }

    // ═══════════════════════════════════════════════════════
    //  Layout
    // ═══════════════════════════════════════════════════════
    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF050810), Color(0xFF0A1220))))
    ) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("SKB AUDIO · 2080 LAB",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp, letterSpacing = 3.sp, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(2.dp))
                Text("LIBRARY",
                    color = Color.White, fontSize = 22.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 3.sp,
                    fontFamily = FontFamily.Monospace)
            }
            Icon(Icons.Filled.SurroundSound, "Spatial Lab",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp).clickable { onOpenSpatialLab() })
            Spacer(Modifier.width(18.dp))
            Icon(Icons.Filled.Equalizer, "DSP Lab",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp).clickable { onOpenDspLab() })
            Spacer(Modifier.width(18.dp))
            Icon(Icons.Filled.Settings, "Settings",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp).clickable { onOpenSettings() })
        }

        HorizontalDivider(color = Color(0x2200E5FF))

        // Tabs
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TabChip("SYSTEM", Icons.Filled.LibraryMusic, tab == LibraryTab.SYSTEM, Modifier.weight(1f)) { tab = LibraryTab.SYSTEM }
            TabChip("FOLDERS", Icons.Filled.Folder, tab == LibraryTab.FOLDERS, Modifier.weight(1f)) { tab = LibraryTab.FOLDERS }
            TabChip("FAVS", Icons.Filled.Favorite, tab == LibraryTab.FAVORITES, Modifier.weight(1f)) { tab = LibraryTab.FAVORITES }
            TabChip("RECENT", Icons.Filled.History, tab == LibraryTab.RECENT, Modifier.weight(1f)) { tab = LibraryTab.RECENT }
        }

        // Status strip + actions
        Surface(color = Color(0x1100E5FF)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                InfoChip("TRACKS", displayed.size.toString())
                InfoChip("QUEUE", snapshot.queueSize.toString())
                InfoChip("PLAYER", snapshot.state.name)
                Spacer(Modifier.weight(1f))
                if (tab == LibraryTab.FOLDERS) {
                    Row(
                        Modifier
                            .clickable { folderPicker.launch(null) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Add, "Add folder",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("ADD FOLDER",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp, letterSpacing = 2.sp,
                            fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // Folder chips (only FOLDERS tab)
        if (tab == LibraryTab.FOLDERS && folderUris.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                folderUris.take(3).forEach { u ->
                    val label = Uri.decode(u).substringAfterLast(':').substringAfterLast('/')
                        .ifBlank { "folder" }
                    Surface(
                        color = Color(0x1A00E5FF),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clickable {
                            scope.launch { folderStore.remove(u) }
                        }
                    ) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label.take(18),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Spacer(Modifier.width(6.dp))
                            Text("×", color = Color(0xFFFF4D4D),
                                fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // List
        Box(Modifier.weight(1f)) {
            when {
                scanning && displayed.isEmpty() -> CenterMessage("SCANNING…", showSpinner = true)

                tab == LibraryTab.FOLDERS && folderUris.isEmpty() ->
                    CenterMessage("NO FOLDERS ADDED",
                        hint = "tap ADD FOLDER above to pick a folder")

                displayed.isEmpty() -> CenterMessage(
                    when (tab) {
                        LibraryTab.SYSTEM -> "NO AUDIO FILES"
                        LibraryTab.FOLDERS -> "NO AUDIO IN FOLDERS"
                        LibraryTab.FAVORITES -> "NO FAVORITES YET"
                        LibraryTab.RECENT -> "NOTHING PLAYED YET"
                    },
                    hint = when (tab) {
                        LibraryTab.SYSTEM -> "copy music to /sdcard/Music"
                        LibraryTab.FOLDERS -> "add a folder with music"
                        LibraryTab.FAVORITES -> "tap ♥ in the full player"
                        LibraryTab.RECENT -> "play something to see it here"
                    }
                )

                else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(displayed, key = { "${it.id}-${it.path}" }) { t ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart ||
                                    value == SwipeToDismissBoxValue.StartToEnd
                                ) { pendingDelete = t }
                                false
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = true,
                            enableDismissFromEndToStart = true,
                            backgroundContent = {
                                Box(
                                    Modifier.fillMaxSize()
                                        .background(Color(0xFF1A0A0E))
                                        .padding(horizontal = 24.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(Icons.Filled.Delete, "Delete",
                                        tint = Color(0xFFFF4D4D),
                                        modifier = Modifier.size(26.dp))
                                }
                            }
                        ) {
                            TrackRow(
                                t = t,
                                isCurrent = snapshot.title == t.title &&
                                            snapshot.artist == t.artist,
                                isFavorite = Favorites.keyOf(t) in favKeys,
                                onTap = {
                                    val idx = displayed.indexOfFirst { it.path == t.path }
                                    NativePlayer.setLibrary(displayed, idx)
                                    NativePlayer.load(ctx.applicationContext, t,
                                        autoplay = true, libraryIndex = idx)
                                    PlaybackService.start(ctx.applicationContext)
                                },
                                onLongPress = { contextTrack = t }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(110.dp)) }
                }
            }
        }

        // Mini player
        if (snapshot.state != PlayerState.IDLE && snapshot.title.isNotBlank()) {
            MiniPlayerBar(
                snapshot = snapshot,
                onPlayPause = {
                    NativePlayer.togglePlayPause()
                    val i = Intent(ctx, PlaybackService::class.java)
                    i.action = if (snapshot.state == PlayerState.PLAYING)
                        PlaybackService.ACTION_PAUSE else PlaybackService.ACTION_PLAY
                    ctx.startService(i)
                },
                onStop = {
                    NativePlayer.stop()
                    PlaybackService.stopService(ctx)
                },
                onSeek = { NativePlayer.seekToFraction(it) },
                onSwipeLeft = { NativePlayer.next() },
                onSwipeRight = { NativePlayer.previous() },
                onSwipeUp = { onOpenPlayer() },
                onTap = { onOpenPlayer() }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
//  Helpers
// ═══════════════════════════════════════════════════════

private fun performDelete(
    context: Context, track: Track,
    onRequest: (IntentSender) -> Unit,
    onDirectSuccess: () -> Unit
) {
    val isContent = track.path.startsWith("content://", ignoreCase = true)
    if (isContent) {
        // SAF — files we don't own can't be deleted via MediaStore. Skip.
        Log.w(TAG, "cannot delete SAF track without DocumentsContract delete; skipping")
        return
    }
    val uri = ContentUris.withAppendedId(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, track.id)
    if (Build.VERSION.SDK_INT >= 30) {
        try {
            val pi = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
            onRequest(pi.intentSender)
        } catch (t: Throwable) { Log.e(TAG, "createDeleteRequest: ${t.message}") }
    } else {
        try {
            val rows = context.contentResolver.delete(uri, null, null)
            if (rows > 0) onDirectSuccess()
        } catch (t: Throwable) { Log.e(TAG, "direct delete: ${t.message}") }
    }
}

@Composable
private fun TabChip(
    label: String, icon: ImageVector, active: Boolean,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Surface(
        color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else Color(0x1100E5FF),
        shape = RoundedCornerShape(20.dp),
        border = if (active) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null,
                tint = if (active) MaterialTheme.colorScheme.primary else Color(0xFF7A8FA6),
                modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(label,
                color = if (active) MaterialTheme.colorScheme.primary else Color(0xFF7A8FA6),
                fontSize = 9.sp, letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun CenterMessage(text: String, hint: String? = null, showSpinner: Boolean = false) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (showSpinner) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(14.dp))
            } else {
                Icon(Icons.Filled.MusicNote, null, tint = Color(0xFF3D5266),
                    modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(12.dp))
            }
            Text(text, color = Color(0xFF7A8FA6),
                fontSize = 12.sp, letterSpacing = 4.sp, fontFamily = FontFamily.Monospace)
            if (hint != null) {
                Spacer(Modifier.height(4.dp))
                Text(hint, color = Color(0xFF3D5266), fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackRow(
    t: Track, isCurrent: Boolean, isFavorite: Boolean,
    onTap: () -> Unit, onLongPress: () -> Unit
) {
    val accent = if (isCurrent) MaterialTheme.colorScheme.primary else Color(0xFF7A8FA6)
    Box(Modifier.background(Color(0xFF050810))) {
        Row(
            Modifier.fillMaxWidth()
                .combinedClickable(onClick = onTap, onLongClick = onLongPress)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else Color(0x1100E5FF),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.MusicNote, null, tint = accent,
                        modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t.title,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White,
                        fontSize = 14.sp, fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace, maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false))
                    if (isFavorite) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Filled.Favorite, null,
                            tint = Color(0xFFFF4D7A),
                            modifier = Modifier.size(12.dp))
                    }
                }
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
        HorizontalDivider(color = Color(0x1100E5FF),
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 74.dp))
    }
}

@Composable
private fun MiniPlayerBar(
    snapshot: com.skbsakib.audiosuperpower.player.PlayerSnapshot,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onSeek: (Double) -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeUp: () -> Unit,
    onTap: () -> Unit
) {
    var dragValue by remember { mutableStateOf<Float?>(null) }
    val realProgress = if (snapshot.durationMs > 0)
        (snapshot.positionMs.toFloat() / snapshot.durationMs.toFloat()).coerceIn(0f, 1f)
    else 0f
    val animated by animateFloatAsState(
        targetValue = realProgress,
        animationSpec = tween(250, easing = LinearEasing),
        label = "seek"
    )
    val display = dragValue ?: animated

    Surface(
        color = Color(0xFF0E1826),
        shadowElevation = 12.dp,
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, delta ->
                    if (delta > 40f) onSwipeRight()
                    else if (delta < -40f) onSwipeLeft()
                }
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dy ->
                    if (dy < -30f) onSwipeUp()
                }
            }
            .clickable { onTap() }
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(snapshot.title, color = Color.White,
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace, maxLines = 1)
                    Spacer(Modifier.height(2.dp))
                    Text("${snapshot.artist} · ${snapshot.sourceInfo}",
                        color = Color(0xFF7A8FA6), fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace, maxLines = 1)
                }
                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (snapshot.state == PlayerState.PLAYING)
                            Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                IconButton(onClick = onStop) {
                    Icon(Icons.Filled.Stop, "Stop",
                        tint = Color(0xFF7A8FA6), modifier = Modifier.size(22.dp))
                }
            }
            Slider(
                value = display,
                onValueChange = { dragValue = it },
                onValueChangeFinished = {
                    dragValue?.let { onSeek(it.toDouble()) }
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

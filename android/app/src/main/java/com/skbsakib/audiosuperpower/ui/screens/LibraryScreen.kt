package com.skbsakib.audiosuperpower.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.skbsakib.audiosuperpower.library.Favorites
import com.skbsakib.audiosuperpower.library.FolderStore
import com.skbsakib.audiosuperpower.library.MediaStoreScanner
import com.skbsakib.audiosuperpower.library.Recent
import com.skbsakib.audiosuperpower.library.SafScanner
import com.skbsakib.audiosuperpower.library.Track
import com.skbsakib.audiosuperpower.playback.PlaybackService
import com.skbsakib.audiosuperpower.player.NativePlayer
import com.skbsakib.audiosuperpower.ui.components.TrackContextSheet
import com.skbsakib.audiosuperpower.ui.screens.library.components.InfoChip
import com.skbsakib.audiosuperpower.ui.screens.library.components.LibraryEmptyState
import com.skbsakib.audiosuperpower.ui.screens.library.components.LibraryTabChip
import com.skbsakib.audiosuperpower.ui.screens.library.components.MiniStat
import com.skbsakib.audiosuperpower.ui.screens.library.components.TrackRow
import com.skbsakib.audiosuperpower.ui.screens.library.util.performDelete
import com.skbsakib.audiosuperpower.ui.theme.LocalThemeVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "SKB-Library"

enum class LibraryTab { SYSTEM, FOLDERS, FAVORITES, RECENT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onOpenSettings: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val variant = LocalThemeVariant.current

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
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
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

    val displayed: List<Track> = when (tab) {
        LibraryTab.SYSTEM -> systemTracks
        LibraryTab.FOLDERS -> folderTracks
        LibraryTab.FAVORITES -> systemTracks.filter { Favorites.keyOf(it) in favKeys }
        LibraryTab.RECENT -> recentTracks
    }
    LaunchedEffect(displayed) {
        NativePlayer.setLibrary(displayed, -1)
    }

    // ── Delete launcher ──
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

    // ── Delete dialog ──
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
            .background(Brush.verticalGradient(listOf(variant.background, variant.backgroundAlt)))
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
            LibraryTabChip("SYSTEM", Icons.Filled.LibraryMusic, tab == LibraryTab.SYSTEM, Modifier.weight(1f)) { tab = LibraryTab.SYSTEM }
            LibraryTabChip("FOLDERS", Icons.Filled.Folder, tab == LibraryTab.FOLDERS, Modifier.weight(1f)) { tab = LibraryTab.FOLDERS }
            LibraryTabChip("FAVS", Icons.Filled.Favorite, tab == LibraryTab.FAVORITES, Modifier.weight(1f)) { tab = LibraryTab.FAVORITES }
            LibraryTabChip("RECENT", Icons.Filled.History, tab == LibraryTab.RECENT, Modifier.weight(1f)) { tab = LibraryTab.RECENT }
        }

        // Status strip
        Surface(color = Color(0x1100E5FF)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                InfoChip("TRACKS", displayed.size.toString())
                val videoCount = displayed.count { it.isVideo }
                if (videoCount > 0) InfoChip("VIDEO", videoCount.toString())
                InfoChip("QUEUE", snapshot.queueSize.toString())
                InfoChip("PLAYER", snapshot.state.name)
                Spacer(Modifier.weight(1f))
                if (tab == LibraryTab.FOLDERS) {
                    Row(
                        Modifier.clickable { folderPicker.launch(null) }
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

        // Folder chips
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

        // Folder stats
        if (tab == LibraryTab.FOLDERS && folderTracks.isNotEmpty()) {
            val audioCount = folderTracks.count { !it.isVideo }
            val videoCount = folderTracks.count { it.isVideo }
            val totalBytes = folderTracks.sumOf { it.sizeBytes }
            val totalMb = totalBytes / 1_000_000.0

            Surface(
                color = Color(0x0D00E5FF),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    MiniStat("AUDIO", audioCount.toString())
                    if (videoCount > 0) MiniStat("VIDEO", videoCount.toString())
                    MiniStat("SIZE", "%.1f MB".format(totalMb))
                    MiniStat("FOLDERS", folderUris.size.toString())
                }
            }
        }

        // List
        Box(Modifier.weight(1f)) {
            when {
                scanning && displayed.isEmpty() ->
                    LibraryEmptyState("SCANNING…", showSpinner = true)

                tab == LibraryTab.FOLDERS && folderUris.isEmpty() ->
                    LibraryEmptyState("NO FOLDERS ADDED",
                        hint = "tap ADD FOLDER above to pick a folder")

                displayed.isEmpty() -> LibraryEmptyState(
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
                                isCurrent = snapshot.title == t.title && snapshot.artist == t.artist,
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
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

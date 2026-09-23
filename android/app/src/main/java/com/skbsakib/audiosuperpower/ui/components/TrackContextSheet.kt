package com.skbsakib.audiosuperpower.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skbsakib.audiosuperpower.library.Track

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackContextSheet(
    track: Track,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onPlayNow: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onInfo: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0E1826),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF3D5266)) }
    ) {
        Column(Modifier.padding(bottom = 24.dp)) {
            // Track header
            Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text(track.title, color = Color.White,
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Text("${track.artist} · ${track.album}",
                    color = Color(0xFF7A8FA6), fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace, maxLines = 1)
            }

            HorizontalDivider(color = Color(0x2200E5FF),
                modifier = Modifier.padding(vertical = 8.dp))

            SheetAction(Icons.Filled.PlayArrow, "Play now",
                MaterialTheme.colorScheme.primary, onPlayNow)
            SheetAction(Icons.Filled.PlayArrow, "Play next",
                Color.White, onPlayNext)
            SheetAction(Icons.Filled.QueueMusic, "Add to queue",
                Color.White, onAddToQueue)
            SheetAction(
                if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                if (isFavorite) "Remove from favorites" else "Add to favorites",
                if (isFavorite) Color(0xFFFF4D7A) else Color.White,
                onToggleFavorite
            )
            SheetAction(Icons.Filled.Info, "Track info",
                Color(0xFF7A8FA6), onInfo)
            SheetAction(Icons.Filled.Delete, "Delete from device",
                Color(0xFFFF4D4D), onDelete)
        }
    }
}

@Composable
private fun SheetAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(18.dp))
        Text(label, color = tint, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
    }
}

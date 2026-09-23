package com.skbsakib.audiosuperpower.ui.screens.library.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skbsakib.audiosuperpower.library.Track

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackRow(
    t: Track,
    isCurrent: Boolean,
    isFavorite: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val accent = if (isCurrent) MaterialTheme.colorScheme.primary else Color(0xFF7A8FA6)
    Box(Modifier.background(Color(0xFF050810))) {
        Row(
            Modifier
                .fillMaxWidth()
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
                    if (t.isVideo) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Filled.VideoLibrary, "Video",
                            tint = Color(0xFF7A8FA6),
                            modifier = Modifier.size(12.dp))
                    }
                    if (t.isHiRes) {
                        Spacer(Modifier.width(4.dp))
                        Text("HR",
                            color = Color(0xFFB4FF39),
                            fontSize = 8.sp, letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace)
                    }
                    if (isFavorite) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Filled.Favorite, null,
                            tint = Color(0xFFFF4D7A),
                            modifier = Modifier.size(12.dp))
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    if (t.isVideo) "${t.artist} · ${t.extension.uppercase()} · video audio track"
                    else "${t.artist} · ${t.album}",
                    color = Color(0xFF7A8FA6),
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

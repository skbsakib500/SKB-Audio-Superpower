package com.skbsakib.audiosuperpower.library

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SafScanner {

    private val AUDIO_EXT = setOf(
        "mp3", "wav", "flac", "m4a", "aac",
        "ogg", "opus", "wma", "mka", "mp4", "aiff", "aif", "alac"
    )

    suspend fun scan(context: Context, folderUris: Set<String>): List<Track> =
        withContext(Dispatchers.IO) {
            if (folderUris.isEmpty()) return@withContext emptyList()
            val out = ArrayList<Track>(512)
            val seen = HashSet<String>()
            for (u in folderUris) {
                val tree = runCatching {
                    DocumentFile.fromTreeUri(context, Uri.parse(u))
                }.getOrNull() ?: continue
                walk(context, tree, out, seen)
            }
            out.sortedBy { it.title.lowercase() }
        }

    private fun walk(
        ctx: Context,
        dir: DocumentFile,
        out: MutableList<Track>,
        seen: MutableSet<String>
    ) {
        val kids = runCatching { dir.listFiles() }.getOrNull() ?: return
        for (f in kids) {
            if (f.isDirectory) {
                walk(ctx, f, out, seen)
            } else if (f.isFile) {
                val name = f.name ?: continue
                val dot = name.lastIndexOf('.')
                if (dot <= 0) continue
                val ext = name.substring(dot + 1).lowercase()
                if (ext !in AUDIO_EXT) continue

                val uriStr = f.uri.toString()
                if (!seen.add(uriStr)) continue

                val meta = readMeta(ctx, f.uri)
                val rawTitle = name.substring(0, dot)

                out += Track(
                    id = uriStr.hashCode().toLong(),
                    title = meta.title ?: rawTitle,
                    artist = meta.artist ?: "Unknown artist",
                    album = meta.album ?: (dir.name ?: "Unknown album"),
                    durationMs = meta.durationMs,
                    path = uriStr,
                    sizeBytes = f.length()
                )
            }
        }
    }

    private data class Meta(
        val title: String?,
        val artist: String?,
        val album: String?,
        val durationMs: Long
    )

    private fun readMeta(ctx: Context, uri: Uri): Meta {
        val mmr = MediaMetadataRetriever()
        return try {
            mmr.setDataSource(ctx, uri)
            val title  = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
            val album  = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val durStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            Meta(
                title = title?.takeIf { it.isNotBlank() },
                artist = artist?.takeIf { it.isNotBlank() && it != "<unknown>" },
                album = album?.takeIf { it.isNotBlank() && it != "<unknown>" },
                durationMs = durStr?.toLongOrNull() ?: 0L
            )
        } catch (_: Throwable) {
            Meta(null, null, null, 0L)
        } finally {
            runCatching { mmr.release() }
        }
    }
}

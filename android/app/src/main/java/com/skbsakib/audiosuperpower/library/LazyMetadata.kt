package com.skbsakib.audiosuperpower.library

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads metadata for a single track — call on-demand,
 * never during scan (too slow for thousands of files).
 */
object LazyMetadata {

    suspend fun read(context: Context, track: Track): Track =
        withContext(Dispatchers.IO) {
            val mmr = MediaMetadataRetriever()
            try {
                mmr.setDataSource(context, Uri.parse(track.path))
                val title  = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?: mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                val album  = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                val dur    = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

                track.copy(
                    title = title?.takeIf { it.isNotBlank() } ?: track.title,
                    artist = artist?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: track.artist,
                    album = album?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: track.album,
                    durationMs = dur?.toLongOrNull() ?: track.durationMs
                )
            } catch (_: Throwable) {
                track
            } finally {
                runCatching { mmr.release() }
            }
        }
}

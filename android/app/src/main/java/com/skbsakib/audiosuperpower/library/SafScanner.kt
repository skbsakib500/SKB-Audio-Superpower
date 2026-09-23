package com.skbsakib.audiosuperpower.library

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Scans ONLY user-selected folders via SAF tree URIs.
 * No access to files outside the chosen folders.
 */
object SafScanner {

    private val AUDIO_EXT = setOf(
        "mp3", "wav", "flac", "m4a", "aac",
        "ogg", "opus", "wma", "mka", "mp4"
    )

    suspend fun scan(context: Context, folderUris: Set<String>): List<Track> =
        withContext(Dispatchers.IO) {
            if (folderUris.isEmpty()) return@withContext emptyList()
            val out = ArrayList<Track>(256)
            for (u in folderUris) {
                val tree = runCatching {
                    DocumentFile.fromTreeUri(context, Uri.parse(u))
                }.getOrNull() ?: continue
                walk(tree, out)
            }
            out.sortedBy { it.title.lowercase() }
        }

    private fun walk(dir: DocumentFile, out: MutableList<Track>) {
        for (f in dir.listFiles()) {
            if (f.isDirectory) {
                walk(f, out)
            } else if (f.isFile) {
                val name = f.name ?: continue
                val dot = name.lastIndexOf('.')
                if (dot <= 0) continue
                val ext = name.substring(dot + 1).lowercase()
                if (ext !in AUDIO_EXT) continue

                out += Track(
                    id = f.uri.toString().hashCode().toLong(),
                    title = name.substring(0, dot),
                    artist = "Unknown Artist",
                    album = dir.name ?: "Unknown Album",
                    durationMs = 0L,
                    path = f.uri.toString(),
                    sizeBytes = f.length()
                )
            }
        }
    }
}

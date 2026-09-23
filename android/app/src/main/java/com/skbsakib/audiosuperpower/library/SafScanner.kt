package com.skbsakib.audiosuperpower.library

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fast SAF scanner using DocumentsContract (not DocumentFile).
 * Skips per-file MediaMetadataRetriever — metadata is lazy.
 */
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
                val treeUri = runCatching { Uri.parse(u) }.getOrNull() ?: continue
                val rootDocId = runCatching {
                    DocumentsContract.getTreeDocumentId(treeUri)
                }.getOrNull() ?: continue

                val rootChildren = runCatching {
                    DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootDocId)
                }.getOrNull() ?: continue

                // BFS queue — avoids deep recursion stack overflow
                val queue = ArrayDeque<Pair<Uri, String>>()
                queue.addLast(rootChildren to rootDocId)

                while (queue.isNotEmpty()) {
                    val (childrenUri, _) = queue.removeFirst()
                    walkChildren(context, treeUri, childrenUri, out, seen, queue)
                }
            }
            out.sortBy { it.title.lowercase() }
            out
        }

    private fun walkChildren(
        ctx: Context,
        treeUri: Uri,
        childrenUri: Uri,
        out: MutableList<Track>,
        seen: MutableSet<String>,
        queue: ArrayDeque<Pair<Uri, String>>
    ) {
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )
        val cursor: Cursor = runCatching {
            ctx.contentResolver.query(childrenUri, projection, null, null, null)
        }.getOrNull() ?: return

        cursor.use { c ->
            val idCol   = c.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
            if (idCol < 0 || nameCol < 0 || mimeCol < 0) return

            while (c.moveToNext()) {
                val docId = c.getString(idCol) ?: continue
                val name  = c.getString(nameCol) ?: continue
                val mime  = c.getString(mimeCol) ?: ""
                val size  = if (sizeCol >= 0) c.getLong(sizeCol) else 0L

                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    val subChildren = runCatching {
                        DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
                    }.getOrNull() ?: continue
                    queue.addLast(subChildren to docId)
                    continue
                }

                val dot = name.lastIndexOf('.')
                if (dot <= 0) continue
                val ext = name.substring(dot + 1).lowercase()
                if (ext !in AUDIO_EXT) continue

                val fileUri = runCatching {
                    DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                }.getOrNull() ?: continue

                val uriStr = fileUri.toString()
                if (!seen.add(uriStr)) continue

                out += Track(
                    id = uriStr.hashCode().toLong(),
                    title = name.substring(0, dot),
                    artist = "Unknown artist",
                    album = "Unknown album",
                    durationMs = 0L,
                    path = uriStr,
                    sizeBytes = size
                )
            }
        }
    }
}

package com.skbsakib.audiosuperpower.library

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fast SAF scanner using DocumentsContract (not DocumentFile).
 *
 * Indexes:
 *   • pure audio files (mp3, wav, flac, m4a, aac, ogg, opus, wma, mka, aiff, alac)
 *   • video containers with an audio track (mp4, m4v, mkv, mov, webm, 3gp, ts, m2ts)
 *
 * No per-file MediaMetadataRetriever (metadata is lazy via LazyMetadata).
 * BFS traversal, resumable-friendly, reports per-folder stats.
 */
object SafScanner {

    private val AUDIO_EXT = setOf(
        "mp3", "wav", "flac", "m4a", "aac",
        "ogg", "opus", "wma", "mka", "aiff", "aif", "alac"
    )

    private val VIDEO_EXT = setOf(
        "mp4", "m4v", "mkv", "mov", "webm", "3gp", "ts", "m2ts", "avi"
    )

    data class FolderStat(
        val uri: String,
        val trackCount: Int,
        val totalBytes: Long,
        val audioCount: Int,
        val videoCount: Int
    )

    data class ScanResult(
        val tracks: List<Track>,
        val stats: List<FolderStat>,
        val elapsedMs: Long
    )

    suspend fun scan(context: Context, folderUris: Set<String>): List<Track> =
        scanWithStats(context, folderUris).tracks

    suspend fun scanWithStats(context: Context, folderUris: Set<String>): ScanResult =
        withContext(Dispatchers.IO) {
            val t0 = System.currentTimeMillis()
            if (folderUris.isEmpty()) {
                return@withContext ScanResult(emptyList(), emptyList(), 0L)
            }

            val allTracks = ArrayList<Track>(512)
            val stats = ArrayList<FolderStat>()
            val globalSeen = HashSet<String>()

            for (u in folderUris) {
                val treeUri = runCatching { Uri.parse(u) }.getOrNull() ?: continue
                val rootDocId = runCatching {
                    DocumentsContract.getTreeDocumentId(treeUri)
                }.getOrNull() ?: continue

                val rootChildren = runCatching {
                    DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootDocId)
                }.getOrNull() ?: continue

                val localTracks = ArrayList<Track>(128)
                val queue = ArrayDeque<Pair<Uri, String>>()
                queue.addLast(rootChildren to rootDocId)

                while (queue.isNotEmpty()) {
                    val (childrenUri, _) = queue.removeFirst()
                    walkChildren(context, treeUri, childrenUri, localTracks, globalSeen, queue)
                }

                var audioCount = 0
                var videoCount = 0
                var totalBytes = 0L
                localTracks.forEach { t ->
                    totalBytes += t.sizeBytes
                    if (isVideoPath(t.path)) videoCount++ else audioCount++
                }

                stats += FolderStat(
                    uri = u,
                    trackCount = localTracks.size,
                    totalBytes = totalBytes,
                    audioCount = audioCount,
                    videoCount = videoCount
                )

                allTracks += localTracks
            }

            allTracks.sortBy { it.title.lowercase() }
            ScanResult(
                tracks = allTracks,
                stats = stats,
                elapsedMs = System.currentTimeMillis() - t0
            )
        }

    fun isVideoPath(path: String): Boolean {
        val dot = path.lastIndexOf('.')
        if (dot <= 0) return false
        val ext = path.substring(dot + 1).lowercase()
        return ext in VIDEO_EXT
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

                // Directory → enqueue
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    val subChildren = runCatching {
                        DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
                    }.getOrNull() ?: continue
                    queue.addLast(subChildren to docId)
                    continue
                }

                // Determine by mime first (more reliable than extension)
                val isAudio = mime.startsWith("audio/")
                val isVideo = mime.startsWith("video/")

                // Fall back to extension
                val dot = name.lastIndexOf('.')
                val ext = if (dot > 0) name.substring(dot + 1).lowercase() else ""
                val extAudio = ext in AUDIO_EXT
                val extVideo = ext in VIDEO_EXT

                if (!isAudio && !isVideo && !extAudio && !extVideo) continue

                val fileUri = runCatching {
                    DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                }.getOrNull() ?: continue

                val uriStr = fileUri.toString()
                if (!seen.add(uriStr)) continue

                out += Track(
                    id = uriStr.hashCode().toLong(),
                    title = if (dot > 0) name.substring(0, dot) else name,
                    artist = "Unknown artist",
                    album = if (isVideo || extVideo) "Video Library" else "Unknown album",
                    durationMs = 0L,
                    path = uriStr,
                    sizeBytes = size
                )
            }
        }
    }
}

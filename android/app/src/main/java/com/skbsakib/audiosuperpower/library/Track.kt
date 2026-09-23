package com.skbsakib.audiosuperpower.library

enum class TrackKind { AUDIO, VIDEO, UNKNOWN }

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val path: String,
    val sizeBytes: Long
) {
    val durationLabel: String
        get() {
            val totalSec = durationMs / 1000
            val m = totalSec / 60
            val s = totalSec % 60
            return "%d:%02d".format(m, s)
        }

    val sizeLabel: String
        get() = when {
            sizeBytes >= 1_000_000 -> "%.1f MB".format(sizeBytes / 1_000_000.0)
            sizeBytes >= 1_000 -> "%.0f KB".format(sizeBytes / 1_000.0)
            else -> "$sizeBytes B"
        }

    val extension: String
        get() {
            val cut = path.substringAfterLast('/').substringBefore('?')
            val dot = cut.lastIndexOf('.')
            return if (dot > 0) cut.substring(dot + 1).lowercase() else ""
        }

    val kind: TrackKind
        get() = when (extension) {
            "mp4", "m4v", "mkv", "mov", "webm", "3gp", "ts", "m2ts", "avi" -> TrackKind.VIDEO
            "mp3", "wav", "flac", "m4a", "aac", "ogg", "opus",
            "wma", "mka", "aiff", "aif", "alac" -> TrackKind.AUDIO
            else -> TrackKind.UNKNOWN
        }

    val isVideo: Boolean get() = kind == TrackKind.VIDEO
    val isHiRes: Boolean get() = extension in setOf("flac", "wav", "aiff", "aif", "alac")
}

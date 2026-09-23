package com.skbsakib.audiosuperpower.library

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
}

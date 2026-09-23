package com.skbsakib.audiosuperpower.ui.util

/** Format milliseconds as "M:SS". */
fun formatMs(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

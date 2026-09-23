package com.skbsakib.audiosuperpower.ui.screens.library.util

import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.skbsakib.audiosuperpower.library.Track

private const val TAG = "SKB-LibraryDelete"

/**
 * Delete a track through MediaStore.
 *
 * Android 11+  : uses MediaStore.createDeleteRequest (user confirmation)
 * Android 10-  : direct delete (needs WRITE_EXTERNAL_STORAGE up to API 29)
 * SAF content:// : skipped (we don't own the file)
 */
fun performDelete(
    context: Context,
    track: Track,
    onRequest: (IntentSender) -> Unit,
    onDirectSuccess: () -> Unit
) {
    val isContent = track.path.startsWith("content://", ignoreCase = true)
    if (isContent) {
        Log.w(TAG, "cannot delete SAF track; skipping")
        return
    }
    val uri = ContentUris.withAppendedId(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, track.id)
    if (Build.VERSION.SDK_INT >= 30) {
        try {
            val pi = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
            onRequest(pi.intentSender)
        } catch (t: Throwable) {
            Log.e(TAG, "createDeleteRequest: ${t.message}")
        }
    } else {
        try {
            val rows = context.contentResolver.delete(uri, null, null)
            if (rows > 0) onDirectSuccess()
        } catch (t: Throwable) {
            Log.e(TAG, "direct delete: ${t.message}")
        }
    }
}

package com.skbsakib.audiosuperpower.analyzer

import android.content.Context
import com.skbsakib.audiosuperpower.NativeBridge

/**
 * Orchestrates BS.1770-4 loudness analysis for tracks.
 *
 * Assumes the native player already has the track loaded (nativeLoadWav).
 * Reads its LUFS via JNI and caches the result per track path.
 *
 * Not a WorkManager worker — it's a lightweight coroutine helper called
 * from NativePlayer after load. For full-library scan, a driving loop
 * (in LibraryScreen) decodes + analyzes each track in sequence.
 */
object LoudnessAnalyzerWorker {

    /**
     * Analyze the currently loaded track and cache the result.
     * Returns the entry, or null if measurement failed.
     */
    suspend fun analyzeLoadedTrack(context: Context, trackPath: String): LoudnessCache.Entry? {
        return try {
            val lufs = NativeBridge.nativeAnalyzeLoadedLufs()
            val tp = NativeBridge.nativeAnalyzeLoadedTruePeakDb()
            if (lufs.isNaN() || lufs <= -71f) return null
            val entry = LoudnessCache.Entry(lufs, tp)
            LoudnessCache.put(context.applicationContext, trackPath, entry)
            entry
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Look up cached LUFS for a track. If not present, caller should
     * decode → nativeLoadWav → analyzeLoadedTrack.
     */
    suspend fun cached(context: Context, trackPath: String): LoudnessCache.Entry? =
        LoudnessCache.get(context.applicationContext, trackPath)

    /**
     * Given a target LUFS (default -14.0, matching Spotify/YouTube),
     * compute the gain in dB to apply for a measured track.
     * Result is clamped to [-12, +12] dB.
     */
    fun gainDbFor(measuredLufs: Float, targetLufs: Float = -14.0f): Float {
        val g = targetLufs - measuredLufs
        return g.coerceIn(-12f, 12f)
    }
}

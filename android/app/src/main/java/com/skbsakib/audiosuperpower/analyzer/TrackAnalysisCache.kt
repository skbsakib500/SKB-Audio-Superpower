package com.skbsakib.audiosuperpower.analyzer

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.trackAnalysisDataStore: DataStore<Preferences> by preferencesDataStore("skb_track_analysis")

/**
 * Persistent cache of per-track Smart Queue analysis.
 *
 * Format per line (unit-separator \u241F between fields):
 *   path  bpm  confidence  low  mid  high  centroidHz
 */
object TrackAnalysisCache {

    private const val SEP = "\u241F"
    private val KEY_LINES = stringPreferencesKey("lines")

    data class Entry(
        val bpm: Float,
        val confidence: Float,
        val low: Float,
        val mid: Float,
        val high: Float,
        val centroidHz: Float
    ) {
        /** Simple "energy" scalar 0..1 — biased toward perceived energy. */
        val energy: Float
            get() = (0.5f * high + 0.35f * mid + 0.15f * low).coerceIn(0f, 1f)
    }

    suspend fun get(context: Context, path: String): Entry? {
        return loadAll(context)[path]
    }

    suspend fun put(context: Context, path: String, e: Entry) {
        val map = loadAll(context).toMutableMap()
        map[path] = e
        write(context, map)
    }

    suspend fun clear(context: Context) {
        context.trackAnalysisDataStore.edit { it.remove(KEY_LINES) }
    }

    suspend fun loadAll(context: Context): Map<String, Entry> {
        val raw = context.trackAnalysisDataStore.data.first()[KEY_LINES] ?: ""
        val out = mutableMapOf<String, Entry>()
        raw.split("\n").forEach { line ->
            if (line.isBlank()) return@forEach
            val p = line.split(SEP)
            if (p.size < 7) return@forEach
            val e = Entry(
                bpm = p[1].toFloatOrNull() ?: 0f,
                confidence = p[2].toFloatOrNull() ?: 0f,
                low = p[3].toFloatOrNull() ?: 0f,
                mid = p[4].toFloatOrNull() ?: 0f,
                high = p[5].toFloatOrNull() ?: 0f,
                centroidHz = p[6].toFloatOrNull() ?: 0f
            )
            out[p[0]] = e
        }
        return out
    }

    private suspend fun write(context: Context, map: Map<String, Entry>) {
        val lines = map.entries.joinToString("\n") { (k, v) ->
            listOf(k, v.bpm, v.confidence, v.low, v.mid, v.high, v.centroidHz)
                .joinToString(SEP)
        }
        context.trackAnalysisDataStore.edit { it[KEY_LINES] = lines }
    }
}

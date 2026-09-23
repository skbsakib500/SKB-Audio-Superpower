package com.skbsakib.audiosuperpower.analyzer

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.loudnessDataStore: DataStore<Preferences> by preferencesDataStore("skb_loudness")

/**
 * Persistent cache of per-track integrated loudness (LUFS) + true peak.
 *
 * Format per line:  path\u241Flufs\u241FtruePeakDb
 * Where path is either a filesystem path or a SAF content:// URI.
 */
object LoudnessCache {

    private const val SEP = "\u241F"
    private val KEY_LINES = stringPreferencesKey("lines")

    suspend fun loadAll(context: Context): MutableMap<String, Entry> {
        val raw = context.loudnessDataStore.data.first()[KEY_LINES] ?: ""
        val map = mutableMapOf<String, Entry>()
        raw.split("\n").forEach { line ->
            if (line.isBlank()) return@forEach
            val p = line.split(SEP)
            if (p.size < 3) return@forEach
            val lufs = p[1].toFloatOrNull() ?: return@forEach
            val tp = p[2].toFloatOrNull() ?: -120f
            map[p[0]] = Entry(lufs, tp)
        }
        return map
    }

    suspend fun get(context: Context, path: String): Entry? = loadAll(context)[path]

    suspend fun put(context: Context, path: String, e: Entry) {
        val map = loadAll(context)
        map[path] = e
        val lines = map.entries.joinToString("\n") { (k, v) ->
            "$k$SEP${v.integratedLufs}$SEP${v.truePeakDb}"
        }
        context.loudnessDataStore.edit { it[KEY_LINES] = lines }
    }

    suspend fun putAll(context: Context, entries: Map<String, Entry>) {
        val existing = loadAll(context)
        existing.putAll(entries)
        val lines = existing.entries.joinToString("\n") { (k, v) ->
            "$k$SEP${v.integratedLufs}$SEP${v.truePeakDb}"
        }
        context.loudnessDataStore.edit { it[KEY_LINES] = lines }
    }

    suspend fun clear(context: Context) {
        context.loudnessDataStore.edit { it.remove(KEY_LINES) }
    }

    data class Entry(
        val integratedLufs: Float,
        val truePeakDb: Float
    )
}

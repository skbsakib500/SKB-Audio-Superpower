package com.skbsakib.audiosuperpower.library

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.recentDataStore: DataStore<Preferences> by preferencesDataStore("skb_recent")

/**
 * Recently-played list, encoded as newline-delimited JSON-ish key strings.
 * Format per line: title\u241Fartist\u241Falbum\u241Fpath\u241FsizeBytes
 * (unit separator \u241F keeps paths with ':' or ',' from breaking us)
 */
object Recent {
    private const val SEP = "\u241F"
    private const val MAX = 50
    private val KEY = stringPreferencesKey("recent_lines")

    fun flow(context: Context): Flow<List<Track>> =
        context.recentDataStore.data.map { prefs ->
            (prefs[KEY] ?: "").split("\n").filter { it.isNotBlank() }.mapNotNull { line ->
                val p = line.split(SEP)
                if (p.size < 5) return@mapNotNull null
                Track(
                    id = 0L,  // synthetic
                    title = p[0], artist = p[1], album = p[2],
                    durationMs = 0L, path = p[3],
                    sizeBytes = p[4].toLongOrNull() ?: 0L
                )
            }
        }

    suspend fun push(context: Context, t: Track) {
        val line = listOf(t.title, t.artist, t.album, t.path, t.sizeBytes.toString())
            .joinToString(SEP)
        context.recentDataStore.edit { prefs ->
            val cur = (prefs[KEY] ?: "").split("\n").filter { it.isNotBlank() }
            val dedup = cur.filterNot {
                val p = it.split(SEP)
                p.size >= 5 && p[3] == t.path
            }
            val next = (listOf(line) + dedup).take(MAX)
            prefs[KEY] = next.joinToString("\n")
        }
    }

    suspend fun clear(context: Context) {
        context.recentDataStore.edit { it.remove(KEY) }
    }
}

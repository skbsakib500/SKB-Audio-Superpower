package com.skbsakib.audiosuperpower.library

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.cacheDataStore: DataStore<Preferences> by preferencesDataStore("skb_library_cache")

/**
 * Caches the MediaStore scan result so the library appears instantly on
 * cold start. A background re-scan refreshes it.
 *
 * Format: newline-delimited records, unit-separator (\u241F) between fields:
 *   id\u241Ftitle\u241Fartist\u241Falbum\u241FdurationMs\u241Fpath\u241FsizeBytes
 */
object LibraryCache {

    private const val SEP = "\u241F"
    private val KEY_LINES = stringPreferencesKey("lines")
    private val KEY_TS = longPreferencesKey("timestamp")
    private val STALE_MS = 1000L * 60 * 60 * 6   // 6 hours

    fun flow(context: Context): Flow<List<Track>> =
        context.cacheDataStore.data.map { prefs ->
            (prefs[KEY_LINES] ?: "").split("\n")
                .filter { it.isNotBlank() }
                .mapNotNull { line -> parseLine(line) }
        }

    suspend fun read(context: Context): List<Track> = flow(context).first()

    suspend fun isStale(context: Context): Boolean {
        val ts = context.cacheDataStore.data.first()[KEY_TS] ?: 0L
        return System.currentTimeMillis() - ts > STALE_MS
    }

    suspend fun write(context: Context, tracks: List<Track>) {
        val lines = tracks.joinToString("\n") { t ->
            listOf(
                t.id, t.title, t.artist, t.album,
                t.durationMs, t.path, t.sizeBytes
            ).joinToString(SEP)
        }
        context.cacheDataStore.edit { prefs ->
            prefs[KEY_LINES] = lines
            prefs[KEY_TS] = System.currentTimeMillis()
        }
    }

    suspend fun clear(context: Context) {
        context.cacheDataStore.edit {
            it.remove(KEY_LINES); it.remove(KEY_TS)
        }
    }

    private fun parseLine(line: String): Track? {
        val p = line.split(SEP)
        if (p.size < 7) return null
        return Track(
            id = p[0].toLongOrNull() ?: return null,
            title = p[1],
            artist = p[2],
            album = p[3],
            durationMs = p[4].toLongOrNull() ?: 0L,
            path = p[5],
            sizeBytes = p[6].toLongOrNull() ?: 0L
        )
    }
}

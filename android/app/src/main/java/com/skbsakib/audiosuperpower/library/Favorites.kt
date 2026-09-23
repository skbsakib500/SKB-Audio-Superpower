package com.skbsakib.audiosuperpower.library

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.favDataStore: DataStore<Preferences> by preferencesDataStore("skb_favorites")

object Favorites {
    private val KEY = stringSetPreferencesKey("track_keys")

    fun keys(context: Context): Flow<Set<String>> =
        context.favDataStore.data.map { it[KEY] ?: emptySet() }

    suspend fun toggle(context: Context, key: String) {
        context.favDataStore.edit { prefs ->
            val cur = prefs[KEY] ?: emptySet()
            prefs[KEY] = if (cur.contains(key)) cur - key else cur + key
        }
    }

    fun keyOf(t: Track): String = "${t.title}::${t.artist}"
}

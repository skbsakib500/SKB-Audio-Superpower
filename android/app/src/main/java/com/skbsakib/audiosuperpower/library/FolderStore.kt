package com.skbsakib.audiosuperpower.library

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.skbFolderStore by preferencesDataStore(name = "skb_folders")

class FolderStore(private val ctx: Context) {
    private val KEY = stringSetPreferencesKey("selected_folder_uris")

    val folders: Flow<Set<String>> =
        ctx.skbFolderStore.data.map { it[KEY] ?: emptySet() }

    suspend fun add(uri: String) {
        ctx.skbFolderStore.edit { prefs ->
            val cur = prefs[KEY] ?: emptySet()
            prefs[KEY] = cur + uri
        }
    }
    suspend fun remove(uri: String) {
        ctx.skbFolderStore.edit { prefs ->
            val cur = prefs[KEY] ?: emptySet()
            prefs[KEY] = cur - uri
        }
    }
    suspend fun clear() {
        ctx.skbFolderStore.edit { it.remove(KEY) }
    }
}

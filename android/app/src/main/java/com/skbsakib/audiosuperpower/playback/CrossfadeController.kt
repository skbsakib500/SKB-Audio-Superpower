package com.skbsakib.audiosuperpower.playback

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.skbsakib.audiosuperpower.NativeBridge
import com.skbsakib.audiosuperpower.library.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.crossfadeDataStore: DataStore<Preferences> by preferencesDataStore("skb_crossfade")

/**
 * Crossfade duration controller. 0 = off (hard cut / gapless-ish).
 * Range: 0..12000 ms. Default 0 (safe — modern players use gapless).
 */
object CrossfadeController {

    private val KEY_MS = intPreferencesKey("crossfade_ms")

    private val _ms = MutableStateFlow(0)
    val ms: StateFlow<Int> = _ms.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun init(context: Context) {
        val saved = context.crossfadeDataStore.data.first()[KEY_MS] ?: 0
        _ms.value = saved
        NativeBridge.nativeSetCrossfadeMs(saved)
    }

    fun set(context: Context, ms: Int) {
        val clamped = ms.coerceIn(0, 12000)
        _ms.value = clamped
        NativeBridge.nativeSetCrossfadeMs(clamped)
        scope.launch {
            context.crossfadeDataStore.edit { it[KEY_MS] = clamped }
        }
    }

    /**
     * Preload the next track into native so it can blend seamlessly.
     * Call this once the current track is playing; safe to call repeatedly.
     */
    fun preloadNext(track: Track?, decodedWavPath: String?) {
        if (track == null || decodedWavPath.isNullOrBlank()) {
            NativeBridge.nativeClearNext()
            return
        }
        scope.launch(Dispatchers.IO) {
            runCatching { NativeBridge.nativeLoadNext(decodedWavPath) }
        }
    }

    fun clear() {
        NativeBridge.nativeClearNext()
    }
}

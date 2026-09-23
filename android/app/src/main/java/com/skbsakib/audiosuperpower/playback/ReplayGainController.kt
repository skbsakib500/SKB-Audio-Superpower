package com.skbsakib.audiosuperpower.playback

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.skbsakib.audiosuperpower.NativeBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.rgDataStore: DataStore<Preferences> by preferencesDataStore("skb_replaygain")

enum class RgMode { TRACK, ALBUM }

data class RgState(
    val enabled: Boolean = false,
    val mode: RgMode = RgMode.TRACK,
    val targetDb: Float = -1.0f,
    val preampDb: Float = 0.0f,          // user preamp offset
    val lastMeasuredPeakDb: Float = 0f,
    val lastAppliedGainDb: Float = 0f
)

/**
 * Controller for peak-based ReplayGain.
 *
 * Track/Album mode is stored for UI consistency; the current native engine
 * applies per-track normalization only (album mode requires full-library
 * scan, which is a Phase 10C+ feature).
 */
object ReplayGainController {

    private val KEY_ENABLED = booleanPreferencesKey("enabled")
    private val KEY_MODE = stringPreferencesKey("mode")
    private val KEY_TARGET = floatPreferencesKey("target_db")
    private val KEY_PREAMP = floatPreferencesKey("preamp_db")

    private val _state = MutableStateFlow(RgState())
    val state: StateFlow<RgState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun init(context: Context) {
        val prefs = context.rgDataStore.data.first()
        val enabled = prefs[KEY_ENABLED] ?: false
        val mode = when (prefs[KEY_MODE]) {
            "album" -> RgMode.ALBUM
            else -> RgMode.TRACK
        }
        val target = prefs[KEY_TARGET] ?: -1.0f
        val preamp = prefs[KEY_PREAMP] ?: 0.0f

        _state.value = RgState(
            enabled = enabled, mode = mode,
            targetDb = target, preampDb = preamp
        )

        NativeBridge.nativeSetReplayGainEnabled(enabled)
        NativeBridge.nativeSetReplayGainTargetDb(target + preamp)
    }

    fun setEnabled(context: Context, e: Boolean) {
        _state.value = _state.value.copy(enabled = e)
        NativeBridge.nativeSetReplayGainEnabled(e)
        scope.launch { context.rgDataStore.edit { it[KEY_ENABLED] = e } }
    }

    fun setMode(context: Context, mode: RgMode) {
        _state.value = _state.value.copy(mode = mode)
        scope.launch {
            context.rgDataStore.edit {
                it[KEY_MODE] = if (mode == RgMode.ALBUM) "album" else "track"
            }
        }
    }

    fun setTargetDb(context: Context, db: Float) {
        val c = db.coerceIn(-12f, 0f)
        _state.value = _state.value.copy(targetDb = c)
        NativeBridge.nativeSetReplayGainTargetDb(c + _state.value.preampDb)
        scope.launch { context.rgDataStore.edit { it[KEY_TARGET] = c } }
    }

    fun setPreampDb(context: Context, db: Float) {
        val c = db.coerceIn(-6f, 6f)
        _state.value = _state.value.copy(preampDb = c)
        NativeBridge.nativeSetReplayGainTargetDb(_state.value.targetDb + c)
        scope.launch { context.rgDataStore.edit { it[KEY_PREAMP] = c } }
    }

    /**
     * Called by NativePlayer after a track is decoded.
     * peakDb should come from a pre-scan of the decoded PCM.
     */
    fun applyMeasuredPeak(peakDb: Float) {
        _state.value = _state.value.copy(lastMeasuredPeakDb = peakDb)
        NativeBridge.nativeSetReplayGainPeakDb(peakDb)
    }

    fun refreshReadout() {
        val cur = runCatching { NativeBridge.nativeReplayGainCurrentDb() }.getOrElse { 0f }
        val peak = runCatching { NativeBridge.nativeReplayGainMeasuredPeakDb() }.getOrElse { 0f }
        _state.value = _state.value.copy(
            lastAppliedGainDb = cur,
            lastMeasuredPeakDb = peak
        )
    }
}

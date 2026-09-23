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
import com.skbsakib.audiosuperpower.analyzer.LoudnessAnalyzerWorker
import com.skbsakib.audiosuperpower.analyzer.LoudnessCache
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

/** Gain computation method. PEAK = sample peak; LUFS = BS.1770-4. */
enum class RgMethod { PEAK, LUFS }

data class RgState(
    val enabled: Boolean = false,
    val mode: RgMode = RgMode.TRACK,
    val method: RgMethod = RgMethod.LUFS,
    val targetDb: Float = -1.0f,          // peak target (dBFS)
    val targetLufs: Float = -14.0f,       // LUFS target
    val preampDb: Float = 0.0f,
    val lastMeasuredPeakDb: Float = 0f,
    val lastMeasuredLufs: Float = -70f,
    val lastAppliedGainDb: Float = 0f
)

object ReplayGainController {

    private val KEY_ENABLED = booleanPreferencesKey("enabled")
    private val KEY_MODE = stringPreferencesKey("mode")
    private val KEY_METHOD = stringPreferencesKey("method")
    private val KEY_TARGET = floatPreferencesKey("target_db")
    private val KEY_TARGET_LUFS = floatPreferencesKey("target_lufs")
    private val KEY_PREAMP = floatPreferencesKey("preamp_db")

    private val _state = MutableStateFlow(RgState())
    val state: StateFlow<RgState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun init(context: Context) {
        val prefs = context.rgDataStore.data.first()
        val enabled = prefs[KEY_ENABLED] ?: false
        val mode = when (prefs[KEY_MODE]) { "album" -> RgMode.ALBUM; else -> RgMode.TRACK }
        val method = when (prefs[KEY_METHOD]) { "peak" -> RgMethod.PEAK; else -> RgMethod.LUFS }
        val target = prefs[KEY_TARGET] ?: -1.0f
        val targetLufs = prefs[KEY_TARGET_LUFS] ?: -14.0f
        val preamp = prefs[KEY_PREAMP] ?: 0.0f

        _state.value = RgState(
            enabled = enabled, mode = mode, method = method,
            targetDb = target, targetLufs = targetLufs, preampDb = preamp
        )

        NativeBridge.nativeSetReplayGainEnabled(enabled)
        if (method == RgMethod.PEAK) {
            NativeBridge.nativeSetReplayGainTargetDb(target + preamp)
        }
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

    fun setMethod(context: Context, method: RgMethod) {
        _state.value = _state.value.copy(method = method)
        scope.launch {
            context.rgDataStore.edit {
                it[KEY_METHOD] = if (method == RgMethod.PEAK) "peak" else "lufs"
            }
        }
        // Re-apply current track: caller (NativePlayer) will re-trigger on next load
    }

    fun setTargetDb(context: Context, db: Float) {
        val c = db.coerceIn(-12f, 0f)
        _state.value = _state.value.copy(targetDb = c)
        if (_state.value.method == RgMethod.PEAK) {
            NativeBridge.nativeSetReplayGainTargetDb(c + _state.value.preampDb)
        }
        scope.launch { context.rgDataStore.edit { it[KEY_TARGET] = c } }
    }

    fun setTargetLufs(context: Context, lufs: Float) {
        val c = lufs.coerceIn(-30f, -5f)
        _state.value = _state.value.copy(targetLufs = c)
        scope.launch { context.rgDataStore.edit { it[KEY_TARGET_LUFS] = c } }
    }

    fun setPreampDb(context: Context, db: Float) {
        val c = db.coerceIn(-6f, 6f)
        _state.value = _state.value.copy(preampDb = c)
        if (_state.value.method == RgMethod.PEAK) {
            NativeBridge.nativeSetReplayGainTargetDb(_state.value.targetDb + c)
        }
        scope.launch { context.rgDataStore.edit { it[KEY_PREAMP] = c } }
    }

    /**
     * Called by NativePlayer after a track is loaded.
     *
     * PEAK method: pass measured peak dB → native sets gain directly.
     * LUFS method: analyze (or use cache) → derive gain from target LUFS.
     */
    fun applyMeasuredPeak(peakDb: Float) {
        _state.value = _state.value.copy(lastMeasuredPeakDb = peakDb)
        if (_state.value.method == RgMethod.PEAK) {
            NativeBridge.nativeSetReplayGainPeakDb(peakDb)
        }
    }

    /**
     * LUFS method: called from NativePlayer with the loaded track's path.
     * Tries cache first, then runs native analysis and caches the result.
     */
    fun applyLufsForTrack(context: Context, trackPath: String) {
        scope.launch {
            val cached = LoudnessAnalyzerWorker.cached(context, trackPath)
            val entry = cached ?: LoudnessAnalyzerWorker.analyzeLoadedTrack(context, trackPath)

            if (entry == null) return@launch

            _state.value = _state.value.copy(
                lastMeasuredLufs = entry.integratedLufs,
                lastMeasuredPeakDb = entry.truePeakDb
            )

            if (_state.value.method != RgMethod.LUFS) return@launch

            val gain = LoudnessAnalyzerWorker.gainDbFor(
                entry.integratedLufs,
                _state.value.targetLufs
            ) + _state.value.preampDb

            // Direct gain application (no fake-peak hack).
            NativeBridge.nativeSetReplayGainDirectGainDb(gain)
            _state.value = _state.value.copy(lastAppliedGainDb = gain)
        }
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

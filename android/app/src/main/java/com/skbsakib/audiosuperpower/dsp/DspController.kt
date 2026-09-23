package com.skbsakib.audiosuperpower.dsp

import com.skbsakib.audiosuperpower.NativeBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DspState(
    val enabled: Boolean = false,
    val preampDb: Float = 0f,
    val bandGainsDb: List<Float> = listOf(0f, 0f, 0f, 0f, 0f),
    val bassDb: Float = 0f,
    val trebleDb: Float = 0f,
    val limiterEnabled: Boolean = true,
    val limiterCeilingDb: Float = -0.3f,
    val activePreset: String = "FLAT"
)

object DspController {

    val BAND_FREQS = listOf(60, 240, 1000, 3500, 10000)

    private val _state = MutableStateFlow(DspState())
    val state: StateFlow<DspState> = _state.asStateFlow()

    fun setEnabled(e: Boolean) {
        _state.value = _state.value.copy(enabled = e)
        NativeBridge.nativeSetDspEnabled(e)
    }

    fun setPreamp(db: Float) {
        val c = db.coerceIn(-12f, 12f)
        _state.value = _state.value.copy(preampDb = c)
        NativeBridge.nativeSetPreamp(c)
    }

    fun setBand(index: Int, db: Float) {
        if (index !in 0..4) return
        val c = db.coerceIn(-12f, 12f)
        val list = _state.value.bandGainsDb.toMutableList()
        list[index] = c
        _state.value = _state.value.copy(bandGainsDb = list, activePreset = "CUSTOM")
        NativeBridge.nativeSetEqBand(index, c)
    }

    fun setBass(db: Float) {
        val c = db.coerceIn(-12f, 12f)
        _state.value = _state.value.copy(bassDb = c, activePreset = "CUSTOM")
        NativeBridge.nativeSetBass(c)
    }

    fun setTreble(db: Float) {
        val c = db.coerceIn(-12f, 12f)
        _state.value = _state.value.copy(trebleDb = c, activePreset = "CUSTOM")
        NativeBridge.nativeSetTreble(c)
    }

    fun setLimiterEnabled(e: Boolean) {
        _state.value = _state.value.copy(limiterEnabled = e)
        NativeBridge.nativeSetLimiterEnabled(e)
    }

    fun setLimiterCeiling(db: Float) {
        val c = db.coerceIn(-6f, 0f)
        _state.value = _state.value.copy(limiterCeilingDb = c)
        NativeBridge.nativeSetLimiterCeiling(c)
    }

    fun reset() {
        _state.value = DspState(enabled = _state.value.enabled)
        NativeBridge.nativeResetDsp()
        NativeBridge.nativeSetPreamp(0f)
        for (i in 0..4) NativeBridge.nativeSetEqBand(i, 0f)
        NativeBridge.nativeSetBass(0f)
        NativeBridge.nativeSetTreble(0f)
        NativeBridge.nativeSetLimiterEnabled(_state.value.limiterEnabled)
        NativeBridge.nativeSetLimiterCeiling(_state.value.limiterCeilingDb)
    }

    // ── Presets ──
    data class Preset(
        val name: String,
        val preampDb: Float,
        val bands: List<Float>,
        val bassDb: Float,
        val trebleDb: Float
    )

    val presets = listOf(
        Preset("FLAT",     0f, listOf(0f, 0f, 0f, 0f, 0f),      0f,  0f),
        Preset("BASS",    -2f, listOf(6f, 3f, 0f, -1f, 0f),     6f,  1f),
        Preset("VOCAL",    0f, listOf(-2f, 0f, 3f, 3f, 1f),     0f,  1f),
        Preset("ROCK",    -1f, listOf(4f, 2f, -1f, 2f, 4f),     3f,  3f),
        Preset("CLASSICAL",0f, listOf(2f, 1f, 0f, 1f, 3f),      1f,  2f),
        Preset("ELECTRONIC",-2f,listOf(6f, 4f, 0f, 2f, 3f),     5f,  3f),
        Preset("LOUDNESS",-3f, listOf(5f, 2f, -1f, 2f, 5f),     4f,  4f)
    )

    fun applyPreset(name: String) {
        val p = presets.firstOrNull { it.name == name } ?: return
        _state.value = _state.value.copy(
            preampDb = p.preampDb,
            bandGainsDb = p.bands,
            bassDb = p.bassDb,
            trebleDb = p.trebleDb,
            activePreset = p.name
        )
        NativeBridge.nativeSetPreamp(p.preampDb)
        p.bands.forEachIndexed { i, g -> NativeBridge.nativeSetEqBand(i, g) }
        NativeBridge.nativeSetBass(p.bassDb)
        NativeBridge.nativeSetTreble(p.trebleDb)
    }
}

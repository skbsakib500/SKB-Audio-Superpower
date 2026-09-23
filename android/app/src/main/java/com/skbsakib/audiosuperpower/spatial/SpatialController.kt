package com.skbsakib.audiosuperpower.spatial

import com.skbsakib.audiosuperpower.NativeBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SpatialMode(val id: Int, val label: String, val desc: String) {
    OFF(0,  "OFF",  "Stock stereo"),
    D3(3,   "3D",   "Stereo width + crossfeed"),
    D4(4,   "4D",   "Wider + subtle motion"),
    D5(5,   "5D",   "Wider + small room depth"),
    D6(6,   "6D",   "First elevation cue"),
    D7(7,   "7D",   "Noticeable above-head"),
    D8(8,   "8D",   "Strong 3D space"),
    D9(9,   "9D",   "Concert-like"),
    D10(10, "10D",  "Full immersive stage")
}

enum class RoomType(val id: Int, val label: String) {
    STUDIO(0, "STUDIO"),
    HALL(1, "HALL"),
    STAGE(2, "STAGE"),
    CATHEDRAL(3, "CATHEDRAL")
}

data class SpatialState(
    val enabled: Boolean = false,
    val mode: SpatialMode = SpatialMode.D3,
    val intensity: Float = 0.7f,
    val height: Float = 0.5f,
    val room: RoomType = RoomType.HALL
)

object SpatialController {

    private val _state = MutableStateFlow(SpatialState())
    val state: StateFlow<SpatialState> = _state.asStateFlow()

    fun setEnabled(e: Boolean) {
        _state.value = _state.value.copy(enabled = e)
        NativeBridge.nativeSetSpatialEnabled(e)
        if (e) {
            // Re-apply current mode when re-enabling
            applyAll()
        }
    }

    fun setMode(m: SpatialMode) {
        _state.value = _state.value.copy(mode = m)
        NativeBridge.nativeSetSpatialMode(m.id)
    }

    fun setIntensity(i: Float) {
        val c = i.coerceIn(0f, 1f)
        _state.value = _state.value.copy(intensity = c)
        NativeBridge.nativeSetSpatialIntensity(c)
    }

    fun setHeight(h: Float) {
        val c = h.coerceIn(0f, 1f)
        _state.value = _state.value.copy(height = c)
        NativeBridge.nativeSetSpatialHeight(c)
    }

    fun setRoom(r: RoomType) {
        _state.value = _state.value.copy(room = r)
        NativeBridge.nativeSetSpatialRoom(r.id)
    }

    fun reset() {
        NativeBridge.nativeResetSpatial()
        applyAll()
    }

    private fun applyAll() {
        val s = _state.value
        NativeBridge.nativeSetSpatialMode(s.mode.id)
        NativeBridge.nativeSetSpatialIntensity(s.intensity)
        NativeBridge.nativeSetSpatialHeight(s.height)
        NativeBridge.nativeSetSpatialRoom(s.room.id)
    }
}

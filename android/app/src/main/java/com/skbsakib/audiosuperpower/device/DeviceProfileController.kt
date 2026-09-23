package com.skbsakib.audiosuperpower.device

import android.content.Context
import android.util.Log
import com.skbsakib.audiosuperpower.dsp.DspController
import com.skbsakib.audiosuperpower.spatial.RoomType
import com.skbsakib.audiosuperpower.spatial.SpatialController
import com.skbsakib.audiosuperpower.spatial.SpatialMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "SKB-DeviceProfile"

data class DeviceState(
    val snapshot: DeviceProfile.Snapshot? = null,
    val autoTune: DeviceProfile.AutoTune? = null,
    val appliedAt: Long = 0L,
    val autoApplied: Boolean = false,
    val lastError: String? = null
)

/**
 * Holds the current device profile, runs detection, and applies the derived
 * AutoTune to DspController + SpatialController.
 *
 * Applies ONCE per app session (or on manual "Auto-tune now").
 * Never overrides user choices silently — user can disable via Settings later.
 */
object DeviceProfileController {

    private val _state = MutableStateFlow(DeviceState())
    val state: StateFlow<DeviceState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    /** Full detect + derive. Does NOT apply. */
    fun refresh(context: Context) {
        scope.launch {
            try {
                val snapshot = DeviceProfile.detect(context.applicationContext)
                val tune = DeviceProfile.derive(snapshot)
                _state.value = _state.value.copy(
                    snapshot = snapshot,
                    autoTune = tune,
                    lastError = null
                )
                Log.i(TAG, "detected: ${snapshot.summary.replace('\n', ' ')}")
                Log.i(TAG, "autotune: ${tune.reason}")
            } catch (t: Throwable) {
                Log.e(TAG, "refresh failed: ${t.message}")
                _state.value = _state.value.copy(lastError = t.message)
            }
        }
    }

    /** Detect + derive + immediately apply. Called on app start. */
    fun autoApply(context: Context) {
        scope.launch {
            try {
                val snapshot = DeviceProfile.detect(context.applicationContext)
                val tune = DeviceProfile.derive(snapshot)
                _state.value = _state.value.copy(
                    snapshot = snapshot,
                    autoTune = tune,
                    lastError = null
                )
                applyTune(tune)
                _state.value = _state.value.copy(
                    appliedAt = System.currentTimeMillis(),
                    autoApplied = true
                )
                Log.i(TAG, "auto-applied: ${tune.reason}")
            } catch (t: Throwable) {
                Log.e(TAG, "autoApply failed: ${t.message}")
                _state.value = _state.value.copy(lastError = t.message)
            }
        }
    }

    /** Re-apply the current autoTune without re-detecting. */
    fun reapply() {
        val tune = _state.value.autoTune ?: return
        scope.launch {
            applyTune(tune)
            _state.value = _state.value.copy(appliedAt = System.currentTimeMillis())
        }
    }

    private fun applyTune(t: DeviceProfile.AutoTune) {
        // DSP chain
        DspController.setEnabled(t.dspEnabled)
        DspController.setPreamp(t.preampDb)
        DspController.setBass(t.bassDb)
        DspController.setTreble(t.trebleDb)
        DspController.setLimiterCeiling(t.limiterCeilingDb)
        DspController.setLimiterEnabled(true)

        // Spatial chain
        val mode = SpatialMode.entries.firstOrNull { it.id == t.spatialModeId }
            ?: SpatialMode.OFF
        SpatialController.setMode(mode)
        SpatialController.setIntensity(t.spatialIntensity)
        SpatialController.setHeight(t.spatialHeight)
        val room = RoomType.entries.firstOrNull { it.id == t.spatialRoomId }
            ?: RoomType.HALL
        SpatialController.setRoom(room)
        SpatialController.setEnabled(mode != SpatialMode.OFF)
    }
}

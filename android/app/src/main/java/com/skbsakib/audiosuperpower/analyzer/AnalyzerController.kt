package com.skbsakib.audiosuperpower.analyzer

import com.skbsakib.audiosuperpower.NativeBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AnalyzerState(
    val enabled: Boolean = false,
    val spectrum: FloatArray = FloatArray(0),
    val peak: Float = 0f,
    val rmsDb: Float = -70f,
    val clipping: Boolean = false,
    val bins: Int = 0
)

object AnalyzerController {

    private val _state = MutableStateFlow(AnalyzerState())
    val state: StateFlow<AnalyzerState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var pollJob: Job? = null

    fun setEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(enabled = enabled)
        NativeBridge.nativeSetAnalyzerEnabled(enabled)
        if (enabled) startPolling() else stopPolling()
    }

    private fun startPolling() {
        stopPolling()
        pollJob = scope.launch {
            val bins = runCatching { NativeBridge.nativeAnalyzerBins() }.getOrElse { 512 }
            if (bins > 0) {
                _state.value = _state.value.copy(
                    bins = bins,
                    spectrum = FloatArray(bins)
                )
            }
            while (true) {
                try {
                    NativeBridge.nativeAnalyzerTick()
                    val spec = NativeBridge.nativeAnalyzerReadSpectrum()
                    val peak = NativeBridge.nativeAnalyzerTakePeak()
                    val rms = NativeBridge.nativeAnalyzerTakeRmsDb()
                    val clip = NativeBridge.nativeAnalyzerIsClipping()
                    _state.value = _state.value.copy(
                        spectrum = spec,
                        peak = peak,
                        rmsDb = rms,
                        clipping = clip
                    )
                } catch (_: Throwable) { /* swallow transient errors */ }
                delay(40L)   // ~25 FPS UI refresh
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }
}

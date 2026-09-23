package com.skbsakib.audiosuperpower.playback

import com.skbsakib.audiosuperpower.player.NativePlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Sleep timer: pauses playback after N minutes.
 * Emits remaining time to the UI for a live countdown.
 */
object SleepTimer {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var job: Job? = null

    private val _remainingMs = MutableStateFlow(0L)
    val remainingMs: StateFlow<Long> = _remainingMs.asStateFlow()

    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active.asStateFlow()

    fun start(minutes: Int) {
        stop()
        if (minutes <= 0) return
        var remaining = minutes * 60_000L
        _remainingMs.value = remaining
        _active.value = true

        job = scope.launch {
            while (remaining > 0) {
                delay(500L)
                remaining -= 500L
                _remainingMs.value = remaining
            }
            NativePlayer.pause()
            _active.value = false
            _remainingMs.value = 0L
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _active.value = false
        _remainingMs.value = 0L
    }

    fun formatted(): String {
        val ms = _remainingMs.value
        if (ms <= 0) return "--:--"
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }
}

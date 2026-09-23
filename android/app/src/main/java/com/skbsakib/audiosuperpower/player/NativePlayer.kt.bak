package com.skbsakib.audiosuperpower.player

import android.content.Context
import com.skbsakib.audiosuperpower.NativeBridge
import com.skbsakib.audiosuperpower.library.AudioDecoder
import com.skbsakib.audiosuperpower.library.Recent
import com.skbsakib.audiosuperpower.library.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PlayerState { IDLE, LOADING, PLAYING, PAUSED, COMPLETED, ERROR }

data class PlayerSnapshot(
    val state: PlayerState = PlayerState.IDLE,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val sourceInfo: String = "",
    val deviceInfo: String = "",
    val error: String? = null,
    val queueSize: Int = 0,
    val queueIndex: Int = -1
)

object NativePlayer {

    private val _snapshot = MutableStateFlow(PlayerSnapshot())
    val snapshot: StateFlow<PlayerSnapshot> = _snapshot.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var pollJob: Job? = null

    private var currentTrack: Track? = null
    private val queue = ArrayDeque<Track>()
    private var history = ArrayDeque<Track>()      // for "previous"
    private var libraryContext: List<Track> = emptyList()  // source list for auto-next
    private var currentLibraryIndex: Int = -1
    private var appContext: Context? = null

    fun attach(context: Context) {
        appContext = context.applicationContext
    }

    /** Sets the source list so next/prev can walk it by index. */
    fun setLibrary(list: List<Track>, index: Int) {
        libraryContext = list
        currentLibraryIndex = index
    }

    /** Load and optionally auto-play a track. */
    fun load(context: Context, track: Track, autoplay: Boolean = true, libraryIndex: Int = -1) {
        appContext = context.applicationContext
        if (libraryIndex >= 0) currentLibraryIndex = libraryIndex
        scope.launch {
            _snapshot.value = _snapshot.value.copy(
                state = PlayerState.LOADING,
                title = track.title, artist = track.artist, album = track.album,
                positionMs = 0L, durationMs = 0L, error = null
            )

            val decoded = AudioDecoder.decodeToCache(context.applicationContext, track.path)
            if (decoded == null) {
                _snapshot.value = _snapshot.value.copy(
                    state = PlayerState.ERROR, error = "Decode failed")
                return@launch
            }

            val ok = runCatching { NativeBridge.nativeLoadWav(decoded.wavPath) }
                .getOrElse { false }
            if (!ok) {
                _snapshot.value = _snapshot.value.copy(
                    state = PlayerState.ERROR, error = "Native load failed")
                return@launch
            }

            currentTrack?.let { prev -> if (prev.path != track.path) history.addLast(prev) }
            currentTrack = track

            val src = runCatching { NativeBridge.nativeSourceInfo() }.getOrElse { "" }
            val dur = runCatching { NativeBridge.nativeDurationMs() }.getOrElse { 0L }

            _snapshot.value = _snapshot.value.copy(
                state = PlayerState.PAUSED,
                positionMs = 0L, durationMs = dur,
                sourceInfo = src,
                queueSize = queue.size,
                error = null
            )

            Recent.push(context.applicationContext, track)

            if (autoplay) play()
        }
    }

    // ── Transport ──
    fun play() {
        scope.launch {
            val ok = runCatching { NativeBridge.nativePlay() }.getOrElse { false }
            if (!ok) {
                _snapshot.value = _snapshot.value.copy(
                    state = PlayerState.ERROR, error = "Play failed")
                return@launch
            }
            val dev = runCatching { NativeBridge.nativeDeviceInfo() }.getOrElse { "" }
            _snapshot.value = _snapshot.value.copy(
                state = PlayerState.PLAYING, deviceInfo = dev, error = null)
            startPolling()
        }
    }

    fun pause() {
        scope.launch {
            runCatching { NativeBridge.nativePause() }
            _snapshot.value = _snapshot.value.copy(state = PlayerState.PAUSED)
            stopPolling()
        }
    }

    fun togglePlayPause() {
        if (_snapshot.value.state == PlayerState.PLAYING) pause() else play()
    }

    fun stop() {
        scope.launch {
            runCatching { NativeBridge.nativeStop() }
            _snapshot.value = _snapshot.value.copy(state = PlayerState.IDLE, positionMs = 0L)
            stopPolling()
        }
    }

    fun seekToFraction(fraction: Double) {
        scope.launch {
            runCatching { NativeBridge.nativeSeek(fraction.coerceIn(0.0, 1.0)) }
            refreshPosition()
        }
    }

    // ── Next / Previous ──
    fun next() {
        val ctx = appContext ?: return
        // Priority: explicit queue → library list → nothing
        if (queue.isNotEmpty()) {
            val nt = queue.removeFirst()
            _snapshot.value = _snapshot.value.copy(queueSize = queue.size)
            load(ctx, nt, autoplay = true)
            return
        }
        if (libraryContext.isNotEmpty() && currentLibraryIndex >= 0) {
            val ni = (currentLibraryIndex + 1) % libraryContext.size
            currentLibraryIndex = ni
            load(ctx, libraryContext[ni], autoplay = true, libraryIndex = ni)
        }
    }

    fun previous() {
        val ctx = appContext ?: return
        // If >3s in, restart current
        if (_snapshot.value.positionMs > 3000L && currentTrack != null) {
            seekToFraction(0.0)
            return
        }
        if (history.isNotEmpty()) {
            val pt = history.removeLast()
            load(ctx, pt, autoplay = true)
            return
        }
        if (libraryContext.isNotEmpty() && currentLibraryIndex > 0) {
            val pi = currentLibraryIndex - 1
            currentLibraryIndex = pi
            load(ctx, libraryContext[pi], autoplay = true, libraryIndex = pi)
        } else {
            seekToFraction(0.0)
        }
    }

    // ── Queue ops ──
    fun enqueueNext(t: Track) {
        queue.addFirst(t)
        _snapshot.value = _snapshot.value.copy(queueSize = queue.size)
    }

    fun enqueue(t: Track) {
        queue.addLast(t)
        _snapshot.value = _snapshot.value.copy(queueSize = queue.size)
    }

    fun clearQueue() {
        queue.clear()
        _snapshot.value = _snapshot.value.copy(queueSize = 0)
    }

    fun current(): Track? = currentTrack

    // ── Polling ──
    private fun startPolling() {
        stopPolling()
        pollJob = scope.launch {
            while (true) {
                refreshPosition()
                if (_snapshot.value.state != PlayerState.PLAYING) break
                delay(250L)
            }
        }
    }

    private fun stopPolling() { pollJob?.cancel(); pollJob = null }

    private fun refreshPosition() {
        val pos = runCatching { NativeBridge.nativePositionMs() }.getOrElse { 0L }
        val dur = runCatching { NativeBridge.nativeDurationMs() }.getOrElse { 0L }
        val ns  = runCatching { NativeBridge.nativeState() }.getOrElse { 0 }
        val mapped = when (ns) {
            0 -> PlayerState.IDLE; 1 -> PlayerState.PLAYING; 2 -> PlayerState.PAUSED
            3 -> PlayerState.COMPLETED; 4 -> PlayerState.ERROR; else -> PlayerState.IDLE
        }
        _snapshot.value = _snapshot.value.copy(
            positionMs = pos, durationMs = dur, state = mapped)
        if (mapped == PlayerState.COMPLETED) {
            stopPolling()
            next()      // auto-advance on completion
        }
    }
}

package com.skbsakib.audiosuperpower.library

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared now-playing state.
 * Written by NativePlayer, read by LibraryScreen for highlight + auto-scroll.
 */
object PlaybackState {
    private val _currentTrackId = MutableStateFlow<Long>(-1L)
    val currentTrackId: StateFlow<Long> = _currentTrackId.asStateFlow()

    private val _currentTrackUri = MutableStateFlow<String?>(null)
    val currentTrackUri: StateFlow<String?> = _currentTrackUri.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    fun setNowPlaying(id: Long, uri: String?, playing: Boolean) {
        _currentTrackId.value = id
        _currentTrackUri.value = uri
        _isPlaying.value = playing
    }

    fun setPlaying(p: Boolean) {
        _isPlaying.value = p
    }

    fun clear() {
        _currentTrackId.value = -1L
        _currentTrackUri.value = null
        _isPlaying.value = false
    }
}

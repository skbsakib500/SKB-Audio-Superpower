package com.skbsakib.audiosuperpower.ui.overlay

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton state for the full-player overlay.
 *
 * Kept OUTSIDE the NavHost so the mini-player can expand into it without
 * losing the Library screen underneath (scroll position, tab selection,
 * selection highlight all preserved).
 */
object PlayerOverlayState {
    private val _visible = MutableStateFlow(false)
    val visible: StateFlow<Boolean> = _visible.asStateFlow()

    fun open()   { _visible.value = true }
    fun close()  { _visible.value = false }
    fun toggle() { _visible.value = !_visible.value }
}

package com.skbsakib.audiosuperpower.ui.overlay

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skbsakib.audiosuperpower.ui.screens.FullPlayerScreen

/**
 * Full-player overlay. Slides up from bottom on open; slides back down on
 * close. Android back button (and gesture) closes the overlay instead of
 * exiting the app.
 */
@Composable
fun PlayerOverlay() {
    val visible by PlayerOverlayState.visible.collectAsStateWithLifecycle()

    BackHandler(enabled = visible) { PlayerOverlayState.close() }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis = 260)
        ) + fadeIn(animationSpec = tween(180)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(durationMillis = 220)
        ) + fadeOut(animationSpec = tween(160))
    ) {
        FullPlayerScreen(onClose = { PlayerOverlayState.close() })
    }
}

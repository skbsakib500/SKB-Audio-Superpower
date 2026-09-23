package com.skbsakib.audiosuperpower.ui.navigation

/**
 * Typed route model. Replaces the flat `object Routes` string constants.
 *
 * Primary destinations appear in the bottom navigation.
 * Secondary destinations are reached from within screens.
 */
sealed class AppRoute(val route: String) {

    // ── Gate ──
    data object Permissions : AppRoute("permissions")

    // ── Primary (bottom nav) ──
    data object Home        : AppRoute("home")
    data object Library     : AppRoute("library")
    data object Player      : AppRoute("player")     // opens overlay
    data object DspLab      : AppRoute("dsp_lab")
    data object AudioLab    : AppRoute("audio_lab")

    // ── Secondary ──
    data object Settings          : AppRoute("settings")
    data object PlaybackSettings  : AppRoute("playback_settings")
    data object AutoEq            : AppRoute("autoeq")
    data object Spatial           : AppRoute("spatial")
    data object Analyzer          : AppRoute("analyzer")
    data object DeviceLab         : AppRoute("device_lab")
    data object GlobalEffect      : AppRoute("global_effect")
    data object Cloud             : AppRoute("cloud")

    companion object {
        /** Items visible in the bottom navigation, in order. */
        val bottomNavItems: List<AppRoute> = listOf(
            Home, Library, Player, DspLab, AudioLab
        )
    }
}

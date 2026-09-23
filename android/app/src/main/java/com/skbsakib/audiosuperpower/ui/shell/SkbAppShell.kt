package com.skbsakib.audiosuperpower.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.skbsakib.audiosuperpower.settings.SkbSettings
import com.skbsakib.audiosuperpower.ui.navigation.AppRoute
import com.skbsakib.audiosuperpower.ui.overlay.PlayerOverlay
import com.skbsakib.audiosuperpower.ui.overlay.PlayerOverlayState

/**
 * Top-level app shell.
 *
 *   ┌────────────────────────┐
 *   │  NavHost (screens)     │ ← each screen draws its own header
 *   ├────────────────────────┤
 *   │  MiniPlayerBar         │ ← visible when a track is loaded
 *   ├────────────────────────┤
 *   │  SkbBottomBar          │ ← 5 primary items
 *   └────────────────────────┘
 *
 * Full player is an overlay above all of this.
 */
@Composable
fun SkbAppShell(
    settings: SkbSettings,
    onAccentChange: (String) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onVariantChange: (String) -> Unit
) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val openSettings: () -> Unit = {
        nav.navigate(AppRoute.Settings.route) { launchSingleTop = true }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        Column(Modifier.fillMaxSize()) {

            Box(Modifier.weight(1f).fillMaxWidth()) {
                ShellNavGraph(
                    nav = nav,
                    settings = settings,
                    onOpenSettings = openSettings,
                    onOpenPlayer = { PlayerOverlayState.open() },
                    onAccentChange = onAccentChange,
                    onHapticsChange = onHapticsChange,
                    onVariantChange = onVariantChange
                )
            }

            MiniPlayerBar(onOpenPlayer = { PlayerOverlayState.open() })

            SkbBottomBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    if (route == AppRoute.Player) {
                        PlayerOverlayState.open()
                    } else {
                        nav.navigate(route.route) {
                            launchSingleTop = true
                            popUpTo(AppRoute.Home.route) { saveState = true }
                            restoreState = true
                        }
                    }
                },
                onOpenPlayer = { PlayerOverlayState.open() }
            )
        }

        PlayerOverlay()
    }
}

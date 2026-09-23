package com.skbsakib.audiosuperpower.ui.nav

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.skbsakib.audiosuperpower.settings.SkbSettings
import com.skbsakib.audiosuperpower.ui.screens.FullPlayerScreen
import com.skbsakib.audiosuperpower.ui.screens.LibraryScreen
import com.skbsakib.audiosuperpower.ui.screens.PermissionsScreen
import com.skbsakib.audiosuperpower.ui.screens.SettingsScreen

object Routes {
    const val PERMISSIONS = "permissions"
    const val HOME        = "home"
    const val SETTINGS    = "settings"
    const val PLAYER      = "player"
}

@Composable
fun SkbNavHost(
    nav: NavHostController,
    startDestination: String,
    settings: SkbSettings,
    onGrantPermissions: () -> Unit,
    onAccentChange: (String) -> Unit,
    onHapticsChange: (Boolean) -> Unit
) {
    val ctx = LocalContext.current
    val currentRoute = nav.currentBackStackEntry?.destination?.route
    var backPressedTime by remember { mutableLongStateOf(0L) }

    // ── Back / edge-swipe handling ──
    // On HOME or PERMISSIONS → double-tap-to-exit
    // On SETTINGS or PLAYER    → NavHost handles it (pops back to HOME)
    BackHandler(
        enabled = currentRoute == Routes.HOME ||
                  currentRoute == Routes.PERMISSIONS ||
                  currentRoute == null
    ) {
        val now = System.currentTimeMillis()
        if (now - backPressedTime < 2000L) {
            (ctx as? Activity)?.finish()
        } else {
            backPressedTime = now
            Toast.makeText(ctx, "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
    }

    NavHost(navController = nav, startDestination = startDestination) {
        composable(Routes.PERMISSIONS) {
            PermissionsScreen(onAllGranted = onGrantPermissions)
        }
        composable(Routes.HOME) {
            LibraryScreen(
                settings = settings,
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                onOpenPlayer   = { nav.navigate(Routes.PLAYER) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                settings = settings,
                onBack = { nav.popBackStack() },
                onAccentChange = onAccentChange,
                onHapticsChange = onHapticsChange
            )
        }
        composable(Routes.PLAYER) {
            FullPlayerScreen(onClose = { nav.popBackStack() })
        }
    }
}

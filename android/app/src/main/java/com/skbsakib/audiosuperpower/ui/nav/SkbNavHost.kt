package com.skbsakib.audiosuperpower.ui.nav

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.skbsakib.audiosuperpower.settings.SkbSettings
import com.skbsakib.audiosuperpower.ui.overlay.PlayerOverlayState
import com.skbsakib.audiosuperpower.ui.screens.AnalyzerScreen
import com.skbsakib.audiosuperpower.ui.screens.AutoEqScreen
import com.skbsakib.audiosuperpower.ui.screens.DeviceLabScreen
import com.skbsakib.audiosuperpower.ui.screens.DspLabScreen
import com.skbsakib.audiosuperpower.ui.screens.LibraryScreen
import com.skbsakib.audiosuperpower.ui.screens.PermissionsScreen
import com.skbsakib.audiosuperpower.ui.screens.SettingsScreen
import com.skbsakib.audiosuperpower.ui.screens.SpatialLabScreen

object Routes {
    const val PERMISSIONS = "permissions"
    const val HOME        = "home"
    const val SETTINGS    = "settings"
    const val DSP_LAB     = "dsp_lab"
    const val SPATIAL_LAB = "spatial_lab"
    const val DEVICE_LAB  = "device_lab"
    const val AUTOEQ      = "autoeq"
    const val ANALYZER    = "analyzer"
}

/** Lab routes opened from HOME — treated like secondary tabs. */
private val LAB_ROUTES = setOf(
    Routes.DSP_LAB,
    Routes.SPATIAL_LAB,
    Routes.DEVICE_LAB,
    Routes.AUTOEQ,
    Routes.ANALYZER
)

@Composable
fun SkbNavHost(
    nav: NavHostController,
    startDestination: String,
    settings: SkbSettings,
    onGrantPermissions: () -> Unit,
    onAccentChange: (String) -> Unit,
    onHapticsChange: (Boolean) -> Unit
) {

    // ── Reusable transitions ──
    val labEnter = slideInHorizontally(
        initialOffsetX = { it }, animationSpec = tween(240)
    ) + fadeIn(animationSpec = tween(180))
    val labExit = slideOutHorizontally(
        targetOffsetX = { -it / 4 }, animationSpec = tween(240)
    ) + fadeOut(animationSpec = tween(180))
    val labPopEnter = slideInHorizontally(
        initialOffsetX = { -it / 4 }, animationSpec = tween(240)
    ) + fadeIn(animationSpec = tween(180))
    val labPopExit = slideOutHorizontally(
        targetOffsetX = { it }, animationSpec = tween(240)
    ) + fadeOut(animationSpec = tween(180))

    val settingsEnter = slideInVertically(
        initialOffsetY = { it }, animationSpec = tween(260)
    ) + fadeIn(animationSpec = tween(200))
    val settingsExit = slideOutVertically(
        targetOffsetY = { it }, animationSpec = tween(260)
    ) + fadeOut(animationSpec = tween(200))

    // ── Navigate helpers ──
    fun navToLab(route: String) {
        nav.navigate(route) {
            launchSingleTop = true
            popUpTo(Routes.HOME) { saveState = true }
            restoreState = true
        }
    }

    NavHost(
        navController = nav,
        startDestination = startDestination,
        enterTransition = { fadeIn(animationSpec = tween(180)) },
        exitTransition = { fadeOut(animationSpec = tween(180)) }
    ) {
        // ── Permissions ──
        composable(
            Routes.PERMISSIONS,
            exitTransition = { fadeOut(tween(220)) }
        ) {
            PermissionsScreen(onAllGranted = onGrantPermissions)
        }

        // ── HOME (Library) ──
        composable(Routes.HOME) {
            LibraryScreen(
                settings = settings,
                onOpenSettings   = {
                    nav.navigate(Routes.SETTINGS) { launchSingleTop = true }
                },
                onOpenPlayer     = { PlayerOverlayState.open() },
                onOpenDspLab     = { navToLab(Routes.DSP_LAB) },
                onOpenSpatialLab = { navToLab(Routes.SPATIAL_LAB) },
                onOpenDeviceLab  = { navToLab(Routes.DEVICE_LAB) },
                onOpenAutoEq     = { navToLab(Routes.AUTOEQ) },
                onOpenAnalyzer   = { navToLab(Routes.ANALYZER) }
            )
        }

        // ── SETTINGS (slide from bottom) ──
        composable(
            Routes.SETTINGS,
            enterTransition = { settingsEnter },
            exitTransition = { settingsExit },
            popEnterTransition = { settingsEnter },
            popExitTransition = { settingsExit }
        ) {
            SettingsScreen(
                settings = settings,
                onBack = { nav.popBackStack() },
                onAccentChange = onAccentChange,
                onHapticsChange = onHapticsChange
            )
        }

        // ── LABS (slide from right, singleton tabs) ──
        composable(
            Routes.DSP_LAB,
            enterTransition = { labEnter },
            exitTransition = { labExit },
            popEnterTransition = { labPopEnter },
            popExitTransition = { labPopExit }
        ) {
            DspLabScreen(onClose = { nav.popBackStack() })
        }

        composable(
            Routes.SPATIAL_LAB,
            enterTransition = { labEnter },
            exitTransition = { labExit },
            popEnterTransition = { labPopEnter },
            popExitTransition = { labPopExit }
        ) {
            SpatialLabScreen(onClose = { nav.popBackStack() })
        }

        composable(
            Routes.DEVICE_LAB,
            enterTransition = { labEnter },
            exitTransition = { labExit },
            popEnterTransition = { labPopEnter },
            popExitTransition = { labPopExit }
        ) {
            DeviceLabScreen(onClose = { nav.popBackStack() })
        }

        composable(
            Routes.AUTOEQ,
            enterTransition = { labEnter },
            exitTransition = { labExit },
            popEnterTransition = { labPopEnter },
            popExitTransition = { labPopExit }
        ) {
            AutoEqScreen(onClose = { nav.popBackStack() })
        }

        composable(
            Routes.ANALYZER,
            enterTransition = { labEnter },
            exitTransition = { labExit },
            popEnterTransition = { labPopEnter },
            popExitTransition = { labPopExit }
        ) {
            AnalyzerScreen(onClose = { nav.popBackStack() })
        }
    }
}

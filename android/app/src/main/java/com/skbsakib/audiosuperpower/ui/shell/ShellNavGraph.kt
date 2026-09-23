package com.skbsakib.audiosuperpower.ui.shell

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.skbsakib.audiosuperpower.settings.SkbSettings
import com.skbsakib.audiosuperpower.ui.navigation.AppRoute
import com.skbsakib.audiosuperpower.ui.screens.AnalyzerScreen
import com.skbsakib.audiosuperpower.ui.screens.AutoEqScreen
import com.skbsakib.audiosuperpower.ui.screens.CloudSourceScreen
import com.skbsakib.audiosuperpower.ui.screens.DeviceLabScreen
import com.skbsakib.audiosuperpower.ui.screens.DspLabScreen
import com.skbsakib.audiosuperpower.ui.screens.GlobalEffectScreen
import com.skbsakib.audiosuperpower.ui.screens.LibraryScreen
import com.skbsakib.audiosuperpower.ui.screens.PlaybackSettingsScreen
import com.skbsakib.audiosuperpower.ui.screens.SettingsScreen
import com.skbsakib.audiosuperpower.ui.screens.SpatialLabScreen
import com.skbsakib.audiosuperpower.ui.screens.home.HomeScreen
import com.skbsakib.audiosuperpower.ui.screens.lab.AudioLabScreen

@Composable
fun ShellNavGraph(
    nav: NavHostController,
    settings: SkbSettings,
    onOpenSettings: () -> Unit,
    onOpenPlayer: () -> Unit,
    onAccentChange: (String) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onVariantChange: (String) -> Unit
) {
    val labEnter = slideInHorizontally(
        initialOffsetX = { it }, animationSpec = tween(240)
    ) + fadeIn(animationSpec = tween(180))
    val labExit = slideOutHorizontally(
        targetOffsetX = { -it / 4 }, animationSpec = tween(240)
    ) + fadeOut(animationSpec = tween(180))

    NavHost(
        navController = nav,
        startDestination = AppRoute.Home.route,
        enterTransition = { fadeIn(animationSpec = tween(180)) },
        exitTransition = { fadeOut(animationSpec = tween(180)) }
    ) {
        composable(AppRoute.Home.route) {
            HomeScreen(
                onOpenLibrary = { nav.navigate(AppRoute.Library.route) },
                onOpenSettings = onOpenSettings
            )
        }

        composable(AppRoute.Library.route) {
            LibraryScreen(onOpenSettings = onOpenSettings)
        }

        composable(AppRoute.DspLab.route) {
            DspLabScreen(onClose = { nav.popBackStack() })
        }

        composable(AppRoute.AudioLab.route) {
            AudioLabScreen(
                onOpenAutoEq = { nav.navigate(AppRoute.AutoEq.route) },
                onOpenSpatial = { nav.navigate(AppRoute.Spatial.route) },
                onOpenAnalyzer = { nav.navigate(AppRoute.Analyzer.route) },
                onOpenDeviceLab = { nav.navigate(AppRoute.DeviceLab.route) },
                onOpenGlobalEffect = { nav.navigate(AppRoute.GlobalEffect.route) },
                onOpenSettings = onOpenSettings
            )
        }

        composable(AppRoute.Settings.route) {
            SettingsScreen(
                settings = settings,
                onBack = { nav.popBackStack() },
                onAccentChange = onAccentChange,
                onHapticsChange = onHapticsChange,
                onVariantChange = onVariantChange
            )
        }

        composable(AppRoute.PlaybackSettings.route,
            enterTransition = { labEnter }, exitTransition = { labExit }) {
            PlaybackSettingsScreen(onClose = { nav.popBackStack() })
        }

        composable(AppRoute.AutoEq.route,
            enterTransition = { labEnter }, exitTransition = { labExit }) {
            AutoEqScreen(onClose = { nav.popBackStack() })
        }

        composable(AppRoute.Spatial.route,
            enterTransition = { labEnter }, exitTransition = { labExit }) {
            SpatialLabScreen(onClose = { nav.popBackStack() })
        }

        composable(AppRoute.Analyzer.route,
            enterTransition = { labEnter }, exitTransition = { labExit }) {
            AnalyzerScreen(onClose = { nav.popBackStack() })
        }

        composable(AppRoute.DeviceLab.route,
            enterTransition = { labEnter }, exitTransition = { labExit }) {
            DeviceLabScreen(onClose = { nav.popBackStack() })
        }

        composable(AppRoute.GlobalEffect.route,
            enterTransition = { labEnter }, exitTransition = { labExit }) {
            GlobalEffectScreen(onClose = { nav.popBackStack() })
        }

        composable(AppRoute.Cloud.route,
            enterTransition = { labEnter }, exitTransition = { labExit }) {
            CloudSourceScreen(onClose = { nav.popBackStack() })
        }
    }
}

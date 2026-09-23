package com.skbsakib.audiosuperpower.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.skbsakib.audiosuperpower.settings.SkbSettings
import com.skbsakib.audiosuperpower.ui.screens.AutoEqScreen
import com.skbsakib.audiosuperpower.ui.screens.DeviceLabScreen
import com.skbsakib.audiosuperpower.ui.screens.DspLabScreen
import com.skbsakib.audiosuperpower.ui.screens.FullPlayerScreen
import com.skbsakib.audiosuperpower.ui.screens.LibraryScreen
import com.skbsakib.audiosuperpower.ui.screens.PermissionsScreen
import com.skbsakib.audiosuperpower.ui.screens.SettingsScreen
import com.skbsakib.audiosuperpower.ui.screens.SpatialLabScreen

object Routes {
    const val PERMISSIONS = "permissions"
    const val HOME        = "home"
    const val SETTINGS    = "settings"
    const val PLAYER      = "player"
    const val DSP_LAB     = "dsp_lab"
    const val SPATIAL_LAB = "spatial_lab"
    const val DEVICE_LAB  = "device_lab"
    const val AUTOEQ      = "autoeq"
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
    NavHost(navController = nav, startDestination = startDestination) {
        composable(Routes.PERMISSIONS) {
            PermissionsScreen(onAllGranted = onGrantPermissions)
        }
        composable(Routes.HOME) {
            LibraryScreen(
                settings = settings,
                onOpenSettings   = { nav.navigate(Routes.SETTINGS) },
                onOpenPlayer     = { nav.navigate(Routes.PLAYER) },
                onOpenDspLab     = { nav.navigate(Routes.DSP_LAB) },
                onOpenSpatialLab = { nav.navigate(Routes.SPATIAL_LAB) },
                onOpenDeviceLab  = { nav.navigate(Routes.DEVICE_LAB) },
                onOpenAutoEq     = { nav.navigate(Routes.AUTOEQ) }
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
        composable(Routes.PLAYER)      { FullPlayerScreen(onClose = { nav.popBackStack() }) }
        composable(Routes.DSP_LAB)     { DspLabScreen(onClose = { nav.popBackStack() }) }
        composable(Routes.SPATIAL_LAB) { SpatialLabScreen(onClose = { nav.popBackStack() }) }
        composable(Routes.DEVICE_LAB)  { DeviceLabScreen(onClose = { nav.popBackStack() }) }
        composable(Routes.AUTOEQ)      { AutoEqScreen(onClose = { nav.popBackStack() }) }
    }
}

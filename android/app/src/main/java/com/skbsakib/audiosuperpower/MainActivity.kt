package com.skbsakib.audiosuperpower

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.skbsakib.audiosuperpower.device.DeviceProfileController
import com.skbsakib.audiosuperpower.settings.SettingsStore
import com.skbsakib.audiosuperpower.settings.SkbSettings
import com.skbsakib.audiosuperpower.ui.nav.Routes
import com.skbsakib.audiosuperpower.ui.nav.SkbNavHost
import com.skbsakib.audiosuperpower.ui.theme.AccentPalette
import com.skbsakib.audiosuperpower.ui.theme.SkbTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val ctx = LocalContext.current
            val store = remember { SettingsStore(ctx.applicationContext) }
            val settings by store.settings.collectAsStateWithLifecycle(initialValue = SkbSettings())
            val scope = rememberCoroutineScope()

            val hasAudioPerm = remember { mutableStateOf(checkAudioPermission()) }

            // One-shot device auto-tune on cold start
            LaunchedEffect(Unit) {
                DeviceProfileController.autoApply(ctx.applicationContext)
            }

            SkbTheme(accent = AccentPalette.from(settings.accentColor)) {
                Surface(Modifier.fillMaxSize(), color = Color.Black) {
                    val nav = rememberNavController()
                    val start = if (hasAudioPerm.value) Routes.HOME else Routes.PERMISSIONS

                    SkbNavHost(
                        nav = nav,
                        startDestination = start,
                        settings = settings,
                        onGrantPermissions = {
                            hasAudioPerm.value = true
                            nav.navigate(Routes.HOME) {
                                popUpTo(Routes.PERMISSIONS) { inclusive = true }
                            }
                        },
                        onAccentChange = { scope.launch { store.setAccent(it) } },
                        onHapticsChange = { scope.launch { store.setHaptics(it) } }
                    )
                }
            }
        }
    }

    private fun checkAudioPermission(): Boolean {
        val perm = if (Build.VERSION.SDK_INT >= 33)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE
        return ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
    }
}

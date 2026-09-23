package com.skbsakib.audiosuperpower

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.skbsakib.audiosuperpower.autoeq.AutoEqController
import com.skbsakib.audiosuperpower.playback.CrossfadeController
import com.skbsakib.audiosuperpower.settings.SettingsStore
import com.skbsakib.audiosuperpower.settings.SkbSettings
import com.skbsakib.audiosuperpower.ui.nav.Routes
import com.skbsakib.audiosuperpower.ui.nav.SkbNavHost
import com.skbsakib.audiosuperpower.ui.overlay.PlayerOverlay
import com.skbsakib.audiosuperpower.ui.theme.AccentPalette
import com.skbsakib.audiosuperpower.ui.theme.SkbTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // ── System splash (Android 12+ / core-splashscreen fallback) ──
        val splash = installSplashScreen()

        super.onCreate(savedInstanceState)

        // Keep the system splash visible until our first composition is ready.
        var keepSplash = true
        splash.setKeepOnScreenCondition { keepSplash }

        setContent {
            val ctx = LocalContext.current
            val store = remember { SettingsStore(ctx.applicationContext) }
            val settings by store.settings.collectAsStateWithLifecycle(initialValue = SkbSettings())
            val scope = rememberCoroutineScope()

            val hasAudioPerm = remember { mutableStateOf(checkAudioPermission()) }

            // Intro animation state (runs once per cold start)
            var showIntro by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                AutoEqController.init(ctx.applicationContext)
                CrossfadeController.init(ctx.applicationContext)
                // Let the composable settle before hiding system splash
                delay(60)
                keepSplash = false
                // Intro overlay: total ~900ms
                delay(900)
                showIntro = false
            }

            SkbTheme(accent = AccentPalette.from(settings.accentColor)) {
                Surface(Modifier.fillMaxSize(), color = Color.Black) {

                    // ── Main app (rendered under the intro overlay) ──
                    val nav = rememberNavController()
                    val start = if (hasAudioPerm.value) Routes.HOME else Routes.PERMISSIONS

                    Box(Modifier.fillMaxSize()) {
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
                        PlayerOverlay()
                    }

                    // ── Animated intro overlay (2080 lab entry) ──
                    AnimatedVisibility(
                        visible = showIntro,
                        enter = fadeIn(animationSpec = tween(200)),
                        exit = fadeOut(animationSpec = tween(340)) +
                               scaleIn(
                                   initialScale = 1.0f,
                                   targetScale = 1.08f,
                                   animationSpec = tween(340, easing = LinearEasing)
                               )
                    ) {
                        IntroOverlay()
                    }
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

/**
 * Minimal animated intro overlay — fades in the logo, sweeps a cyan scanline
 * across the screen, then fades out. ~900ms total.
 */
@Composable
private fun IntroOverlay() {
    var phase by remember { mutableStateOf(0f) }
    val anim by animateFloatAsState(
        targetValue = phase,
        animationSpec = tween(durationMillis = 900, easing = LinearEasing),
        label = "intro-sweep"
    )
    LaunchedEffect(Unit) { phase = 1f }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF050810), Color(0xFF0A1220), Color(0xFF050810))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "SKB AUDIO",
                color = Color(0xFF00E5FF).copy(alpha = anim.coerceIn(0f, 1f)),
                fontSize = 12.sp,
                letterSpacing = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Light
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "SUPERPOWER",
                color = Color.White.copy(alpha = anim.coerceIn(0f, 1f)),
                fontSize = 34.sp,
                letterSpacing = 4.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "2080 AUDIO LAB",
                color = Color(0xFF7A8FA6).copy(alpha = anim.coerceIn(0f, 1f)),
                fontSize = 11.sp,
                letterSpacing = 6.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Bottom scanline
        Box(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp)
        ) {
            Spacer(
                Modifier
                    .align(Alignment.Center)
                    .width(2.dp)
                    .height(120.dp)
                    .background(Color(0xFF00E5FF).copy(alpha = (1f - anim).coerceIn(0f, 0.6f)))
            )
        }
    }
}

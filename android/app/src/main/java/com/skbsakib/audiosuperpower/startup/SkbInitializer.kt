package com.skbsakib.audiosuperpower.startup

import android.content.Context
import androidx.startup.Initializer
import com.skbsakib.audiosuperpower.NativeBridge
import com.skbsakib.audiosuperpower.device.DeviceProfileController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Runs at process start, BEFORE the first frame.
 *
 * Keeps it short:
 *   - Force-load native lib (so first JNI call is instant)
 *   - Kick off device auto-tune on a background coroutine (non-blocking)
 *
 * Everything else (AutoEQ init, media scan) happens on first composition.
 */
class SkbInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        // 1. Load native library on main thread — blocking ~5-20ms but ensures
        //    the first JNI call from UI is instant (no jank on first play).
        runCatching { NativeBridge.nativeVersion() }

        // 2. Kick off device profile detect in background
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            runCatching { DeviceProfileController.autoApply(context.applicationContext) }
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}

package com.skbsakib.audiosuperpower

/**
 * JNI bridge to skb_audio_core.
 *
 * Native state (player, buffers, stream) lives in C++ — this object
 * is a thin stateless façade.
 */
object NativeBridge {

    init {
        System.loadLibrary("skb_audio_core")
    }

    // ── Version ──
    external fun nativeVersion(): String

    // ── Playback control ──
    external fun nativeLoadWav(path: String): Boolean
    external fun nativePlay(): Boolean
    external fun nativePause()
    external fun nativeStop()
    external fun nativeSeek(fraction: Double)

    // ── Status ──
    external fun nativePositionMs(): Long
    external fun nativeDurationMs(): Long
    external fun nativeState(): Int          // 0=Idle 1=Playing 2=Paused 3=Completed 4=Error
    external fun nativeSourceInfo(): String  // "48000 Hz | 2 ch | 24 bit"
    external fun nativeDeviceInfo(): String  // "48000 Hz | 2 ch"
}

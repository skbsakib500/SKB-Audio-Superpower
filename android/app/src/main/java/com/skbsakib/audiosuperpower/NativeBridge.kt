package com.skbsakib.audiosuperpower

object NativeBridge {

    init {
        System.loadLibrary("skb_audio_core")
    }

    // Version
    external fun nativeVersion(): String

    // Playback
    external fun nativeLoadWav(path: String): Boolean
    external fun nativePlay(): Boolean
    external fun nativePause()
    external fun nativeStop()
    external fun nativeSeek(fraction: Double)

    // Status
    external fun nativePositionMs(): Long
    external fun nativeDurationMs(): Long
    external fun nativeState(): Int
    external fun nativeSourceInfo(): String
    external fun nativeDeviceInfo(): String

    // DSP
    external fun nativeSetDspEnabled(enabled: Boolean)
    external fun nativeSetPreamp(db: Float)
    external fun nativeSetEqBand(index: Int, db: Float)
    external fun nativeSetBass(db: Float)
    external fun nativeSetTreble(db: Float)
    external fun nativeSetLimiterEnabled(enabled: Boolean)
    external fun nativeSetLimiterCeiling(db: Float)
    external fun nativeResetDsp()

    // Spatial
    external fun nativeSetSpatialEnabled(enabled: Boolean)
    external fun nativeSetSpatialMode(mode: Int)
    external fun nativeSetSpatialHeight(height: Float)
    external fun nativeSetSpatialRoom(room: Int)
    external fun nativeSetSpatialIntensity(intensity: Float)
    external fun nativeResetSpatial()
}

package com.skbsakib.audiosuperpower

object NativeBridge {

    init {
        System.loadLibrary("skb_audio_core")
    
    // ReplayGain
    external fun nativeSetReplayGainEnabled(enabled: Boolean)
    external fun nativeSetReplayGainTargetDb(db: Float)
    external fun nativeSetReplayGainPeakDb(peakDb: Float)
    external fun nativeReplayGainCurrentDb(): Float
    external fun nativeReplayGainMeasuredPeakDb(): Float
    external fun nativeComputeLoadedPeakDb(): Float
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


    // AutoEQ
    external fun nativeSetAutoEqEnabled(enabled: Boolean)
    external fun nativeAutoEqClear()
    external fun nativeAutoEqSetPreamp(db: Float)
    external fun nativeAutoEqAddFilter(type: Int, freq: Float, q: Float, gainDb: Float): Boolean

    // Analyzer
    external fun nativeSetAnalyzerEnabled(enabled: Boolean)
    external fun nativeAnalyzerTick()
    external fun nativeAnalyzerBins(): Int
    external fun nativeAnalyzerReadSpectrum(): FloatArray
    external fun nativeAnalyzerTakePeak(): Float
    external fun nativeAnalyzerTakeRmsDb(): Float
    external fun nativeAnalyzerIsClipping(): Boolean

    // Crossfade / Gapless
    external fun nativeSetCrossfadeMs(ms: Int)
    external fun nativeCrossfadeMs(): Int
    external fun nativeLoadNext(path: String): Boolean
    external fun nativeClearNext()

    // Spatial
    external fun nativeSetSpatialEnabled(enabled: Boolean)
    external fun nativeSetSpatialMode(mode: Int)
    external fun nativeSetSpatialHeight(height: Float)
    external fun nativeSetSpatialRoom(room: Int)
    external fun nativeSetSpatialIntensity(intensity: Float)
    external fun nativeResetSpatial()
}

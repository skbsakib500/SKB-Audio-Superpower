package com.skbsakib.audiosuperpower.device

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log

/**
 * Detects device audio capabilities and derives an auto-tuned audio profile.
 *
 * Rootless, honest: we read what Android exposes and apply only what the
 * platform actually allows. No fake system-level modification.
 */
object DeviceProfile {

    private const val TAG = "SKB-DeviceProfile"

    // ─────────────────────────────────────────────────────
    //  Snapshot: raw detected capabilities
    // ─────────────────────────────────────────────────────
    data class Snapshot(
        // SoC / hardware
        val socModel: String,
        val socManufacturer: String,
        val hardware: String,
        val board: String,
        val deviceModel: String,
        val deviceManufacturer: String,
        val abi: String,
        val androidVersion: String,
        val apiLevel: Int,

        // Audio
        val nativeSampleRate: Int,
        val nativeFramesPerBuffer: Int,
        val lowLatencyCapable: Boolean,
        val proAudioCapable: Boolean,
        val outputDevice: String,       // SPEAKER / WIRED_HEADPHONES / BLUETOOTH_A2DP / USB_DEVICE / BLE_HEADSET / ...
        val maxChannelCount: Int        // 0 if unknown

    ) {
        val summary: String
            get() = buildString {
                append("$deviceManufacturer $deviceModel\n")
                append("SoC: $socManufacturer $socModel\n")
                append("HW: $hardware  Board: $board\n")
                append("ABI: $abi   Android $androidVersion (API $apiLevel)\n")
                append("Native: ${nativeSampleRate} Hz, ${nativeFramesPerBuffer} frames\n")
                append("LowLatency: $lowLatencyCapable   ProAudio: $proAudioCapable\n")
                append("Output: $outputDevice")
            }
    }

    // ─────────────────────────────────────────────────────
    //  AutoTune: what we'll actually apply
    // ─────────────────────────────────────────────────────
    data class AutoTune(
        val dspEnabled: Boolean,
        val preampDb: Float,
        val bassDb: Float,
        val trebleDb: Float,
        val limiterCeilingDb: Float,
        val spatialModeId: Int,          // 0=OFF, 3..10
        val spatialIntensity: Float,
        val spatialHeight: Float,
        val spatialRoomId: Int,
        val reason: String               // human-readable rationale
    )

    // ─────────────────────────────────────────────────────
    //  Detection
    // ─────────────────────────────────────────────────────
    fun detect(context: Context): Snapshot {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val pm = context.packageManager

        val nativeRate = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
            ?.toIntOrNull() ?: 48000
        val nativeBuffer = am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
            ?.toIntOrNull() ?: 256

        val lowLatency = pm.hasSystemFeature(PackageManager.FEATURE_AUDIO_LOW_LATENCY)
        val proAudio = if (Build.VERSION.SDK_INT >= 24)
            pm.hasSystemFeature(PackageManager.FEATURE_AUDIO_PRO) else false

        // Output device classification
        val outputDevice = detectOutputDevice(am)

        // SoC info (Android 12+)
        val socModel = if (Build.VERSION.SDK_INT >= 31) Build.SOC_MODEL else ""
        val socMfg   = if (Build.VERSION.SDK_INT >= 31) Build.SOC_MANUFACTURER else ""

        val maxCh = maxOutputChannelCount(am)

        return Snapshot(
            socModel = socModel.ifBlank { "unknown" },
            socManufacturer = socMfg.ifBlank { "unknown" },
            hardware = Build.HARDWARE ?: "unknown",
            board = Build.BOARD ?: "unknown",
            deviceModel = Build.MODEL ?: "unknown",
            deviceManufacturer = Build.MANUFACTURER ?: "unknown",
            abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",
            androidVersion = Build.VERSION.RELEASE ?: "unknown",
            apiLevel = Build.VERSION.SDK_INT,
            nativeSampleRate = nativeRate,
            nativeFramesPerBuffer = nativeBuffer,
            lowLatencyCapable = lowLatency,
            proAudioCapable = proAudio,
            outputDevice = outputDevice,
            maxChannelCount = maxCh
        )
    }

    private fun detectOutputDevice(am: AudioManager): String {
        if (Build.VERSION.SDK_INT < 23) return "UNKNOWN"
        val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        // Priority: prefer currently active wired/BT/USB over speaker
        val priority = listOf(
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_HDMI,
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        )
        for (type in priority) {
            val found = devices.firstOrNull { it.type == type }
            if (found != null) return deviceTypeName(type)
        }
        return devices.firstOrNull()?.let { deviceTypeName(it.type) } ?: "UNKNOWN"
    }

    private fun deviceTypeName(t: Int): String = when (t) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER    -> "SPEAKER"
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES   -> "WIRED_HEADPHONES"
        AudioDeviceInfo.TYPE_WIRED_HEADSET      -> "WIRED_HEADSET"
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP     -> "BLUETOOTH_A2DP"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO      -> "BLUETOOTH_SCO"
        AudioDeviceInfo.TYPE_BLE_HEADSET        -> "BLE_HEADSET"
        AudioDeviceInfo.TYPE_BLE_SPEAKER        -> "BLE_SPEAKER"
        AudioDeviceInfo.TYPE_USB_DEVICE         -> "USB_DEVICE"
        AudioDeviceInfo.TYPE_USB_HEADSET        -> "USB_HEADSET"
        AudioDeviceInfo.TYPE_HDMI               -> "HDMI"
        else                                    -> "TYPE_$t"
    }

    private fun maxOutputChannelCount(am: AudioManager): Int {
        if (Build.VERSION.SDK_INT < 23) return 0
        return am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .maxOfOrNull { it.channelCounts.maxOrNull() ?: 0 } ?: 0
    }

    // ─────────────────────────────────────────────────────
    //  Auto-tune logic
    // ─────────────────────────────────────────────────────
    fun derive(s: Snapshot): AutoTune {
        val reasons = mutableListOf<String>()

        // Start from neutral
        var preamp = 0f
        var bass = 0f
        var treble = 0f
        var limiterCeil = -0.3f
        var spatialModeId = 0       // OFF by default
        var spatialIntensity = 0.7f
        var spatialHeight = 0.5f
        var spatialRoomId = 1       // HALL

        // SoC tuning
        val socLower = (s.socManufacturer + " " + s.socModel).lowercase()
        when {
            "mediatek" in socLower || "mt" in socLower -> {
                preamp = -1.0f; limiterCeil = -0.5f
                reasons += "MediaTek: conservative headroom"
            }
            "qualcomm" in socLower || "snapdragon" in socLower || "sm" in socLower -> {
                preamp = -0.5f; limiterCeil = -0.3f
                reasons += "Snapdragon: standard headroom"
            }
            "exynos" in socLower || "samsung" in socLower -> {
                preamp = -0.8f; limiterCeil = -0.4f
                reasons += "Exynos: moderate headroom"
            }
            "google" in socLower || "tensor" in socLower -> {
                preamp = -0.5f; limiterCeil = -0.3f
                reasons += "Tensor: standard headroom"
            }
            else -> reasons += "Generic SoC: neutral profile"
        }

        // Output-device tuning
        when (s.outputDevice) {
            "SPEAKER" -> {
                bass = 2.5f; treble = 1.0f
                spatialModeId = 3; spatialIntensity = 0.5f
                reasons += "Speaker: bass boost + light width"
            }
            "WIRED_HEADPHONES", "WIRED_HEADSET" -> {
                bass = 1.0f; treble = 1.5f
                spatialModeId = 6; spatialIntensity = 0.7f; spatialHeight = 0.5f
                reasons += "Wired: crossfeed + height cue"
            }
            "BLUETOOTH_A2DP", "BLUETOOTH_SCO" -> {
                // BT already has latency; gentler spatial
                bass = 1.5f; treble = 0.5f
                spatialModeId = 4; spatialIntensity = 0.5f
                reasons += "Bluetooth: gentle width (BT latency aware)"
            }
            "BLE_HEADSET", "BLE_SPEAKER" -> {
                bass = 1.0f; treble = 0.5f
                spatialModeId = 4; spatialIntensity = 0.5f
                reasons += "BLE: gentle profile"
            }
            "USB_DEVICE", "USB_HEADSET" -> {
                // External DAC: go best-quality, minimal processing
                preamp = 0f; bass = 0f; treble = 0f
                limiterCeil = -0.1f
                spatialModeId = 3; spatialIntensity = 0.4f
                reasons += "USB DAC: transparent path + subtle width"
            }
            "HDMI" -> {
                spatialModeId = 0
                reasons += "HDMI: no processing"
            }
            else -> reasons += "Unknown output: neutral"
        }

        // Low-latency capable → allow small preamp headroom recovery
        if (s.lowLatencyCapable) {
            preamp += 0.2f
            reasons += "LowLatency capable"
        }

        return AutoTune(
            dspEnabled = true,
            preampDb = preamp.coerceIn(-6f, 6f),
            bassDb = bass.coerceIn(-6f, 6f),
            trebleDb = treble.coerceIn(-6f, 6f),
            limiterCeilingDb = limiterCeil.coerceIn(-3f, 0f),
            spatialModeId = spatialModeId,
            spatialIntensity = spatialIntensity,
            spatialHeight = spatialHeight,
            spatialRoomId = spatialRoomId,
            reason = reasons.joinToString(" · ")
        )
    }
}

package com.skbsakib.audiosuperpower.global

import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.util.Log

/**
 * Best-effort global audio effects attached to AudioEffect session 0
 * (the "global mix" session).
 *
 * ⚠️ HONEST SCOPE ⚠️
 * Android's AudioEffect system is per-session. Session 0 is a *hint* that
 * vendor implementations MAY treat as "apply to all playback". Many devices
 * silently ignore it. This controller probes each effect on attach:
 *
 *   - attach → check getEnabled() / getStrengthSupported()
 *   - if attach or enable throws → mark UNSUPPORTED
 *   - if attach succeeds but effect reports disabled after enable() → LIMITED
 *   - otherwise → SUPPORTED
 *
 * We NEVER claim success we didn't actually verify. The Device Lab shows
 * per-effect status honestly.
 *
 * This controller is entirely optional — our own playback DSP path
 * (session-specific, on our own stream) always works regardless.
 */
object GlobalEffectController {

    private const val TAG = "SKB-GlobalEffect"
    private const val GLOBAL_SESSION = 0

    enum class Status { UNKNOWN, SUPPORTED, LIMITED, UNSUPPORTED }

    data class EffectStatus(
        val name: String,
        val status: Status,
        val detail: String = ""
    )

    data class GlobalState(
        val probed: Boolean = false,
        val effects: List<EffectStatus> = emptyList()
    )

    private var bassBoost: BassBoost? = null
    private var equalizer: Equalizer? = null
    private var virtualizer: Virtualizer? = null
    private var loudness: LoudnessEnhancer? = null

    @Volatile private var state = GlobalState()
    fun current(): GlobalState = state

    /** Probe each effect. Safe to call multiple times; replaces prior state. */
    @Synchronized
    fun probe(): GlobalState {
        release()
        val out = mutableListOf<EffectStatus>()
        out += probeBassBoost()
        out += probeEqualizer()
        out += probeVirtualizer()
        out += probeLoudness()
        state = GlobalState(probed = true, effects = out)
        Log.i(TAG, "probe result: " + out.joinToString { "${it.name}=${it.status}" })
        return state
    }

    private fun probeBassBoost(): EffectStatus {
        return try {
            val bb = BassBoost(0, GLOBAL_SESSION)
            val strengthSupported = bb.strengthSupported
            try {
                bb.enabled = true
                val actuallyOn = bb.enabled
                if (!actuallyOn) {
                    runCatching { bb.release() }
                    EffectStatus("BassBoost", Status.LIMITED,
                        "enabled=false after enable()")
                } else {
                    bassBoost = bb
                    EffectStatus("BassBoost", Status.SUPPORTED,
                        if (strengthSupported) "strength supported" else "fixed strength")
                }
            } catch (t: Throwable) {
                runCatching { bb.release() }
                EffectStatus("BassBoost", Status.UNSUPPORTED, t.message ?: "")
            }
        } catch (t: Throwable) {
            EffectStatus("BassBoost", Status.UNSUPPORTED, t.message ?: "attach failed")
        }
    }

    private fun probeEqualizer(): EffectStatus {
        return try {
            val eq = Equalizer(0, GLOBAL_SESSION)
            try {
                eq.enabled = true
                val actuallyOn = eq.enabled
                if (!actuallyOn) {
                    runCatching { eq.release() }
                    EffectStatus("Equalizer", Status.LIMITED,
                        "enabled=false after enable()")
                } else {
                    equalizer = eq
                    val bands = eq.numberOfBands.toInt()
                    EffectStatus("Equalizer", Status.SUPPORTED, "$bands bands")
                }
            } catch (t: Throwable) {
                runCatching { eq.release() }
                EffectStatus("Equalizer", Status.UNSUPPORTED, t.message ?: "")
            }
        } catch (t: Throwable) {
            EffectStatus("Equalizer", Status.UNSUPPORTED, t.message ?: "attach failed")
        }
    }

    private fun probeVirtualizer(): EffectStatus {
        return try {
            val v = Virtualizer(0, GLOBAL_SESSION)
            try {
                v.enabled = true
                val actuallyOn = v.enabled
                if (!actuallyOn) {
                    runCatching { v.release() }
                    EffectStatus("Virtualizer", Status.LIMITED,
                        "enabled=false after enable()")
                } else {
                    virtualizer = v
                    EffectStatus("Virtualizer", Status.SUPPORTED,
                        if (v.strengthSupported) "strength supported" else "fixed")
                }
            } catch (t: Throwable) {
                runCatching { v.release() }
                EffectStatus("Virtualizer", Status.UNSUPPORTED, t.message ?: "")
            }
        } catch (t: Throwable) {
            EffectStatus("Virtualizer", Status.UNSUPPORTED, t.message ?: "attach failed")
        }
    }

    private fun probeLoudness(): EffectStatus {
        return try {
            val le = LoudnessEnhancer(GLOBAL_SESSION)
            try {
                le.enabled = true
                val actuallyOn = le.enabled
                if (!actuallyOn) {
                    runCatching { le.release() }
                    EffectStatus("LoudnessEnhancer", Status.LIMITED,
                        "enabled=false after enable()")
                } else {
                    loudness = le
                    EffectStatus("LoudnessEnhancer", Status.SUPPORTED)
                }
            } catch (t: Throwable) {
                runCatching { le.release() }
                EffectStatus("LoudnessEnhancer", Status.UNSUPPORTED, t.message ?: "")
            }
        } catch (t: Throwable) {
            EffectStatus("LoudnessEnhancer", Status.UNSUPPORTED, t.message ?: "attach failed")
        }
    }

    /** Detach all effects. Called on shutdown / disable. */
    @Synchronized
    fun release() {
        runCatching { bassBoost?.release() }; bassBoost = null
        runCatching { equalizer?.release() }; equalizer = null
        runCatching { virtualizer?.release() }; virtualizer = null
        runCatching { loudness?.release() }; loudness = null
    }
}

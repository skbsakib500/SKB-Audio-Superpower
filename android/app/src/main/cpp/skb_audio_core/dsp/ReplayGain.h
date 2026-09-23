#pragma once
#include <atomic>
#include <cmath>
#include <cstdint>

namespace skb::dsp {

/**
 * Peak-based gain normalization (ReplayGain-style, simplified).
 *
 * Real ReplayGain uses ITU-R BS.1770 loudness on tagged metadata. Android's
 * MediaMetadataRetriever does not expose RG tags, so we fall back to peak
 * detection:
 *
 *   1. Pre-scan pass (offline) measures the track's sample peak.
 *   2. targetPeak_dB (default -1.0 dBFS) - measuredPeak_dB = gain_dB.
 *   3. Applied as a smooth preamp at the head of the DSP chain.
 *
 * Peak-based is less perceptually accurate than loudness-based, but it
 * prevents clipping and gives consistent headroom across the library.
 *
 * A K-weighted BS.1770 pass will replace this in Phase 10B.
 */
class ReplayGain {
public:
    void setEnabled(bool e) {
        enabled_.store(e, std::memory_order_release);
        if (!e) reset();
    }

    /** Set the target peak in dBFS (usually -1.0 for safety). */
    void setTargetPeakDb(float db) {
        if (db > 0.0f) db = 0.0f;
        if (db < -12.0f) db = -12.0f;
        targetPeakDb_.store(db, std::memory_order_release);
        targetLin_.store(std::pow(10.0f, db / 20.0f), std::memory_order_release);
    }

    /**
     * Feed a sample-block peak value (from a decode-time pre-scan).
     * Typically called once per track, before playback.
     */
    void setMeasuredPeak(float peak) {
        if (peak < 1e-9f) peak = 1e-9f;
        measuredLin_.store(peak, std::memory_order_release);
        // Compute desired gain so measured peak → target peak.
        const float target = targetLin_.load(std::memory_order_acquire);
        float gain = target / peak;               // linear
        if (gain > 4.0f) gain = 4.0f;             // +12 dB cap
        if (gain < 0.0316f) gain = 0.0316f;       // -30 dB cap
        desiredGainLin_.store(gain, std::memory_order_release);
        desiredGainDb_.store(20.0f * std::log10(gain), std::memory_order_release);
    }

    /** Convenience: from a peak in dBFS. */
    void setMeasuredPeakDb(float peakDb) {
        setMeasuredPeak(std::pow(10.0f, peakDb / 20.0f));
    }

    /** Set the gain directly (dB). Used by LUFS-based RG. */
    void setDirectGainDb(float db) {
        if (db > 12.0f) db = 12.0f;
        if (db < -30.0f) db = -30.0f;
        const float lin = std::pow(10.0f, db / 20.0f);
        desiredGainLin_.store(lin, std::memory_order_release);
        desiredGainDb_.store(db, std::memory_order_release);
    }

    void reset() {
        measuredLin_.store(1.0f, std::memory_order_release);
        desiredGainLin_.store(1.0f, std::memory_order_release);
        desiredGainDb_.store(0.0f, std::memory_order_release);
        currentGain_ = 1.0f;
    }

    void setSampleRate(float sr) {
        // 20 ms smoothing time-constant
        smoothCoef_ = std::exp(-1.0f / (0.020f * sr));
        sr_ = sr;
    }

    /** Current smoothed gain in linear domain (for UI readout). */
    float currentGainLin() const { return currentGain_; }
    float currentGainDb()  const {
        if (currentGain_ < 1e-9f) return -120.0f;
        return 20.0f * std::log10(currentGain_);
    }

    float desiredGainDb() const { return desiredGainDb_.load(std::memory_order_acquire); }
    float measuredPeakDb() const {
        const float p = measuredLin_.load(std::memory_order_acquire);
        return 20.0f * std::log10(p + 1e-9f);
    }

    /** Realtime: apply gain to a mono/stereo pair. No allocation. */
    inline void process(float& l, float& r) {
        if (!enabled_.load(std::memory_order_relaxed)) return;
        const float target = desiredGainLin_.load(std::memory_order_relaxed);

        // One-pole smoothing toward target
        currentGain_ = smoothCoef_ * currentGain_ + (1.0f - smoothCoef_) * target;

        l *= currentGain_;
        r *= currentGain_;
    }

private:
    std::atomic<bool>  enabled_{false};
    std::atomic<float> targetPeakDb_{-1.0f};
    std::atomic<float> targetLin_{0.891f};        // -1 dB
    std::atomic<float> measuredLin_{1.0f};
    std::atomic<float> desiredGainLin_{1.0f};
    std::atomic<float> desiredGainDb_{0.0f};

    float sr_ = 48000.0f;
    float smoothCoef_ = 0.95f;
    float currentGain_ = 1.0f;
};

} // namespace skb::dsp

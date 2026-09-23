#pragma once
#include <atomic>
#include <cmath>
#include <vector>
#include <algorithm>
#include "Fft.h"

namespace skb::analyzer {

/**
 * Analyzer: writes spectrum + meters from audio callback, reads from UI
 * thread via JNI. Double-buffered spectrum, atomic meters.
 *
 * "RMS dBFS" here is short-term RMS, not full ITU-R BS.1770 LUFS
 * (K-weighting filter deferred). Labels reflect actual measurement.
 */
class Analyzer {
public:
    static constexpr int FFT_BINS = 512;
    static constexpr int FFT_SIZE = 1024;

    Analyzer() : fft_(10) {
        window_.resize(FFT_SIZE);
        for (int i = 0; i < FFT_SIZE; ++i) {
            window_[i] = 0.5f * (1.0f - std::cos(2.0f * float(M_PI) * i / (FFT_SIZE - 1)));
        }
        bufA_.assign(FFT_BINS, 0.0f);
        bufB_.assign(FFT_BINS, 0.0f);
        fftWork_.assign(FFT_BINS, 0.0f);
        ring_.assign(FFT_SIZE, 0.0f);
    }

    void setSampleRate(float sr) { sampleRate_ = sr; }
    void enable(bool e) { enabled_.store(e, std::memory_order_release); }

    inline void pushSample(float mono) {
        if (!enabled_.load(std::memory_order_relaxed)) return;

        const float absv = std::fabs(mono);
        const float prev = peak_.load(std::memory_order_relaxed);
        if (absv > prev) peak_.store(absv, std::memory_order_relaxed);

        sumSq_.fetch_add(static_cast<double>(mono) * mono, std::memory_order_relaxed);
        samplesCount_.fetch_add(1, std::memory_order_relaxed);

        ring_[ringWrite_] = mono;
        ringWrite_ = (ringWrite_ + 1) % FFT_SIZE;
        if (ringWrite_ == 0) needFft_.store(true, std::memory_order_release);
    }

    void maybeRunFft() {
        if (!needFft_.exchange(false)) return;

        for (int i = 0; i < FFT_SIZE; ++i) {
            const int idx = (ringWrite_ + i) % FFT_SIZE;
            workFrame_[i] = ring_[idx] * window_[i];
        }
        fft_.forward(workFrame_.data());
        fft_.magnitudes(fftWork_.data());

        const int b = FFT_BINS;
        if (writeToA_.load(std::memory_order_acquire)) {
            for (int i = 0; i < b; ++i) bufA_[i] = fftWork_[i];
            writeToA_.store(false, std::memory_order_release);
        } else {
            for (int i = 0; i < b; ++i) bufB_[i] = fftWork_[i];
            writeToA_.store(true, std::memory_order_release);
        }
    }

    void readSpectrum(float* out, int count) const {
        const int n = std::min(count, FFT_BINS);
        const bool fromA = !writeToA_.load(std::memory_order_acquire);
        const auto& src = fromA ? bufA_ : bufB_;
        for (int i = 0; i < n; ++i) out[i] = src[i];
    }

    float takePeak() {
        return peak_.exchange(0.0f, std::memory_order_acq_rel);
    }

    float takeRms() {
        const int64_t n = samplesCount_.exchange(0, std::memory_order_acq_rel);
        const double s = sumSq_.exchange(0.0, std::memory_order_acq_rel);
        if (n <= 0) return 0.0f;
        return static_cast<float>(std::sqrt(s / static_cast<double>(n)));
    }

    /** Short-term RMS in dBFS. Not full LUFS (K-weighting not applied). */
    float takeRmsDb() {
        const float r = takeRms();
        if (r < 1e-9f) return -70.0f;
        return 20.0f * std::log10(r);
    }

    bool isClipping() const {
        return peak_.load(std::memory_order_relaxed) >= 0.999f;
    }

private:
    Fft fft_;
    std::vector<float> window_;
    std::vector<float> ring_;
    std::vector<float> workFrame_ = std::vector<float>(FFT_SIZE, 0.0f);
    std::vector<float> fftWork_;
    std::vector<float> bufA_, bufB_;
    int ringWrite_ = 0;
    std::atomic<bool> needFft_{false};
    std::atomic<bool> writeToA_{true};
    std::atomic<bool> enabled_{false};
    float sampleRate_ = 48000.0f;

    std::atomic<float>   peak_{0.0f};
    std::atomic<double>  sumSq_{0.0};
    std::atomic<int64_t> samplesCount_{0};
};

} // namespace skb::analyzer

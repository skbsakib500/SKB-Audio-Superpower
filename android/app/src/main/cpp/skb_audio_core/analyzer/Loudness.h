#pragma once
#include <atomic>
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <vector>

#include "../dsp/Biquad.h"

namespace skb::analyzer {

/**
 * ITU-R BS.1770-4 loudness measurement (integrated LUFS).
 *
 * Pipeline:
 *   1. K-weighting pre-filter (two stages):
 *       stage 1: high-shelf @ ~1681 Hz, +4 dB
 *       stage 2: RLB high-pass @ ~38 Hz
 *   2. Compute mean square per 400 ms block (75% overlap optional; we do
 *      non-overlapping for simplicity, spec allows).
 *   3. Convert to LUFS per block:  -0.691 + 10 * log10( meanSquare )
 *   4. Gating:
 *       absolute: keep blocks with LUFS >= -70
 *       relative: keep blocks with LUFS >= (ungated mean - 10)
 *   5. Integrated LUFS = -0.691 + 10 * log10( mean of kept blocks' MS )
 *
 * Offline, one-shot use. NOT realtime-safe (uses vector resize) — call
 * from a background thread. Live per-block measurement can be added later.
 */
class Loudness {
public:
    struct Result {
        float integratedLufs = -70.0f;
        float loudnessRange = 0.0f;    // LU (approx LRA via spread)
        float truePeakDb = -120.0f;    // max sample peak in dBFS
        int blocksMeasured = 0;
        int blocksKept = 0;
        bool ok = false;
    };

    void setSampleRate(float sr) {
        sr_ = sr;
        // ── BS.1770 K-weighting ──
        // Stage 1: high-shelf @ 1681.974450955533 Hz, +3.999843853973347 dB
        //          Q = 0.7071752369554196
        // Stage 2: high-pass @ 38.13547087602444 Hz
        //          Q = 0.5003270373238773
        preL_.setHighShelf(sr_, 1681.974450955533f, 0.7071752f, 3.999843853973347f);
        preR_.setHighShelf(sr_, 1681.974450955533f, 0.7071752f, 3.999843853973347f);
        hpL_.setHighPass(sr_, 38.13547087602444f, 0.5003270f);
        hpR_.setHighPass(sr_, 38.13547087602444f, 0.5003270f);
        blockFrames_ = static_cast<int>(std::round(sr_ * 0.400));  // 400 ms
        reset();
    }

    void reset() {
        preL_.reset(); preR_.reset();
        hpL_.reset(); hpR_.reset();
        blockSumSq_ = 0.0;
        blockFramesRun_ = 0;
        blockLufs_.clear();
        blockMS_.clear();
        truePeakLin_ = 0.0f;
    }

    /** Feed stereo PCM. Mono (r == l) or duplicate is fine. */
    inline void pushSample(float l, float r) {
        // True peak (sample peak here; oversampled TP later)
        const float al = std::fabs(l);
        const float ar = std::fabs(r);
        if (al > truePeakLin_) truePeakLin_ = al;
        if (ar > truePeakLin_) truePeakLin_ = ar;

        // K-weight
        const float kl = hpL_.process(preL_.process(l, 0), 0);
        const float kr = hpR_.process(preR_.process(r, 0), 0);

        // Mean square (channels weight = 1.0 for L/R)
        blockSumSq_ += static_cast<double>(kl) * kl + static_cast<double>(kr) * kr;
        blockFramesRun_ += 1;

        if (blockFramesRun_ >= blockFrames_) {
            const double ms = blockSumSq_ / static_cast<double>(blockFrames_);
            blockMS_.push_back(ms);
            // LUFS per block:  -0.691 + 10*log10( ms )
            const float lufs = (ms > 1e-12)
                ? static_cast<float>(-0.691 + 10.0 * std::log10(ms))
                : -100.0f;
            blockLufs_.push_back(lufs);
            blockSumSq_ = 0.0;
            blockFramesRun_ = 0;
        }
    }

    /** Finalize measurement; call after last sample. */
    Result finalize() {
        Result r;
        r.truePeakDb = (truePeakLin_ > 1e-9f)
            ? 20.0f * std::log10(truePeakLin_)
            : -120.0f;

        if (blockMS_.empty()) { r.ok = false; return r; }

        // Absolute gate
        const double ABS_GATE_LUFS = -70.0;
        std::vector<double> keptMS;
        std::vector<float> keptLufs;
        for (size_t i = 0; i < blockMS_.size(); ++i) {
            if (blockLufs_[i] >= ABS_GATE_LUFS) {
                keptMS.push_back(blockMS_[i]);
                keptLufs.push_back(blockLufs_[i]);
            }
        }
        if (keptMS.empty()) {
            r.blocksMeasured = static_cast<int>(blockMS_.size());
            r.blocksKept = 0;
            r.integratedLufs = -70.0f;
            r.ok = false;
            return r;
        }

        // Ungated mean (of kept blocks)
        double sumMS = 0.0;
        for (double v : keptMS) sumMS += v;
        const double ungatedMS = sumMS / static_cast<double>(keptMS.size());
        const float ungatedLufs = static_cast<float>(-0.691 + 10.0 * std::log10(ungatedMS + 1e-12));

        // Relative gate: -10 LU below ungated
        const float REL_GATE_LUFS = ungatedLufs - 10.0f;
        std::vector<double> finalMS;
        for (size_t i = 0; i < keptMS.size(); ++i) {
            if (keptLufs[i] >= REL_GATE_LUFS) {
                finalMS.push_back(keptMS[i]);
            }
        }

        if (finalMS.empty()) {
            r.integratedLufs = ungatedLufs;
            r.ok = true;
        } else {
            double s = 0.0;
            for (double v : finalMS) s += v;
            const double mean = s / static_cast<double>(finalMS.size());
            r.integratedLufs = static_cast<float>(-0.691 + 10.0 * std::log10(mean + 1e-12));
            r.ok = true;
        }

        // Approx LRA: difference between 10th and 95th percentile of kept LUFS
        std::vector<float> sorted = keptLufs;
        std::sort(sorted.begin(), sorted.end());
        if (sorted.size() >= 4) {
            const size_t lo = sorted.size() / 10;
            const size_t hi = (sorted.size() * 95) / 100;
            r.loudnessRange = sorted[hi] - sorted[lo];
        }

        r.blocksMeasured = static_cast<int>(blockMS_.size());
        r.blocksKept = static_cast<int>(keptMS.size());
        return r;
    }

    /** One-shot analysis of an interleaved float buffer. */
    static Result analyze(const float* interleaved, int64_t totalFrames,
                          int channels, float sampleRate) {
        Loudness lm;
        lm.setSampleRate(sampleRate);
        for (int64_t i = 0; i < totalFrames; ++i) {
            const float* f = interleaved + i * channels;
            const float l = f[0];
            const float r = (channels >= 2) ? f[1] : l;
            lm.pushSample(l, r);
        }
        return lm.finalize();
    }

private:
    float sr_ = 48000.0f;
    int blockFrames_ = 19200;    // 400 ms @ 48k

    skb::dsp::Biquad preL_, preR_;
    skb::dsp::Biquad hpL_, hpR_;

    double blockSumSq_ = 0.0;
    int blockFramesRun_ = 0;
    std::vector<double> blockMS_;
    std::vector<float> blockLufs_;
    float truePeakLin_ = 0.0f;
};

} // namespace skb::analyzer

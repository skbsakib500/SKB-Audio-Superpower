#pragma once
#include <cmath>
#include "../dsp/Biquad.h"

namespace skb::spatial {

/**
 * Bauer-style crossfeed: mixes a delayed, low-pass-filtered version of the
 * opposite channel into each ear. Reduces the "in-head" localization that
 * plain headphone stereo produces, at the cost of a slightly narrower image.
 *
 * Algorithm (Bauer "bs2b" simplified):
 *   cross_left  = LP(delay(R))  * amount
 *   cross_right = LP(delay(L))  * amount
 *   L' = L + cross_left  - direct(R) * amount * 0.5
 *   R' = R + cross_right - direct(L) * amount * 0.5
 */
class Crossfeed {
public:
    void setSampleRate(float sr) {
        sr_ = sr;
        maxDelay_ = static_cast<int>(sr_ * 0.0003f);  // 300 µs
        if (maxDelay_ < 1) maxDelay_ = 1;
        ringL_.assign(maxDelay_, 0.0f);
        ringR_.assign(maxDelay_, 0.0f);
        idx_ = 0;
        // 700 Hz Linkwitz-ish low-pass for the crossover path
        lpL_.setLowPass(sr_, 700.0f, 0.5f);
        lpR_.setLowPass(sr_, 700.0f, 0.5f);
    }

    void setEnabled(bool e) { enabled_ = e; if (!e) reset(); }
    void setAmount(float a) { amount_ = a < 0.0f ? 0.0f : (a > 1.0f ? 1.0f : a); }

    void reset() {
        std::fill(ringL_.begin(), ringL_.end(), 0.0f);
        std::fill(ringR_.begin(), ringR_.end(), 0.0f);
        idx_ = 0;
        lpL_.reset(); lpR_.reset();
    }

    inline void process(float& l, float& r) {
        if (!enabled_ || amount_ <= 0.0f) return;

        // Store current samples
        ringL_[idx_] = l;
        ringR_[idx_] = r;

        // Read delayed samples
        const int read = (idx_ + 1) % maxDelay_;
        const float dL = ringL_[read];
        const float dR = ringR_[read];

        // Low-pass the opposite channel's delayed signal
        const float cfL = lpL_.process(dR, 0);
        const float cfR = lpR_.process(dL, 0);

        // Blend
        const float a = amount_;
        l = (1.0f - a) * l + a * cfL;
        r = (1.0f - a) * r + a * cfR;

        idx_ = (idx_ + 1) % maxDelay_;
    }

private:
    float sr_ = 48000.0f;
    bool enabled_ = false;
    float amount_ = 0.5f;
    int maxDelay_ = 14;
    int idx_ = 0;
    std::vector<float> ringL_, ringR_;
    skb::dsp::Biquad lpL_, lpR_;
};

} // namespace skb::spatial

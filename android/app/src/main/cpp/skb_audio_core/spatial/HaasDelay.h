#pragma once
#include <algorithm>
#include <vector>

namespace skb::spatial {

/**
 * Haas / precedence-effect widener:
 * delays one channel by 5-35 ms and mixes back at lower level. Perceptually,
 * the listener hears a wider, "outside-the-head" image without obvious echo,
 * as long as delay stays below ~35 ms and mix stays subtle.
 */
class HaasDelay {
public:
    void setSampleRate(float sr) {
        sr_ = sr;
        const int maxSamples = static_cast<int>(sr_ * 0.05f);   // 50 ms max
        ring_.assign(maxSamples, 0.0f);
        idx_ = 0;
        setDelayMs(15.0f);
    }

    void setEnabled(bool e) { enabled_ = e; if (!e) reset(); }
    void setDelayMs(float ms) {
        const float clamped = ms < 1.0f ? 1.0f : (ms > 40.0f ? 40.0f : ms);
        delaySamples_ = static_cast<int>(sr_ * clamped * 0.001f);
        if (delaySamples_ >= static_cast<int>(ring_.size()))
            delaySamples_ = static_cast<int>(ring_.size()) - 1;
    }
    void setMix(float m) { mix_ = m < 0.0f ? 0.0f : (m > 1.0f ? 1.0f : m); }
    void setSide(int s) { side_ = (s >= 0) ? 1 : -1; }

    void reset() {
        std::fill(ring_.begin(), ring_.end(), 0.0f);
        idx_ = 0;
    }

    inline void process(float& l, float& r) {
        if (!enabled_ || ring_.empty() || delaySamples_ <= 0) return;

        ring_[idx_] = (side_ > 0) ? r : l;

        const int read = (idx_ - delaySamples_ + static_cast<int>(ring_.size()))
                         % static_cast<int>(ring_.size());
        const float delayed = ring_[read];

        if (side_ > 0) r = (1.0f - mix_) * r + mix_ * delayed;
        else           l = (1.0f - mix_) * l + mix_ * delayed;

        idx_ = (idx_ + 1) % static_cast<int>(ring_.size());
    }

private:
    float sr_ = 48000.0f;
    bool enabled_ = false;
    int side_ = 1;
    int delaySamples_ = 720;
    float mix_ = 0.35f;
    int idx_ = 0;
    std::vector<float> ring_;
};

} // namespace skb::spatial

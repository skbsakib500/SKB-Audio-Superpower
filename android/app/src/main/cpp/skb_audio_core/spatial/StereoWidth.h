#pragma once

namespace skb::spatial {

/**
 * Mid/Side stereo width control.
 *   width = 0.0  -> mono
 *   width = 1.0  -> original stereo
 *   width = 2.0  -> very wide (side boosted)
 */
class StereoWidth {
public:
    void setEnabled(bool e) { enabled_ = e; }
    void setWidth(float w) { width_ = w < 0.0f ? 0.0f : (w > 3.0f ? 3.0f : w); }

    inline void process(float& l, float& r) {
        if (!enabled_ || width_ == 1.0f) return;
        const float m = 0.5f * (l + r);
        const float s = 0.5f * (l - r);
        const float ws = s * width_;
        l = m + ws;
        r = m - ws;
    }

private:
    bool enabled_ = false;
    float width_ = 1.0f;
};

} // namespace skb::spatial

#pragma once
#include <cmath>
#include <vector>
#include "../dsp/Biquad.h"

namespace skb::spatial {

/**
 * Simplified vertical-height HRTF emulation.
 *
 * Human perception of "sound from above" comes from two cues:
 *   1. Pinna notch — a narrow spectral dip around 6–10 kHz that deepens
 *      as elevation increases (the pinna acts as a direction-dependent comb).
 *   2. Spectral tilt — elevated sources have subtly less energy above 8 kHz
 *      and slightly more around 4–6 kHz (angle-dependent).
 *
 * We approximate this with:
 *   - A high-shelf cut (elevation-dependent)           → "above" feel
 *   - A peaking boost @ 4.5 kHz (moderate elevation)   → "presence" cue
 *   - A peaking notch @ 8 kHz (elevation-dependent)    → pinna comb
 *
 * This is NOT a full HRTF convolution, but produces a genuine perceptual
 * elevation shift on stereo headphones without needing a dataset.
 */
class HrtfHeight {
public:
    void setSampleRate(float sr) {
        sr_ = sr;
        update(0.0f);
    }

    void setEnabled(bool e) { enabled_ = e; if (!e) reset(); }

    /** elevation in [0, 1]: 0 = neutral, 1 = maximum "above" */
    void setElevation(float e) {
        elevation_ = e < 0.0f ? 0.0f : (e > 1.0f ? 1.0f : e);
        update(elevation_);
    }

    void reset() {
        notchL_.reset(); notchR_.reset();
        presenceL_.reset(); presenceR_.reset();
        shelfL_.reset(); shelfR_.reset();
    }

    inline void process(float& l, float& r) {
        if (!enabled_ || elevation_ <= 0.0f) return;

        // Presence cue (4.5 kHz)
        float l1 = presenceL_.process(l, 0);
        float r1 = presenceR_.process(r, 0);
        // Pinna notch (8 kHz)
        l1 = notchL_.process(l1, 0);
        r1 = notchR_.process(r1, 0);
        // High-shelf cut (beyond ~9 kHz)
        l = shelfL_.process(l1, 0);
        r = shelfR_.process(r1, 0);
    }

private:
    void update(float e) {
        const float presenceGain =  e * 2.5f;              // 0..2.5 dB
        const float notchGain    = -e * 5.0f;              // 0..-5 dB
        const float shelfGain    = -e * 4.0f;              // 0..-4 dB

        presenceL_.setPeaking(sr_, 4500.0f, 1.2f, presenceGain);
        presenceR_.setPeaking(sr_, 4500.0f, 1.2f, presenceGain);
        notchL_.setPeaking(sr_, 8000.0f, 3.0f, notchGain);
        notchR_.setPeaking(sr_, 8000.0f, 3.0f, notchGain);
        shelfL_.setHighShelf(sr_, 9000.0f, 0.7f, shelfGain);
        shelfR_.setHighShelf(sr_, 9000.0f, 0.7f, shelfGain);
    }

    float sr_ = 48000.0f;
    bool enabled_ = false;
    float elevation_ = 0.0f;

    skb::dsp::Biquad notchL_, notchR_;
    skb::dsp::Biquad presenceL_, presenceR_;
    skb::dsp::Biquad shelfL_, shelfR_;
};

} // namespace skb::spatial

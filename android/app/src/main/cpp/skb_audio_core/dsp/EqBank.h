#pragma once
#include "Biquad.h"

namespace skb::dsp {

/**
 * 5 parametric bands (fixed centres) + low-shelf bass + high-shelf treble.
 * All coefficients updated lazily: dirty flag → recompute on next process().
 */
class EqBank {
public:
    static constexpr int BANDS = 5;
    // Frequencies chosen for typical music spectrum
    static constexpr float BAND_FREQ[BANDS] = { 60.0f, 240.0f, 1000.0f, 3500.0f, 10000.0f };
    static constexpr float BAND_Q     = 0.9f;

    void setSampleRate(float sr) { sr_ = sr; markDirty(); }

    void setEnabled(bool e) { enabled_ = e; }

    void setPreampDb(float db) { preampDb_ = db; preampLin_ = std::pow(10.0f, db / 20.0f); }

    void setBandGain(int idx, float gainDb) {
        if (idx < 0 || idx >= BANDS) return;
        bandGain_[idx] = gainDb;
        bandDirty_[idx] = true;
    }

    void setBassDb(float db)   { bassDb_ = db;   bassDirty_ = true; }
    void setTrebleDb(float db) { trebleDb_ = db; trebleDirty_ = true; }

    void reset() {
        for (int i = 0; i < BANDS; ++i) bands_[i].reset();
        bass_.reset(); treble_.reset();
    }

    inline float process(float x, int ch) {
        if (!enabled_) return x;

        // Lazy coefficient recompute (once per sample-block boundary, cheap)
        if (anyDirty_) flushDirty();

        float y = x * preampLin_;
        for (int i = 0; i < BANDS; ++i) y = bands_[i].process(y, ch);
        y = bass_.process(y, ch);
        y = treble_.process(y, ch);
        return y;
    }

private:
    void markDirty() {
        for (int i = 0; i < BANDS; ++i) bandDirty_[i] = true;
        bassDirty_ = true; trebleDirty_ = true;
    }

    void flushDirty() {
        for (int i = 0; i < BANDS; ++i) {
            if (bandDirty_[i]) {
                bands_[i].setPeaking(sr_, BAND_FREQ[i], BAND_Q, bandGain_[i]);
                bandDirty_[i] = false;
            }
        }
        if (bassDirty_)   { bass_.setLowShelf(sr_, 100.0f, 0.7f, bassDb_);      bassDirty_ = false; }
        if (trebleDirty_) { treble_.setHighShelf(sr_, 8000.0f, 0.7f, trebleDb_); trebleDirty_ = false; }
        anyDirty_ = false;
    }

    float sr_ = 48000.0f;
    bool enabled_ = false;

    float preampDb_ = 0.0f;
    float preampLin_ = 1.0f;

    Biquad bands_[BANDS];
    float bandGain_[BANDS] = {0, 0, 0, 0, 0};
    bool  bandDirty_[BANDS] = {true, true, true, true, true};

    Biquad bass_, treble_;
    float bassDb_ = 0.0f;
    float trebleDb_ = 0.0f;
    bool bassDirty_ = true, trebleDirty_ = true;
    bool anyDirty_ = true;
};

} // namespace skb::dsp

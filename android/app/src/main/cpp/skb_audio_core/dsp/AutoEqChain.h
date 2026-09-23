#pragma once
#include <array>
#include <cstdint>
#include <cmath>
#include "Biquad.h"

namespace skb::dsp {

/**
 * AutoEQ correction cascade: up to 10 peaking/shelf filters per channel,
 * driven by a named headphone profile (frequency response correction).
 *
 * Profiles match the AutoEQ project schema:
 *   preamp_db + list of { type, freq, Q, gain_db }
 * where type ∈ {PK, LSH, HSH}
 */
class AutoEqChain {
public:
    static constexpr int MAX_FILTERS = 10;

    void setSampleRate(float sr) {
        sr_ = sr;
        rebuild();
    }

    void setEnabled(bool e) {
        enabled_ = e;
        if (!e) reset();
    }

    void clear() {
        filterCount_ = 0;
        preampDb_ = 0.0f;
        preampLin_ = 1.0f;
        rebuild();
    }

    /** Add a filter. Returns false if chain is full. */
    bool addFilter(int type, float freq, float q, float gainDb) {
        if (filterCount_ >= MAX_FILTERS) return false;
        auto& f = filters_[filterCount_];
        f.type = static_cast<Biquad::Type>(type);
        f.freq = freq;
        f.q = q;
        f.gainDb = gainDb;
        ++filterCount_;
        rebuild();
        return true;
    }

    void setPreampDb(float db) {
        preampDb_ = db;
        preampLin_ = std::pow(10.0f, db / 20.0f);
    }

    void reset() {
        for (int i = 0; i < MAX_FILTERS; ++i) {
            chainL_[i].reset();
            chainR_[i].reset();
        }
    }

    inline void process(float& l, float& r) {
        if (!enabled_ || filterCount_ == 0) return;
        float lv = l * preampLin_;
        float rv = r * preampLin_;
        for (int i = 0; i < filterCount_; ++i) {
            lv = chainL_[i].process(lv, 0);
            rv = chainR_[i].process(rv, 0);
        }
        l = lv;
        r = rv;
    }

private:
    struct Spec {
        Biquad::Type type = Biquad::Type::PK;
        float freq = 1000.0f;
        float q = 1.0f;
        float gainDb = 0.0f;
    };

    void rebuild() {
        for (int i = 0; i < filterCount_; ++i) {
            const auto& s = filters_[i];
            const float gain = s.gainDb;
            switch (s.type) {
                case Biquad::Type::PK:
                    chainL_[i].setPeaking(sr_, s.freq, s.q, gain);
                    chainR_[i].setPeaking(sr_, s.freq, s.q, gain);
                    break;
                case Biquad::Type::LSH:
                    chainL_[i].setLowShelf(sr_, s.freq, 0.7f, gain);
                    chainR_[i].setLowShelf(sr_, s.freq, 0.7f, gain);
                    break;
                case Biquad::Type::HSH:
                    chainL_[i].setHighShelf(sr_, s.freq, 0.7f, gain);
                    chainR_[i].setHighShelf(sr_, s.freq, 0.7f, gain);
                    break;
                default: break;
            }
        }
    }

    float sr_ = 48000.0f;
    bool enabled_ = false;
    int filterCount_ = 0;
    float preampDb_ = 0.0f;
    float preampLin_ = 1.0f;

    Spec filters_[MAX_FILTERS];
    Biquad chainL_[MAX_FILTERS];
    Biquad chainR_[MAX_FILTERS];
};

} // namespace skb::dsp

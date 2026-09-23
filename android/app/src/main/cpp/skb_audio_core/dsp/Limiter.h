#pragma once
#include <cmath>

namespace skb::dsp {

class Limiter {
public:
    void setSampleRate(float sr) {
        sr_ = sr;
        attackCoef_  = std::exp(-1.0f / (0.001f * sr_));
        releaseCoef_ = std::exp(-1.0f / (0.050f * sr_));
    }
    void setEnabled(bool e) { enabled_ = e; reset(); }
    void setCeilingDb(float db) {
        ceilingDb_ = db;
        ceilingLin_ = std::pow(10.0f, db / 20.0f);
    }
    void reset() { env_ = 1.0f; }
    inline float process(float x) {
        if (!enabled_) return x;
        const float ax = std::fabs(x);
        float target = 1.0f;
        if (ax > ceilingLin_) target = ceilingLin_ / (ax + 1e-9f);
        const float coef = (target < env_) ? attackCoef_ : releaseCoef_;
        env_ = coef * env_ + (1.0f - coef) * target;
        float y = x * env_;
        if (y >  ceilingLin_) y =  ceilingLin_;
        if (y < -ceilingLin_) y = -ceilingLin_;
        return y;
    }
private:
    float sr_ = 48000.0f;
    bool enabled_ = false;
    float ceilingDb_ = -0.3f;
    float ceilingLin_ = 0.966f;
    float env_ = 1.0f;
    float attackCoef_  = 0.95f;
    float releaseCoef_ = 0.999f;
};

} // namespace skb::dsp

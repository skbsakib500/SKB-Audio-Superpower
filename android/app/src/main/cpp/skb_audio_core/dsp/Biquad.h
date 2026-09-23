#pragma once
#include <cmath>

namespace skb::dsp {

/**
 * Direct-Form II transposed biquad (Robert Bristow-Johnson's cookbook).
 * Coefficients updated via setPeaking/setLowShelf/setHighShelf/setLowPass/setHighPass.
 *
 * Realtime-safe: only scalar math in process(); coefficient update is cheap
 * enough to run on the audio thread when a user drags a slider.
 */
class Biquad {
public:
    enum class Type { PK, LSH, HSH, LP, HP };

    Biquad() = default;

    void reset() {
        z1_[0] = z2_[0] = 0.0f;
        z1_[1] = z2_[1] = 0.0f;
    }

    void setPeaking(float sampleRate, float f0, float Q, float gainDb) {
        const float A = std::pow(10.0f, gainDb / 40.0f);
        const float w0 = 2.0f * float(M_PI) * f0 / sampleRate;
        const float cw = std::cos(w0);
        const float sw = std::sin(w0);
        const float alpha = sw / (2.0f * Q);

        const float b0 = 1.0f + alpha * A;
        const float b1 = -2.0f * cw;
        const float b2 = 1.0f - alpha * A;
        const float a0 = 1.0f + alpha / A;
        const float a1 = -2.0f * cw;
        const float a2 = 1.0f - alpha / A;
        setNorm(b0, b1, b2, a0, a1, a2);
    }

    void setLowShelf(float sampleRate, float f0, float S, float gainDb) {
        const float A = std::pow(10.0f, gainDb / 40.0f);
        const float w0 = 2.0f * float(M_PI) * f0 / sampleRate;
        const float cw = std::cos(w0);
        const float sw = std::sin(w0);
        const float alpha = sw / 2.0f * std::sqrt((A + 1.0f / A) * (1.0f / S - 1.0f) + 2.0f);
        const float sqA = 2.0f * std::sqrt(A) * alpha;

        const float b0 =        A * ((A + 1.0f) - (A - 1.0f) * cw + sqA);
        const float b1 =  2.0f * A * ((A - 1.0f) - (A + 1.0f) * cw);
        const float b2 =        A * ((A + 1.0f) - (A - 1.0f) * cw - sqA);
        const float a0 =             (A + 1.0f) + (A - 1.0f) * cw + sqA;
        const float a1 = -2.0f *    ((A - 1.0f) + (A + 1.0f) * cw);
        const float a2 =             (A + 1.0f) + (A - 1.0f) * cw - sqA;
        setNorm(b0, b1, b2, a0, a1, a2);
    }

    void setHighShelf(float sampleRate, float f0, float S, float gainDb) {
        const float A = std::pow(10.0f, gainDb / 40.0f);
        const float w0 = 2.0f * float(M_PI) * f0 / sampleRate;
        const float cw = std::cos(w0);
        const float sw = std::sin(w0);
        const float alpha = sw / 2.0f * std::sqrt((A + 1.0f / A) * (1.0f / S - 1.0f) + 2.0f);
        const float sqA = 2.0f * std::sqrt(A) * alpha;

        const float b0 =        A * ((A + 1.0f) + (A - 1.0f) * cw + sqA);
        const float b1 = -2.0f * A * ((A - 1.0f) + (A + 1.0f) * cw);
        const float b2 =        A * ((A + 1.0f) + (A - 1.0f) * cw - sqA);
        const float a0 =             (A + 1.0f) - (A - 1.0f) * cw + sqA;
        const float a1 =  2.0f *    ((A - 1.0f) - (A + 1.0f) * cw);
        const float a2 =             (A + 1.0f) - (A - 1.0f) * cw - sqA;
        setNorm(b0, b1, b2, a0, a1, a2);
    }

    void setLowPass(float sampleRate, float f0, float Q) {
        const float w0 = 2.0f * float(M_PI) * f0 / sampleRate;
        const float cw = std::cos(w0);
        const float sw = std::sin(w0);
        const float alpha = sw / (2.0f * Q);
        const float b0 = (1.0f - cw) * 0.5f;
        const float b1 =  1.0f - cw;
        const float b2 = (1.0f - cw) * 0.5f;
        const float a0 =  1.0f + alpha;
        const float a1 = -2.0f * cw;
        const float a2 =  1.0f - alpha;
        setNorm(b0, b1, b2, a0, a1, a2);
    }

    void setHighPass(float sampleRate, float f0, float Q) {
        const float w0 = 2.0f * float(M_PI) * f0 / sampleRate;
        const float cw = std::cos(w0);
        const float sw = std::sin(w0);
        const float alpha = sw / (2.0f * Q);
        const float b0 =  (1.0f + cw) * 0.5f;
        const float b1 = -(1.0f + cw);
        const float b2 =  (1.0f + cw) * 0.5f;
        const float a0 =   1.0f + alpha;
        const float a1 =  -2.0f * cw;
        const float a2 =   1.0f - alpha;
        setNorm(b0, b1, b2, a0, a1, a2);
    }

    inline float process(float x, int ch) {
        const float y = b0_ * x + z1_[ch];
        z1_[ch] = b1_ * x - a1_ * y + z2_[ch];
        z2_[ch] = b2_ * x - a2_ * y;
        return y;
    }

private:
    void setNorm(float b0, float b1, float b2, float a0, float a1, float a2) {
        const float inv = 1.0f / a0;
        b0_ = b0 * inv;
        b1_ = b1 * inv;
        b2_ = b2 * inv;
        a1_ = a1 * inv;
        a2_ = a2 * inv;
    }

    float b0_ = 1.0f, b1_ = 0.0f, b2_ = 0.0f;
    float a1_ = 0.0f, a2_ = 0.0f;
    float z1_[2] = {0, 0}, z2_[2] = {0, 0};
};

} // namespace skb::dsp

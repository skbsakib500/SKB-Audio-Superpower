#pragma once
#include <algorithm>
#include <cmath>
#include <vector>
#include <array>

namespace skb::spatial {

/**
 * Schroeder-style reverb: 4 parallel comb filters -> 2 series all-pass stages.
 * Stereo, realtime-safe. Audible "hall / stage / studio / cathedral" feel.
 *
 * This is a characterful approximation, not a studio convolution reverb.
 */
class RoomReverb {
public:
    enum class Room { STUDIO, HALL, STAGE, CATHEDRAL };

    void setSampleRate(float sr) {
        sr_ = sr;
        allocateBuffers();
        reset();
        applyRoom(room_);
    }

    void setEnabled(bool e) { enabled_ = e; if (!e) reset(); }

    void setMix(float m) { mix_ = m < 0.0f ? 0.0f : (m > 0.9f ? 0.9f : m); }

    void setRoom(Room r) { room_ = r; applyRoom(r); }

    void reset() {
        std::fill(combL_.begin(), combL_.end(), 0.0f);
        std::fill(combR_.begin(), combR_.end(), 0.0f);
        std::fill(apL_.begin(), apL_.end(), 0.0f);
        std::fill(apR_.begin(), apR_.end(), 0.0f);
        for (int i = 0; i < NUM_COMBS; ++i) {
            combIdxL_[i] = 0; combIdxR_[i] = 0;
            combLPL_[i] = 0.0f; combLPR_[i] = 0.0f;
        }
        for (int i = 0; i < NUM_APS; ++i) {
            apIdxL_[i] = 0; apIdxR_[i] = 0;
        }
    }

    inline void process(float& l, float& r) {
        if (!enabled_ || mix_ <= 0.0f) return;

        const float inL = l;
        const float inR = r;

        float accL = 0.0f, accR = 0.0f;
        for (int i = 0; i < NUM_COMBS; ++i) {
            accL += combFilterL(i, inL);
            accR += combFilterR(i, inR);
        }
        accL *= INV_COMBS;
        accR *= INV_COMBS;

        accL = allpassL(0, accL);
        accR = allpassR(0, accR);
        accL = allpassL(1, accL);
        accR = allpassR(1, accR);

        l = (1.0f - mix_) * inL + mix_ * accL;
        r = (1.0f - mix_) * inR + mix_ * accR;
    }

private:
    static constexpr int NUM_COMBS = 4;
    static constexpr int NUM_APS   = 2;
    static constexpr float INV_COMBS = 0.25f;

    void allocateBuffers() {
        static const float combMs[NUM_COMBS] = { 29.7f, 37.1f, 41.1f, 43.7f };
        static const float apMs[NUM_APS]     = {  5.0f,  1.7f };

        int maxComb = 0, maxAp = 0;
        for (int i = 0; i < NUM_COMBS; ++i) {
            int s = static_cast<int>(sr_ * combMs[i] * 0.001f);
            if (s > maxComb) maxComb = s;
        }
        for (int i = 0; i < NUM_APS; ++i) {
            int s = static_cast<int>(sr_ * apMs[i] * 0.001f);
            if (s > maxAp) maxAp = s;
        }

        combL_.assign(maxComb, 0.0f);
        combR_.assign(maxComb, 0.0f);
        apL_.assign(maxAp, 0.0f);
        apR_.assign(maxAp, 0.0f);

        for (int i = 0; i < NUM_COMBS; ++i) {
            combSizeL_[i] = static_cast<int>(sr_ * combMs[i] * 0.001f);
            int rs = combSizeL_[i] + 23;
            combSizeR_[i] = (rs > maxComb) ? maxComb : rs;
        }
        for (int i = 0; i < NUM_APS; ++i) {
            apSizeL_[i] = static_cast<int>(sr_ * apMs[i] * 0.001f);
            int rs = apSizeL_[i] + 11;
            apSizeR_[i] = (rs > maxAp) ? maxAp : rs;
        }
    }

    void applyRoom(Room r) {
        switch (r) {
            case Room::STUDIO:    decayScale_ = 0.72f; dampScale_ = 0.65f; break;
            case Room::HALL:      decayScale_ = 0.85f; dampScale_ = 0.40f; break;
            case Room::STAGE:     decayScale_ = 0.80f; dampScale_ = 0.55f; break;
            case Room::CATHEDRAL: decayScale_ = 0.91f; dampScale_ = 0.25f; break;
        }
    }

    inline float combFilterL(int i, float x) {
        const int size = combSizeL_[i];
        if (size <= 0) return x;
        const int read = combIdxL_[i] % size;
        const float delayed = combL_[read];
        const float dmp = dampScale_;
        combLPL_[i] = delayed * (1.0f - dmp) + combLPL_[i] * dmp;
        const float y = -x + combLPL_[i];
        combL_[read] = x + y * decayScale_;
        combIdxL_[i] = (combIdxL_[i] + 1) % size;
        return y;
    }

    inline float combFilterR(int i, float x) {
        const int size = combSizeR_[i];
        if (size <= 0) return x;
        const int read = combIdxR_[i] % size;
        const float delayed = combR_[read];
        const float dmp = dampScale_;
        combLPR_[i] = delayed * (1.0f - dmp) + combLPR_[i] * dmp;
        const float y = -x + combLPR_[i];
        combR_[read] = x + y * decayScale_;
        combIdxR_[i] = (combIdxR_[i] + 1) % size;
        return y;
    }

    inline float allpassL(int i, float x) {
        const int size = apSizeL_[i];
        if (size <= 0) return x;
        const int read = apIdxL_[i] % size;
        const float delayed = apL_[read];
        const float y = -x + delayed;
        apL_[read] = x + delayed * 0.5f;
        apIdxL_[i] = (apIdxL_[i] + 1) % size;
        return y;
    }

    inline float allpassR(int i, float x) {
        const int size = apSizeR_[i];
        if (size <= 0) return x;
        const int read = apIdxR_[i] % size;
        const float delayed = apR_[read];
        const float y = -x + delayed;
        apR_[read] = x + delayed * 0.5f;
        apIdxR_[i] = (apIdxR_[i] + 1) % size;
        return y;
    }

    float sr_ = 48000.0f;
    bool enabled_ = false;
    float mix_ = 0.20f;

    Room room_ = Room::HALL;
    float decayScale_ = 0.85f;
    float dampScale_  = 0.40f;

    std::vector<float> combL_, combR_;
    std::vector<float> apL_, apR_;
    std::array<int, NUM_COMBS> combIdxL_{}, combIdxR_{};
    std::array<int, NUM_APS>   apIdxL_{},   apIdxR_{};
    std::array<float, NUM_COMBS> combLPL_{}, combLPR_{};

    int combSizeL_[NUM_COMBS] = {0,0,0,0};
    int combSizeR_[NUM_COMBS] = {0,0,0,0};
    int apSizeL_[NUM_APS]     = {0,0};
    int apSizeR_[NUM_APS]     = {0,0};
};

} // namespace skb::spatial

#pragma once
#include <string>

#include "Crossfeed.h"
#include "StereoWidth.h"
#include "HaasDelay.h"
#include "HrtfHeight.h"
#include "RoomReverb.h"

namespace skb::spatial {

/**
 * SKB Spatial Engine — 3D to 10D product presets.
 *
 * "3D / 4D / ... / 10D" are product names, not physical dimensions.
 * Each maps to a concrete chain configuration of real DSP blocks.
 */
class SpatialEngine {
public:
    enum class Mode {
        OFF = 0,
        D3 = 3, D4 = 4, D5 = 5, D6 = 6,
        D7 = 7, D8 = 8, D9 = 9, D10 = 10
    };

    SpatialEngine() = default;

    void setSampleRate(float sr) {
        sr_ = sr;
        crossfeed_.setSampleRate(sr);
        width_.setEnabled(true);
        haas_.setSampleRate(sr);
        hrtf_.setSampleRate(sr);
        reverb_.setSampleRate(sr);
        applyMode(mode_);
    }

    void setEnabled(bool e) {
        enabled_ = e;
        applyMode(e ? mode_ : Mode::OFF);
    }

    void setMode(Mode m) {
        mode_ = m;
        if (enabled_) applyMode(m);
    }

    void setHeight(float h) {
        height_ = clamp01(h);
        if (enabled_) hrtf_.setElevation(height_);
    }

    void setRoom(int idx) {
        roomIdx_ = idx;
        room_ = static_cast<RoomReverb::Room>(
            idx < 0 ? 0 : (idx > 3 ? 3 : idx));
        if (enabled_) reverb_.setRoom(room_);
    }

    void setIntensity(float i) {
        intensity_ = clamp01(i);
        if (enabled_) applyMode(mode_);
    }

    void reset() {
        crossfeed_.reset();
        haas_.reset();
        hrtf_.reset();
        reverb_.reset();
    }

    inline void process(float& l, float& r) {
        if (!enabled_ || mode_ == Mode::OFF) return;
        width_.process(l, r);
        crossfeed_.process(l, r);
        haas_.process(l, r);
        hrtf_.process(l, r);
        reverb_.process(l, r);
    }

private:
    static float clamp01(float v) { return v < 0.0f ? 0.0f : (v > 1.0f ? 1.0f : v); }

    void applyMode(Mode m) {
        const float I = intensity_;

        // Defaults
        width_.setEnabled(false);     width_.setWidth(1.0f);
        crossfeed_.setEnabled(false); crossfeed_.setAmount(0.0f);
        haas_.setEnabled(false);      haas_.setMix(0.0f); haas_.setDelayMs(15.0f);
        hrtf_.setEnabled(false);      hrtf_.setElevation(0.0f);
        reverb_.setEnabled(false);    reverb_.setMix(0.0f);

        switch (m) {
            case Mode::OFF:
                return;

            case Mode::D3:
                width_.setEnabled(true);     width_.setWidth(1.0f + 0.35f * I);
                crossfeed_.setEnabled(true); crossfeed_.setAmount(0.35f * I);
                break;

            case Mode::D4:
                width_.setEnabled(true);     width_.setWidth(1.0f + 0.55f * I);
                crossfeed_.setEnabled(true); crossfeed_.setAmount(0.40f * I);
                haas_.setEnabled(true);      haas_.setMix(0.20f * I); haas_.setDelayMs(12.0f);
                break;

            case Mode::D5:
                width_.setEnabled(true);     width_.setWidth(1.0f + 0.60f * I);
                crossfeed_.setEnabled(true); crossfeed_.setAmount(0.45f * I);
                haas_.setEnabled(true);      haas_.setMix(0.25f * I); haas_.setDelayMs(18.0f);
                reverb_.setEnabled(true);    reverb_.setMix(0.12f * I);
                break;

            case Mode::D6:
                width_.setEnabled(true);     width_.setWidth(1.0f + 0.60f * I);
                crossfeed_.setEnabled(true); crossfeed_.setAmount(0.50f * I);
                haas_.setEnabled(true);      haas_.setMix(0.28f * I); haas_.setDelayMs(20.0f);
                hrtf_.setEnabled(true);      hrtf_.setElevation(0.35f * I);
                reverb_.setEnabled(true);    reverb_.setMix(0.15f * I);
                break;

            case Mode::D7:
                width_.setEnabled(true);     width_.setWidth(1.0f + 0.70f * I);
                crossfeed_.setEnabled(true); crossfeed_.setAmount(0.55f * I);
                haas_.setEnabled(true);      haas_.setMix(0.32f * I); haas_.setDelayMs(22.0f);
                hrtf_.setEnabled(true);      hrtf_.setElevation(0.50f * I);
                reverb_.setEnabled(true);    reverb_.setMix(0.18f * I);
                break;

            case Mode::D8:
                width_.setEnabled(true);     width_.setWidth(1.0f + 0.80f * I);
                crossfeed_.setEnabled(true); crossfeed_.setAmount(0.60f * I);
                haas_.setEnabled(true);      haas_.setMix(0.35f * I); haas_.setDelayMs(25.0f);
                hrtf_.setEnabled(true);      hrtf_.setElevation(0.60f * I);
                reverb_.setEnabled(true);    reverb_.setMix(0.22f * I);
                break;

            case Mode::D9:
                width_.setEnabled(true);     width_.setWidth(1.0f + 0.85f * I);
                crossfeed_.setEnabled(true); crossfeed_.setAmount(0.65f * I);
                haas_.setEnabled(true);      haas_.setMix(0.38f * I); haas_.setDelayMs(28.0f);
                hrtf_.setEnabled(true);      hrtf_.setElevation(0.75f * I);
                reverb_.setEnabled(true);    reverb_.setMix(0.26f * I);
                break;

            case Mode::D10:
                width_.setEnabled(true);     width_.setWidth(1.0f + 0.95f * I);
                crossfeed_.setEnabled(true); crossfeed_.setAmount(0.70f * I);
                haas_.setEnabled(true);      haas_.setMix(0.40f * I); haas_.setDelayMs(32.0f);
                hrtf_.setEnabled(true);      hrtf_.setElevation(0.90f * I);
                reverb_.setEnabled(true);    reverb_.setMix(0.30f * I);
                break;
        }
    }

    float sr_ = 48000.0f;
    bool enabled_ = false;
    Mode mode_ = Mode::OFF;

    float height_ = 0.5f;
    float intensity_ = 0.7f;
    int roomIdx_ = 1;   // default HALL
    RoomReverb::Room room_ = RoomReverb::Room::HALL;

    StereoWidth  width_;
    Crossfeed    crossfeed_;
    HaasDelay    haas_;
    HrtfHeight   hrtf_;
    RoomReverb   reverb_;
};

} // namespace skb::spatial

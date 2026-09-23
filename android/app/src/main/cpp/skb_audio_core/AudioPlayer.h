#pragma once
#include <atomic>
#include <cstdint>
#include <memory>
#include <mutex>
#include <string>
#include <vector>

#include <oboe/Oboe.h>

#include "WavReader.h"
#include "dsp/EqBank.h"
#include "dsp/Limiter.h"
#include "dsp/AutoEqChain.h"
#include "spatial/SpatialEngine.h"

namespace skb {

enum class PlaybackState : int32_t {
    Idle = 0, Playing = 1, Paused = 2, Completed = 3, Error = 4
};

class AudioPlayer : public oboe::AudioStreamDataCallback,
                    public oboe::AudioStreamErrorCallback {
public:
    AudioPlayer();
    ~AudioPlayer() override;

    // ── Loading / transport ──
    bool loadWav(const std::string& path, std::string& error);
    bool play(std::string& error);
    void pause();
    void stop();
    void seekToFraction(double fraction);

    int64_t positionMs() const;
    int64_t durationMs() const;
    PlaybackState state() const { return state_.load(std::memory_order_acquire); }

    int32_t sourceSampleRate() const { return srcSampleRate_; }
    int32_t sourceChannels() const { return srcChannels_; }
    int32_t sourceBitsPerSample() const { return srcBits_; }

    int32_t deviceSampleRate() const { return deviceSampleRate_.load(); }
    int32_t deviceChannels() const { return deviceChannels_.load(); }

    // ── DSP control ──
    void setDspEnabled(bool e);
    void setPreampDb(float db);
    void setEqBand(int idx, float gainDb);
    void setBassDb(float db);
    void setTrebleDb(float db);
    void setLimiterEnabled(bool e);
    void setLimiterCeilingDb(float db);
    void resetDsp();

    // ── Spatial control ──
    void setSpatialEnabled(bool e);
    void setSpatialMode(int mode);       // 0=OFF, 3..10 = D3..D10
    void setSpatialHeight(float h);      // 0..1
    void setSpatialRoom(int room);       // 0=STUDIO 1=HALL 2=STAGE 3=CATHEDRAL
    void setSpatialIntensity(float i);   // 0..1
    void resetSpatial();

    // ── AutoEQ control ──
    void setAutoEqEnabled(bool e);
    void autoEqClear();
    void autoEqSetPreampDb(float db);
    bool autoEqAddFilter(int type, float freq, float q, float gainDb);

    // Oboe callbacks
    oboe::DataCallbackResult onAudioReady(
        oboe::AudioStream* stream, void* audioData, int32_t numFrames) override;
    bool onError(oboe::AudioStream* stream, oboe::Result error) override;

private:
    bool openStream(std::string& error);
    void closeStream();

    std::shared_ptr<oboe::AudioStream> stream_;
    std::mutex streamMutex_;

    std::vector<float> samples_;
    int32_t srcSampleRate_ = 0;
    int32_t srcChannels_ = 0;
    int32_t srcBits_ = 0;
    int64_t totalFrames_ = 0;

    std::atomic<int64_t> positionFrames_{0};
    std::atomic<int32_t> deviceSampleRate_{0};
    std::atomic<int32_t> deviceChannels_{0};
    std::atomic<PlaybackState> state_{PlaybackState::Idle};

    double resamplePhase_ = 0.0;
    std::vector<float> scratch_;

    // ── DSP ──
    dsp::EqBank  eq_;
    dsp::Limiter limiter_;
    std::atomic<bool> dspEnabled_{false};

    // ── Spatial ──
    spatial::SpatialEngine spatial_;
    std::atomic<bool> spatialEnabled_{false};

    // ── AutoEQ ──
    dsp::AutoEqChain autoEq_;
    std::atomic<bool> autoEqEnabled_{false};
};

} // namespace skb

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
#include "dsp/ReplayGain.h"
#include "spatial/SpatialEngine.h"
#include "analyzer/Analyzer.h"
#include "analyzer/Loudness.h"

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

    // ── Analyzer control ──
    void setAnalyzerEnabled(bool e);
    void analyzerTick();                          // called from UI poll; runs FFT if ready
    int  analyzerBins() const;
    void analyzerReadSpectrum(float* out, int count);
    float analyzerTakePeak();
    float analyzerTakeRmsDb();
    bool  analyzerIsClipping() const;

    // ── Crossfade / Gapless ──
    void setCrossfadeMs(int ms);                  // 0 = gapless (no fade), 1000..8000 = crossfade
    int  crossfadeMs() const { return crossfadeMs_.load(); }
    // Load next track so it's ready when current finishes (gapless/crossfade)
    bool loadNext(const std::string& path, std::string& error);
    void clearNext();

    // ── ReplayGain ──
    void  setReplayGainEnabled(bool e);
    void  setReplayGainTargetDb(float db);
    void  setReplayGainMeasuredPeak(float peakLin);   // from pre-scan
    void  setReplayGainMeasuredPeakDb(float peakDb);
    float replayGainCurrentDb() const;
    float replayGainMeasuredPeakDb() const;

    // Compute the peak (linear) of the currently loaded track. Fast because
    // the samples are already in memory. Call after loadWav to auto-apply RG.
    float computeLoadedPeak() const;

    // Full BS.1770-4 integrated loudness measurement of the currently loaded
    // track. Returns LUFS (-70 if silent/empty). Also fills truePeakDb.
    // NOTE: offline/background use; scans the whole PCM buffer.
    float analyzeLoadedLufs(float& truePeakDbOut) const;

    // Directly set the ReplayGain amount in dB (bypasses peak-based
    // computation). Used by the BS.1770 LUFS path where we already know
    // the desired gain.
    void setReplayGainDirectGainDb(float gainDb);

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

    // ── Analyzer ──
    analyzer::Analyzer analyzer_;

    // ── ReplayGain ──
    dsp::ReplayGain replayGain_;
    std::atomic<bool> replayGainEnabled_{false};

    // ── Crossfade / Gapless ──
    // Second buffer holds the "next" track for seamless transition.
    std::vector<float> nextSamples_;
    int32_t nextSrcSampleRate_ = 0;
    int32_t nextSrcChannels_ = 0;
    int64_t nextTotalFrames_ = 0;
    std::atomic<int> crossfadeMs_{0};             // 0 = gapless hard-cut
    std::atomic<bool> transitioning_{false};
    int64_t transitionStartFrame_ = 0;            // source-frame when fade begins
    int64_t transitionLengthFrames_ = 0;          // fade duration in output frames
    int64_t transitionFramesDone_ = 0;
    int64_t nextPositionFrames_ = 0;
};

} // namespace skb

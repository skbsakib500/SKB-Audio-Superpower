#pragma once
#include <atomic>
#include <cstdint>
#include <memory>
#include <mutex>
#include <string>
#include <vector>

#include <oboe/Oboe.h>

#include "WavReader.h"

namespace skb {

enum class PlaybackState : int32_t {
    Idle = 0,
    Playing = 1,
    Paused = 2,
    Completed = 3,
    Error = 4
};

class AudioPlayer : public oboe::AudioStreamDataCallback,
                    public oboe::AudioStreamErrorCallback {
public:
    AudioPlayer();
    ~AudioPlayer() override;

    // Load a WAV file. Replaces any currently loaded track.
    bool loadWav(const std::string& path, std::string& error);

    // Start / resume playback.
    bool play(std::string& error);

    // Pause playback (keeps position).
    void pause();

    // Stop playback and reset position to 0.
    void stop();

    // Seek to fractional position [0.0, 1.0].
    void seekToFraction(double fraction);

    // Current playback position in milliseconds.
    int64_t positionMs() const;

    // Total duration in milliseconds.
    int64_t durationMs() const;

    // Current state.
    PlaybackState state() const { return state_.load(std::memory_order_acquire); }

    // Source info.
    int32_t sourceSampleRate() const { return srcSampleRate_; }
    int32_t sourceChannels() const { return srcChannels_; }
    int32_t sourceBitsPerSample() const { return srcBits_; }

    // Device output info (filled after play()).
    int32_t deviceSampleRate() const { return deviceSampleRate_.load(); }
    int32_t deviceChannels() const { return deviceChannels_.load(); }

    // Oboe callbacks
    oboe::DataCallbackResult onAudioReady(
        oboe::AudioStream* stream,
        void* audioData,
        int32_t numFrames) override;

    bool onError(oboe::AudioStream* stream, oboe::Result error) override;

private:
    bool openStream(std::string& error);
    void closeStream();

    // --- Stream ---
    std::shared_ptr<oboe::AudioStream> stream_;
    std::mutex streamMutex_;

    // --- Source data ---
    std::vector<float> samples_;          // interleaved, source rate
    int32_t srcSampleRate_ = 0;
    int32_t srcChannels_ = 0;
    int32_t srcBits_ = 0;
    int64_t totalFrames_ = 0;

    // --- Playback position (in source frames) ---
    std::atomic<int64_t> positionFrames_{0};

    // --- Device format ---
    std::atomic<int32_t> deviceSampleRate_{0};
    std::atomic<int32_t> deviceChannels_{0};

    // --- State ---
    std::atomic<PlaybackState> state_{PlaybackState::Idle};

    // --- Simple linear resampler state ---
    // If device rate != source rate, we do a lightweight linear interpolation
    // here so we can produce correct-pitch output. Phase 5 will replace this
    // with r8brain (high quality SRC).
    double resamplePhase_ = 0.0;

    // Scratch buffer for channel mapping (avoid alloc in callback)
    std::vector<float> scratch_;
};

} // namespace skb

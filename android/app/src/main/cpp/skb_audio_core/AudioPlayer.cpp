#include "AudioPlayer.h"

#include <android/log.h>
#include <cmath>
#include <cstring>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif
#ifndef M_PI_2
#define M_PI_2 1.57079632679489661923
#endif

#define LOG_TAG "SKB-AudioPlayer"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace skb {

AudioPlayer::AudioPlayer() = default;

AudioPlayer::~AudioPlayer() {
    stop();
    closeStream();
}

bool AudioPlayer::loadWav(const std::string& path, std::string& error) {
    std::lock_guard<std::mutex> lock(streamMutex_);

    if (stream_) {
        stream_->requestStop();
        stream_->close();
        stream_.reset();
    }
    state_.store(PlaybackState::Idle, std::memory_order_release);

    WavData wav;
    if (!WavReader::load(path, wav, error)) {
        LOGE("loadWav failed: %s", error.c_str());
        return false;
    }

    samples_        = std::move(wav.samples);
    srcSampleRate_  = static_cast<int32_t>(wav.sampleRate);
    srcChannels_    = static_cast<int32_t>(wav.channels);
    srcBits_        = static_cast<int32_t>(wav.bitsPerSample);
    totalFrames_    = static_cast<int64_t>(wav.frameCount);
    positionFrames_.store(0, std::memory_order_release);
    resamplePhase_  = 0.0;
    eq_.reset();
    limiter_.reset();
    spatial_.reset();
    autoEq_.reset();
    replayGain_.reset();

    // Clear any pending next-track transition
    transitioning_.store(false, std::memory_order_release);
    transitionFramesDone_ = 0;
    nextSamples_.clear();
    nextTotalFrames_ = 0;
    nextPositionFrames_ = 0;

    LOGI("Loaded: %d Hz, %d ch, %d bits, %lld frames",
         srcSampleRate_, srcChannels_, srcBits_,
         static_cast<long long>(totalFrames_));
    return true;
}

bool AudioPlayer::openStream(std::string& error) {
    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output)
           ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
           ->setSharingMode(oboe::SharingMode::Shared)
           ->setFormat(oboe::AudioFormat::Float)
           ->setChannelCount(oboe::ChannelCount::Stereo)
           ->setDataCallback(this)
           ->setErrorCallback(this);

    oboe::Result result = builder.openStream(stream_);
    if (result != oboe::Result::OK || !stream_) {
        error = "Oboe openStream failed: ";
        error += oboe::convertToText(result);
        LOGE("%s", error.c_str());
        return false;
    }

    deviceSampleRate_.store(stream_->getSampleRate(), std::memory_order_release);
    deviceChannels_.store(stream_->getChannelCount(), std::memory_order_release);

    scratch_.assign(static_cast<size_t>(stream_->getChannelCount()) *
                    static_cast<size_t>(stream_->getFramesPerBurst() * 8), 0.0f);

    const float sr = static_cast<float>(stream_->getSampleRate());
    eq_.setSampleRate(sr);
    limiter_.setSampleRate(sr);
    spatial_.setSampleRate(sr);
    autoEq_.setSampleRate(sr);
    analyzer_.setSampleRate(sr);
    replayGain_.setSampleRate(sr);

    LOGI("Oboe stream opened: device %d Hz, %d ch",
         stream_->getSampleRate(), stream_->getChannelCount());
    return true;
}

void AudioPlayer::closeStream() {
    std::lock_guard<std::mutex> lock(streamMutex_);
    if (stream_) {
        stream_->requestStop();
        stream_->close();
        stream_.reset();
    }
}

bool AudioPlayer::play(std::string& error) {
    if (samples_.empty()) { error = "no track loaded"; return false; }

    std::lock_guard<std::mutex> lock(streamMutex_);

    if (!stream_) { if (!openStream(error)) return false; }

    if (state_.load(std::memory_order_acquire) == PlaybackState::Completed) {
        positionFrames_.store(0, std::memory_order_release);
        resamplePhase_ = 0.0;
    }

    oboe::Result r = stream_->requestStart();
    if (r != oboe::Result::OK) {
        error = "Oboe requestStart failed: ";
        error += oboe::convertToText(r);
        return false;
    }
    state_.store(PlaybackState::Playing, std::memory_order_release);
    return true;
}

void AudioPlayer::pause() {
    std::lock_guard<std::mutex> lock(streamMutex_);
    if (stream_ && state_.load(std::memory_order_acquire) == PlaybackState::Playing) {
        stream_->requestPause();
        state_.store(PlaybackState::Paused, std::memory_order_release);
    }
}

void AudioPlayer::stop() {
    std::lock_guard<std::mutex> lock(streamMutex_);
    if (stream_) stream_->requestStop();
    positionFrames_.store(0, std::memory_order_release);
    resamplePhase_ = 0.0;
    eq_.reset();
    limiter_.reset();
    spatial_.reset();
    autoEq_.reset();
    replayGain_.reset();
    transitioning_.store(false, std::memory_order_release);
    state_.store(PlaybackState::Idle, std::memory_order_release);
}

void AudioPlayer::seekToFraction(double fraction) {
    if (fraction < 0.0) fraction = 0.0;
    if (fraction > 1.0) fraction = 1.0;
    int64_t target = static_cast<int64_t>(fraction * static_cast<double>(totalFrames_));
    if (target < 0) target = 0;
    if (target > totalFrames_) target = totalFrames_;
    positionFrames_.store(target, std::memory_order_release);
    resamplePhase_ = 0.0;
}

int64_t AudioPlayer::positionMs() const {
    if (srcSampleRate_ <= 0) return 0;
    return (positionFrames_.load(std::memory_order_acquire) * 1000) / srcSampleRate_;
}

int64_t AudioPlayer::durationMs() const {
    if (srcSampleRate_ <= 0) return 0;
    return (totalFrames_ * 1000) / srcSampleRate_;
}

// ─────────────────────────────────────────────────────
//  DSP control
// ─────────────────────────────────────────────────────
void AudioPlayer::setDspEnabled(bool e)      { dspEnabled_.store(e, std::memory_order_release); }
void AudioPlayer::setPreampDb(float db)      { eq_.setPreampDb(db); }
void AudioPlayer::setEqBand(int idx, float db){ eq_.setBandGain(idx, db); }
void AudioPlayer::setBassDb(float db)        { eq_.setBassDb(db); }
void AudioPlayer::setTrebleDb(float db)      { eq_.setTrebleDb(db); }
void AudioPlayer::setLimiterEnabled(bool e)  { limiter_.setEnabled(e); }
void AudioPlayer::setLimiterCeilingDb(float db) { limiter_.setCeilingDb(db); }
void AudioPlayer::resetDsp()                 { eq_.reset(); limiter_.reset(); }

// ─────────────────────────────────────────────────────
//  Spatial control
// ─────────────────────────────────────────────────────
void AudioPlayer::setSpatialEnabled(bool e) {
    spatialEnabled_.store(e, std::memory_order_release);
    spatial_.setEnabled(e);
}
void AudioPlayer::setSpatialMode(int mode) {
    spatial::SpatialEngine::Mode m;
    switch (mode) {
        case 3:  m = spatial::SpatialEngine::Mode::D3;  break;
        case 4:  m = spatial::SpatialEngine::Mode::D4;  break;
        case 5:  m = spatial::SpatialEngine::Mode::D5;  break;
        case 6:  m = spatial::SpatialEngine::Mode::D6;  break;
        case 7:  m = spatial::SpatialEngine::Mode::D7;  break;
        case 8:  m = spatial::SpatialEngine::Mode::D8;  break;
        case 9:  m = spatial::SpatialEngine::Mode::D9;  break;
        case 10: m = spatial::SpatialEngine::Mode::D10; break;
        default: m = spatial::SpatialEngine::Mode::OFF; break;
    }
    spatial_.setMode(m);
}
void AudioPlayer::setSpatialHeight(float h)    { spatial_.setHeight(h); }
void AudioPlayer::setSpatialRoom(int room)     { spatial_.setRoom(room); }
void AudioPlayer::setSpatialIntensity(float i) { spatial_.setIntensity(i); }
void AudioPlayer::resetSpatial()               { spatial_.reset(); }

// ─────────────────────────────────────────────────────
//  AutoEQ control
// ─────────────────────────────────────────────────────
void AudioPlayer::setAutoEqEnabled(bool e) {
    autoEqEnabled_.store(e, std::memory_order_release);
    autoEq_.setEnabled(e);
}
void AudioPlayer::autoEqClear()                       { autoEq_.clear(); }
void AudioPlayer::autoEqSetPreampDb(float db)         { autoEq_.setPreampDb(db); }
bool AudioPlayer::autoEqAddFilter(int t, float f, float q, float g) {
    return autoEq_.addFilter(t, f, q, g);
}

// ─────────────────────────────────────────────────────
//  Analyzer
// ─────────────────────────────────────────────────────
void AudioPlayer::setAnalyzerEnabled(bool e) { analyzer_.enable(e); }
void AudioPlayer::analyzerTick()             { analyzer_.maybeRunFft(); }
int  AudioPlayer::analyzerBins() const       { return analyzer::Analyzer::FFT_BINS; }
void AudioPlayer::analyzerReadSpectrum(float* out, int count) {
    analyzer_.readSpectrum(out, count);
}
float AudioPlayer::analyzerTakePeak()        { return analyzer_.takePeak(); }
float AudioPlayer::analyzerTakeRmsDb()       { return analyzer_.takeRmsDb(); }
bool  AudioPlayer::analyzerIsClipping() const{ return analyzer_.isClipping(); }

// ─────────────────────────────────────────────────────
//  ReplayGain accessors
// ─────────────────────────────────────────────────────
void AudioPlayer::setReplayGainEnabled(bool e) {
    replayGainEnabled_.store(e, std::memory_order_release);
    replayGain_.setEnabled(e);
}
void AudioPlayer::setReplayGainTargetDb(float db) {
    replayGain_.setTargetPeakDb(db);
}
void AudioPlayer::setReplayGainMeasuredPeak(float peakLin) {
    replayGain_.setMeasuredPeak(peakLin);
}
void AudioPlayer::setReplayGainMeasuredPeakDb(float peakDb) {
    replayGain_.setMeasuredPeakDb(peakDb);
}
float AudioPlayer::replayGainCurrentDb() const {
    return replayGain_.currentGainDb();
}
float AudioPlayer::replayGainMeasuredPeakDb() const {
    return replayGain_.measuredPeakDb();
}

// Scan the loaded samples for the absolute peak. Realtime-safe (off audio
// thread). Called once per track after decode, before play.
// Smart Queue: BPM detection of the loaded track (offline).
float AudioPlayer::analyzeLoadedBpm(float& confidenceOut) const {
    confidenceOut = 0.0f;
    if (samples_.empty() || srcChannels_ <= 0 || srcSampleRate_ <= 0) return 0.0f;
    const auto r = analyzer::BpmDetector::analyze(
        samples_.data(), totalFrames_, srcChannels_, srcSampleRate_);
    confidenceOut = r.confidence;
    return r.ok ? r.bpm : 0.0f;
}

// Smart Queue: spectral energy distribution of the loaded track (offline).
void AudioPlayer::analyzeLoadedEnergy(float& lowOut, float& midOut, float& highOut,
                                      float& centroidHzOut) const {
    lowOut = midOut = highOut = 0.0f;
    centroidHzOut = 0.0f;
    if (samples_.empty() || srcChannels_ <= 0 || srcSampleRate_ <= 0) return;
    const auto r = analyzer::EnergyDetector::analyze(
        samples_.data(), totalFrames_, srcChannels_, srcSampleRate_);
    if (!r.ok) return;
    lowOut = r.low;
    midOut = r.mid;
    highOut = r.high;
    centroidHzOut = r.centroidHz;
}

// Direct gain (bypasses peak computation). Used by LUFS-based ReplayGain.
void AudioPlayer::setReplayGainDirectGainDb(float gainDb) {
    replayGain_.setDirectGainDb(gainDb);
}

float AudioPlayer::computeLoadedPeak() const {
    float peak = 0.0f;
    for (float s : samples_) {
        const float a = std::fabs(s);
        if (a > peak) peak = a;
    }
    return peak;
}

// BS.1770-4 integrated loudness of the loaded track. Offline/background.
float AudioPlayer::analyzeLoadedLufs(float& truePeakDbOut) const {
    truePeakDbOut = -120.0f;
    if (samples_.empty() || srcChannels_ <= 0 || srcSampleRate_ <= 0) {
        return -70.0f;
    }
    const auto result = analyzer::Loudness::analyze(
        samples_.data(),
        totalFrames_,
        srcChannels_,
        static_cast<float>(srcSampleRate_));
    truePeakDbOut = result.truePeakDb;
    return result.ok ? result.integratedLufs : -70.0f;
}

// ─────────────────────────────────────────────────────
//  Crossfade / Gapless
// ─────────────────────────────────────────────────────
void AudioPlayer::setCrossfadeMs(int ms) {
    if (ms < 0) ms = 0;
    if (ms > 12000) ms = 12000;
    crossfadeMs_.store(ms, std::memory_order_release);
}

bool AudioPlayer::loadNext(const std::string& path, std::string& error) {
    // Load the next track's PCM into a parallel buffer.
    // This is intended to be called from the UI thread while current is playing.
    WavData wav;
    if (!WavReader::load(path, wav, error)) return false;

    // Guard: only stereo-or-mono sources are allowed for simplified crossfade.
    // (multi-channel handled by channel-mapper on output side)
    std::lock_guard<std::mutex> lock(streamMutex_);
    nextSamples_         = std::move(wav.samples);
    nextSrcSampleRate_   = static_cast<int32_t>(wav.sampleRate);
    nextSrcChannels_     = static_cast<int32_t>(wav.channels);
    nextTotalFrames_     = static_cast<int64_t>(wav.frameCount);
    nextPositionFrames_  = 0;
    transitioning_.store(false, std::memory_order_release);
    transitionFramesDone_ = 0;
    LOGI("Next track preloaded: %lld frames @ %d Hz",
         static_cast<long long>(nextTotalFrames_), nextSrcSampleRate_);
    return true;
}

void AudioPlayer::clearNext() {
    std::lock_guard<std::mutex> lock(streamMutex_);
    nextSamples_.clear();
    nextTotalFrames_ = 0;
    nextPositionFrames_ = 0;
    transitioning_.store(false, std::memory_order_release);
}

// ─────────────────────────────────────────────────────
//  Realtime callback with crossfade blending
// ─────────────────────────────────────────────────────
oboe::DataCallbackResult AudioPlayer::onAudioReady(
        oboe::AudioStream* stream, void* audioData, int32_t numFrames) {

    float* out = static_cast<float*>(audioData);
    const int32_t outChannels = stream->getChannelCount();
    const int32_t outRate = stream->getSampleRate();
    const int32_t srcCh = srcChannels_;
    const int32_t srcRate = srcSampleRate_;

    if (state_.load(std::memory_order_acquire) != PlaybackState::Playing
        || samples_.empty() || srcCh <= 0 || srcRate <= 0) {
        std::memset(out, 0, sizeof(float) * static_cast<size_t>(numFrames) * outChannels);
        return oboe::DataCallbackResult::Continue;
    }

    int64_t pos = positionFrames_.load(std::memory_order_relaxed);
    const double rateRatio = static_cast<double>(srcRate) / static_cast<double>(outRate);
    const bool useDsp     = dspEnabled_.load(std::memory_order_relaxed);
    const bool useSpatial = spatialEnabled_.load(std::memory_order_relaxed);
    const bool useAutoEq  = autoEqEnabled_.load(std::memory_order_relaxed);
    const bool useReplayGain = replayGainEnabled_.load(std::memory_order_relaxed);

    // ── Crossfade setup ──
    // Determine if we should start a transition during this block.
    // We begin crossfade `crossfadeFrames` before the current track ends,
    // provided a next track is loaded.
    const int crossfadeMs = crossfadeMs_.load(std::memory_order_relaxed);
    const int64_t crossfadeFramesSrc = static_cast<int64_t>(
        (static_cast<double>(crossfadeMs) * 0.001) * srcRate);
    const bool haveNext = !nextSamples_.empty() && nextTotalFrames_ > 0;
    const bool isTransitioning = transitioning_.load(std::memory_order_relaxed);

    // Start crossfade when current position crosses threshold
    if (!isTransitioning && haveNext && crossfadeMs > 0
        && (totalFrames_ - pos) <= crossfadeFramesSrc) {
        transitioning_.store(true, std::memory_order_release);
        transitionStartFrame_  = pos;
        transitionLengthFrames_ = (totalFrames_ - pos);  // fade over remaining source frames
        transitionFramesDone_  = 0;
    }

    for (int32_t i = 0; i < numFrames; ++i) {
        float l = 0.0f, r = 0.0f;

        if (pos < totalFrames_ && pos * srcCh < static_cast<int64_t>(samples_.size())) {
            const float* frameIn = samples_.data() + pos * srcCh;
            l = frameIn[0];
            r = (srcCh >= 2) ? frameIn[1] : l;
        } else if (!haveNext) {
            // No next track and current ended → silence + mark completed
            out[i * outChannels] = 0.0f;
            if (outChannels > 1) out[i * outChannels + 1] = 0.0f;
            continue;
        }

        // Crossfade mixing: read next track, apply equal-power fade
        if (isTransitioning && haveNext) {
            const int64_t nextPos = nextPositionFrames_;
            if (nextPos < nextTotalFrames_) {
                const int nSrcCh = nextSrcChannels_;
                const float* nFrame = nextSamples_.data() + nextPos * nSrcCh;
                const float nl = nFrame[0];
                const float nr = (nSrcCh >= 2) ? nFrame[1] : nl;

                // equal-power fade curve: t in [0,1]
                double t = 0.0;
                if (transitionLengthFrames_ > 0) {
                    t = static_cast<double>(transitionFramesDone_) /
                        static_cast<double>(transitionLengthFrames_);
                }
                if (t > 1.0) t = 1.0;
                const float fadeIn  = static_cast<float>(std::sin(t * M_PI_2));   // 0 → 1
                const float fadeOut = static_cast<float>(std::cos(t * M_PI_2));   // 1 → 0

                l = l * fadeOut + nl * fadeIn;
                r = r * fadeOut + nr * fadeIn;

                // Advance next source frame using same ratio
                static thread_local double nextResamplePhase = 0.0;
                if (nextSrcSampleRate_ == outRate) {
                    ++nextPositionFrames_;
                } else {
                    nextResamplePhase += static_cast<double>(nextSrcSampleRate_) /
                                         static_cast<double>(outRate);
                    while (nextResamplePhase >= 1.0) {
                        nextResamplePhase -= 1.0;
                        ++nextPositionFrames_;
                    }
                }
                ++transitionFramesDone_;
            }
        }

        // ReplayGain first (headroom-aware normalization)
        if (useReplayGain) { replayGain_.process(l, r); }

        // DSP chain
        if (useDsp)      { l = eq_.process(l, 0); r = eq_.process(r, 1); }
        if (useAutoEq)   { autoEq_.process(l, r); }
        if (useSpatial)  { spatial_.process(l, r); }
        if (useDsp)      { l = limiter_.process(l); r = limiter_.process(r); }

        if (outChannels >= 2) {
            out[i * outChannels + 0] = l;
            out[i * outChannels + 1] = r;
            for (int32_t c = 2; c < outChannels; ++c) out[i * outChannels + c] = 0.0f;
        } else {
            out[i * outChannels] = 0.5f * (l + r);
        }

        analyzer_.pushSample(0.5f * (l + r));

        // Advance current source frame
        if (srcRate == outRate) ++pos;
        else {
            resamplePhase_ += rateRatio;
            while (resamplePhase_ >= 1.0) { resamplePhase_ -= 1.0; ++pos; }
        }

        // Transition complete?
        if (isTransitioning && transitionFramesDone_ >= transitionLengthFrames_) {
            // Promote next → current (swap buffers), reset position
            samples_ = std::move(nextSamples_);
            srcSampleRate_   = nextSrcSampleRate_;
            srcChannels_     = nextSrcChannels_;
            srcBits_         = 16;
            totalFrames_     = nextTotalFrames_;
            positionFrames_.store(0, std::memory_order_release);
            pos = 0;
            resamplePhase_ = 0.0;

            nextSamples_.clear();
            nextTotalFrames_ = 0;
            nextPositionFrames_ = 0;
            transitioning_.store(false, std::memory_order_release);
            transitionFramesDone_ = 0;
            transitionLengthFrames_ = 0;

            // Fill remainder with silence (rare, one block only)
            for (int32_t j = i + 1; j < numFrames; ++j) {
                for (int32_t c = 0; c < outChannels; ++c) {
                    out[j * outChannels + c] = 0.0f;
                }
            }
            positionFrames_.store(pos, std::memory_order_release);
            return oboe::DataCallbackResult::Continue;
        }
    }

    positionFrames_.store(pos, std::memory_order_release);
    return oboe::DataCallbackResult::Continue;
}

bool AudioPlayer::onError(oboe::AudioStream*, oboe::Result error) {
    LOGE("Oboe stream error: %s", oboe::convertToText(error));
    state_.store(PlaybackState::Error, std::memory_order_release);
    return false;
}

} // namespace skb

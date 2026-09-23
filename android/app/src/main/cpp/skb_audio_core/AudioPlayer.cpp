#include "AudioPlayer.h"

#include <android/log.h>
#include <cmath>
#include <cstring>

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

    eq_.setSampleRate(static_cast<float>(stream_->getSampleRate()));
    limiter_.setSampleRate(static_cast<float>(stream_->getSampleRate()));
    spatial_.setSampleRate(static_cast<float>(stream_->getSampleRate()));
    autoEq_.setSampleRate(static_cast<float>(stream_->getSampleRate()));

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
//  Realtime callback — L/R paired for spatial processing
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

    for (int32_t i = 0; i < numFrames; ++i) {
        if (pos >= totalFrames_) {
            for (int32_t j = i; j < numFrames; ++j) {
                for (int32_t c = 0; c < outChannels; ++c) {
                    out[j * outChannels + c] = 0.0f;
                }
            }
            state_.store(PlaybackState::Completed, std::memory_order_release);
            return oboe::DataCallbackResult::Continue;
        }

        const float* frameIn = samples_.data() + pos * srcCh;

        // L / R extraction (mono source -> duplicate to both)
        float l = frameIn[0];
        float r = (srcCh >= 2) ? frameIn[1] : l;

        // 1) EQ (stereo)
        if (useDsp) {
            l = eq_.process(l, 0);
            r = eq_.process(r, 1);
        }

        // 1b) AutoEQ correction (per-headphone)
        if (useAutoEq) {
            autoEq_.process(l, r);
        }

        // 2) Spatial (stereo paired)
        if (useSpatial) {
            spatial_.process(l, r);
        }

        // 3) Limiter (stereo)
        if (useDsp) {
            l = limiter_.process(l);
            r = limiter_.process(r);
        }

        // Write out
        if (outChannels >= 2) {
            out[i * outChannels + 0] = l;
            out[i * outChannels + 1] = r;
            for (int32_t c = 2; c < outChannels; ++c) {
                out[i * outChannels + c] = 0.0f;
            }
        } else {
            out[i * outChannels] = 0.5f * (l + r);
        }

        // Advance source position
        if (srcRate == outRate) {
            ++pos;
        } else {
            resamplePhase_ += rateRatio;
            while (resamplePhase_ >= 1.0) {
                resamplePhase_ -= 1.0;
                ++pos;
            }
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

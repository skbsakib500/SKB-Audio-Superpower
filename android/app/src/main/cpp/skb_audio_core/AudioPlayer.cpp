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

    // Configure DSP for the device sample rate
    eq_.setSampleRate(static_cast<float>(stream_->getSampleRate()));
    limiter_.setSampleRate(static_cast<float>(stream_->getSampleRate()));

    LOGI("Oboe stream opened: device %d Hz, %d ch, bufferFrames=%d",
         stream_->getSampleRate(), stream_->getChannelCount(),
         stream_->getBufferSizeInFrames());
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
void AudioPlayer::setDspEnabled(bool e) {
    dspEnabled_.store(e, std::memory_order_release);
}

void AudioPlayer::setPreampDb(float db)   { eq_.setPreampDb(db); }
void AudioPlayer::setEqBand(int idx, float db) { eq_.setBandGain(idx, db); }
void AudioPlayer::setBassDb(float db)     { eq_.setBassDb(db); }
void AudioPlayer::setTrebleDb(float db)   { eq_.setTrebleDb(db); }

void AudioPlayer::setLimiterEnabled(bool e)      { limiter_.setEnabled(e); }
void AudioPlayer::setLimiterCeilingDb(float db)  { limiter_.setCeilingDb(db); }

void AudioPlayer::resetDsp() {
    eq_.reset();
    limiter_.reset();
}

// ─────────────────────────────────────────────────────
//  Realtime callback
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
    const bool useDsp = dspEnabled_.load(std::memory_order_relaxed);

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

        for (int32_t c = 0; c < outChannels; ++c) {
            int32_t srcIdx = (c < srcCh) ? c : 0;
            float s = frameIn[srcIdx];

            if (useDsp) {
                s = eq_.process(s, c < 2 ? c : 0);
                s = limiter_.process(s);
            }

            out[i * outChannels + c] = s;
        }

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

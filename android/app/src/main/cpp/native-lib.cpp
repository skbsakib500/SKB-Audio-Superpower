#include <jni.h>
#include <memory>
#include <string>
#include <vector>

#include "skb_audio_core/skb_version.h"
#include "skb_audio_core/AudioPlayer.h"

static std::unique_ptr<skb::AudioPlayer> gPlayer;

static skb::AudioPlayer* ensurePlayer() {
    if (!gPlayer) gPlayer = std::make_unique<skb::AudioPlayer>();
    return gPlayer.get();
}

extern "C" {

// ── Version ──
JNIEXPORT jstring JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeVersion(JNIEnv* env, jobject) {
    std::string v = "skb-core v";
    v += SKB_VERSION_STRING;
    v += " [";
    v += SKB_CODENAME;
    v += "]";
    return env->NewStringUTF(v.c_str());
}

// ── Playback ──
JNIEXPORT jboolean JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeLoadWav(
        JNIEnv* env, jobject, jstring jPath) {
    const char* path = env->GetStringUTFChars(jPath, nullptr);
    std::string error;
    bool ok = ensurePlayer()->loadWav(path ? path : "", error);
    if (path) env->ReleaseStringUTFChars(jPath, path);
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativePlay(JNIEnv*, jobject) {
    std::string error;
    return ensurePlayer()->play(error) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativePause(JNIEnv*, jobject) {
    ensurePlayer()->pause();
}

JNIEXPORT void JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeStop(JNIEnv*, jobject) {
    ensurePlayer()->stop();
}

JNIEXPORT void JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeSeek(JNIEnv*, jobject, jdouble fraction) {
    ensurePlayer()->seekToFraction(fraction);
}

JNIEXPORT jlong JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativePositionMs(JNIEnv*, jobject) {
    return static_cast<jlong>(ensurePlayer()->positionMs());
}

JNIEXPORT jlong JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeDurationMs(JNIEnv*, jobject) {
    return static_cast<jlong>(ensurePlayer()->durationMs());
}

JNIEXPORT jint JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeState(JNIEnv*, jobject) {
    return static_cast<jint>(ensurePlayer()->state());
}

JNIEXPORT jstring JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeSourceInfo(JNIEnv* env, jobject) {
    auto* p = ensurePlayer();
    std::string s = std::to_string(p->sourceSampleRate()) + " Hz | " +
                    std::to_string(p->sourceChannels()) + " ch | " +
                    std::to_string(p->sourceBitsPerSample()) + " bit";
    return env->NewStringUTF(s.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeDeviceInfo(JNIEnv* env, jobject) {
    auto* p = ensurePlayer();
    int32_t sr = p->deviceSampleRate();
    if (sr <= 0) return env->NewStringUTF("device: not opened yet");
    std::string s = std::to_string(sr) + " Hz | " +
                    std::to_string(p->deviceChannels()) + " ch";
    return env->NewStringUTF(s.c_str());
}

// ── DSP control ──
JNIEXPORT void JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeSetDspEnabled(JNIEnv*, jobject, jboolean e) {
    ensurePlayer()->setDspEnabled(e == JNI_TRUE);
}

JNIEXPORT void JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeSetPreamp(JNIEnv*, jobject, jfloat db) {
    ensurePlayer()->setPreampDb(static_cast<float>(db));
}

JNIEXPORT void JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeSetEqBand(JNIEnv*, jobject, jint idx, jfloat db) {
    ensurePlayer()->setEqBand(static_cast<int>(idx), static_cast<float>(db));
}

JNIEXPORT void JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeSetBass(JNIEnv*, jobject, jfloat db) {
    ensurePlayer()->setBassDb(static_cast<float>(db));
}

JNIEXPORT void JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeSetTreble(JNIEnv*, jobject, jfloat db) {
    ensurePlayer()->setTrebleDb(static_cast<float>(db));
}

JNIEXPORT void JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeSetLimiterEnabled(JNIEnv*, jobject, jboolean e) {
    ensurePlayer()->setLimiterEnabled(e == JNI_TRUE);
}

JNIEXPORT void JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeSetLimiterCeiling(JNIEnv*, jobject, jfloat db) {
    ensurePlayer()->setLimiterCeilingDb(static_cast<float>(db));
}

JNIEXPORT void JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeResetDsp(JNIEnv*, jobject) {
    ensurePlayer()->resetDsp();
}


// ── Spatial control ──
JNIEXPORT void JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeSetSpatialEnabled(JNIEnv*, jobject, jboolean e) {
    ensurePlayer()->setSpatialEnabled(e == JNI_TRUE);
}

JNIEXPORT void JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeSetSpatialMode(JNIEnv*, jobject, jint mode) {
    ensurePlayer()->setSpatialMode(static_cast<int>(mode));
}

JNIEXPORT void JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeSetSpatialHeight(JNIEnv*, jobject, jfloat h) {
    ensurePlayer()->setSpatialHeight(static_cast<float>(h));
}

JNIEXPORT void JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeSetSpatialRoom(JNIEnv*, jobject, jint room) {
    ensurePlayer()->setSpatialRoom(static_cast<int>(room));
}

JNIEXPORT void JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeSetSpatialIntensity(JNIEnv*, jobject, jfloat i) {
    ensurePlayer()->setSpatialIntensity(static_cast<float>(i));
}

JNIEXPORT void JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeResetSpatial(JNIEnv*, jobject) {
    ensurePlayer()->resetSpatial();
}


// ── AutoEQ control ──
JNIEXPORT void JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeSetAutoEqEnabled(JNIEnv*, jobject, jboolean e) {
    ensurePlayer()->setAutoEqEnabled(e == JNI_TRUE);
}

JNIEXPORT void JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeAutoEqClear(JNIEnv*, jobject) {
    ensurePlayer()->autoEqClear();
}

JNIEXPORT void JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeAutoEqSetPreamp(JNIEnv*, jobject, jfloat db) {
    ensurePlayer()->autoEqSetPreampDb(static_cast<float>(db));
}

JNIEXPORT jboolean JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeAutoEqAddFilter(
        JNIEnv*, jobject, jint type, jfloat freq, jfloat q, jfloat gainDb) {
    return ensurePlayer()->autoEqAddFilter(
        static_cast<int>(type),
        static_cast<float>(freq),
        static_cast<float>(q),
        static_cast<float>(gainDb)) ? JNI_TRUE : JNI_FALSE;
}


// ── Analyzer control ──
JNIEXPORT void JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeSetAnalyzerEnabled(JNIEnv*, jobject, jboolean e) {
    ensurePlayer()->setAnalyzerEnabled(e == JNI_TRUE);
}

JNIEXPORT void JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeAnalyzerTick(JNIEnv*, jobject) {
    ensurePlayer()->analyzerTick();
}

JNIEXPORT jint JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeAnalyzerBins(JNIEnv*, jobject) {
    return static_cast<jint>(ensurePlayer()->analyzerBins());
}

JNIEXPORT jfloatArray JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeAnalyzerReadSpectrum(JNIEnv* env, jobject) {
    const int bins = ensurePlayer()->analyzerBins();
    std::vector<float> tmp(static_cast<size_t>(bins), 0.0f);
    ensurePlayer()->analyzerReadSpectrum(tmp.data(), bins);
    jfloatArray out = env->NewFloatArray(bins);
    if (out != nullptr) {
        env->SetFloatArrayRegion(out, 0, bins, tmp.data());
    }
    return out;
}

JNIEXPORT jfloat JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeAnalyzerTakePeak(JNIEnv*, jobject) {
    return static_cast<jfloat>(ensurePlayer()->analyzerTakePeak());
}

JNIEXPORT jfloat JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeAnalyzerTakeRmsDb(JNIEnv*, jobject) {
    return static_cast<jfloat>(ensurePlayer()->analyzerTakeRmsDb());
}

JNIEXPORT jboolean JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeAnalyzerIsClipping(JNIEnv*, jobject) {
    return ensurePlayer()->analyzerIsClipping() ? JNI_TRUE : JNI_FALSE;
}


// ── Crossfade / Gapless ──
JNIEXPORT void JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeSetCrossfadeMs(JNIEnv*, jobject, jint ms) {
    ensurePlayer()->setCrossfadeMs(static_cast<int>(ms));
}

JNIEXPORT jint JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeCrossfadeMs(JNIEnv*, jobject) {
    return static_cast<jint>(ensurePlayer()->crossfadeMs());
}

JNIEXPORT jboolean JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeLoadNext(
        JNIEnv* env, jobject, jstring jPath) {
    const char* path = env->GetStringUTFChars(jPath, nullptr);
    std::string error;
    bool ok = ensurePlayer()->loadNext(path ? path : "", error);
    if (path) env->ReleaseStringUTFChars(jPath, path);
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeClearNext(JNIEnv*, jobject) {
    ensurePlayer()->clearNext();
}


// ── ReplayGain ──
JNIEXPORT void JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeSetReplayGainEnabled(JNIEnv*, jobject, jboolean e) {
    ensurePlayer()->setReplayGainEnabled(e == JNI_TRUE);
}

JNIEXPORT void JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeSetReplayGainTargetDb(JNIEnv*, jobject, jfloat db) {
    ensurePlayer()->setReplayGainTargetDb(static_cast<float>(db));
}

JNIEXPORT void JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeSetReplayGainPeakDb(JNIEnv*, jobject, jfloat peakDb) {
    ensurePlayer()->setReplayGainMeasuredPeakDb(static_cast<float>(peakDb));
}

JNIEXPORT jfloat JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeReplayGainCurrentDb(JNIEnv*, jobject) {
    return static_cast<jfloat>(ensurePlayer()->replayGainCurrentDb());
}

JNIEXPORT jfloat JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeReplayGainMeasuredPeakDb(JNIEnv*, jobject) {
    return static_cast<jfloat>(ensurePlayer()->replayGainMeasuredPeakDb());
}


JNIEXPORT jfloat JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeComputeLoadedPeakDb(JNIEnv*, jobject) {
    const float lin = ensurePlayer()->computeLoadedPeak();
    if (lin < 1e-9f) return static_cast<jfloat>(-120.0f);
    return static_cast<jfloat>(20.0f * std::log10(lin));
}


JNIEXPORT jfloat JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeAnalyzeLoadedLufs(JNIEnv*, jobject) {
    float tp = -120.0f;
    return static_cast<jfloat>(ensurePlayer()->analyzeLoadedLufs(tp));
}

JNIEXPORT jfloat JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeAnalyzeLoadedTruePeakDb(JNIEnv*, jobject) {
    float tp = -120.0f;
    (void)ensurePlayer()->analyzeLoadedLufs(tp);
    return static_cast<jfloat>(tp);
}


JNIEXPORT void JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeSetReplayGainDirectGainDb(
        JNIEnv*, jobject, jfloat gainDb) {
    ensurePlayer()->setReplayGainDirectGainDb(static_cast<float>(gainDb));
}

} // extern "C"

#include <jni.h>
#include <memory>
#include <string>

#include "skb_audio_core/skb_version.h"
#include "skb_audio_core/AudioPlayer.h"

static std::unique_ptr<skb::AudioPlayer> gPlayer;

static skb::AudioPlayer* ensurePlayer() {
    if (!gPlayer) {
        gPlayer = std::make_unique<skb::AudioPlayer>();
    }
    return gPlayer.get();
}

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeVersion(JNIEnv* env, jobject) {
    std::string v = "skb-core v";
    v += SKB_VERSION_STRING;
    v += " [";
    v += SKB_CODENAME;
    v += "]";
    return env->NewStringUTF(v.c_str());
}

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
    if (sr <= 0) {
        return env->NewStringUTF("device: not opened yet");
    }
    std::string s = std::to_string(sr) + " Hz | " +
                    std::to_string(p->deviceChannels()) + " ch";
    return env->NewStringUTF(s.c_str());
}

} // extern "C"

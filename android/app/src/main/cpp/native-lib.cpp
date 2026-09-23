#include <jni.h>
#include <string>
#include "skb_audio_core/skb_version.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_skbsakib_audiosuperpower_NativeBridge_nativeVersion(JNIEnv* env, jobject) {
    std::string v = "skb-core v";
    v += SKB_VERSION_STRING;
    v += " [";
    v += SKB_CODENAME;
    v += "]";
    return env->NewStringUTF(v.c_str());
}

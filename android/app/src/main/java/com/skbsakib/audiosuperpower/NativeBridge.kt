package com.skbsakib.audiosuperpower

object NativeBridge {
    init { System.loadLibrary("skb_audio_core") }
    external fun nativeVersion(): String
}

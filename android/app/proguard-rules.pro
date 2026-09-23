# ═══════════════════════════════════════════════════════
#  SKB Audio Superpower — R8 / ProGuard
# ═══════════════════════════════════════════════════════

# ── Native methods — MUST keep JNI signatures ──
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.skbsakib.audiosuperpower.NativeBridge { *; }

# ── Data models used by reflection / DataStore ──
-keepclassmembers class com.skbsakib.audiosuperpower.library.** { *; }
-keepclassmembers class com.skbsakib.audiosuperpower.autoeq.** { *; }
-keepclassmembers class com.skbsakib.audiosuperpower.device.** { *; }

# ── Kotlin metadata safety ──
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod

# ── Coroutines ──
-dontwarn kotlinx.coroutines.**

# ── Compose (AGP handles most; this preserves reflection paths) ──
-keep,allowobfuscation @androidx.compose.runtime.Composable class * { *; }

# ── Debug info: keep line numbers for crash reports ──
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Oboe native ──
-keep class com.google.oboe.** { *; }
-dontwarn com.google.oboe.**

# ── Remove logging in release (small perf + size) ──
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

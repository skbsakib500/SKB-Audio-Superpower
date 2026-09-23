# Development

Requires Termux with: openjdk-17 gradle cmake ninja python3 git gh clang aapt2
SDK: platforms;android-34, build-tools;34.0.0
NDK: 26.1.10909125

Build:
    cd android && gradle :app:assembleDebug

Rules:
- Zero manual editing — regenerate files via scripts.
- Every feature is IMPLEMENTED / PLANNED / EXPERIMENTAL / LIMITED / UNSUPPORTED.
- No fake claims.

#!/data/data/com.termux/files/usr/bin/bash
set -e
source "$HOME/.bashrc" 2>/dev/null || true
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/android"
gradle :app:assembleDebug --no-daemon "$@"
APK="$ROOT/android/app/build/outputs/apk/debug/app-debug.apk"
[ -f "$APK" ] && { ls -lh "$APK"; sha256sum "$APK"; } || { echo "APK missing"; exit 1; }

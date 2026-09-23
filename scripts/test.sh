#!/data/data/com.termux/files/usr/bin/bash
set -e
source "$HOME/.bashrc" 2>/dev/null || true
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/android"
gradle :app:testDebugUnitTest --no-daemon "$@"

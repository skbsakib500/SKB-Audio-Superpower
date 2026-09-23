#!/data/data/com.termux/files/usr/bin/bash
source "$HOME/.bashrc" 2>/dev/null || true
for p in openjdk-17 gradle cmake ninja clang python3 git gh aapt2; do
  command -v "$p" >/dev/null 2>&1 && echo "OK   $p" || echo "MISS $p"
done
echo "ANDROID_HOME=${ANDROID_HOME:-UNSET}"
echo "ANDROID_NDK_HOME=${ANDROID_NDK_HOME:-UNSET}"

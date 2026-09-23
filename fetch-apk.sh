#!/data/data/com.termux/files/usr/bin/bash
# Downloads latest APK from GitHub Actions.
set -e

REPO="skbsakib500/SKB-Audio-Superpower"
OUT="$HOME/SKB-APK"

rm -rf "$OUT"
mkdir -p "$OUT"
cd "$OUT"

echo "── Latest runs ──"
gh run list --repo "$REPO" --limit 3

echo ""
echo "── Waiting for latest completed run ──"
ID=$(gh run list --repo "$REPO" --status success --limit 1 --json databaseId --jq '.[0].databaseId')
echo "Using run: $ID"

echo ""
echo "── Downloading artifact ──"
gh run download "$ID" --repo "$REPO" -D "$OUT" || {
    echo "No artifact on latest run — trying previous..."
    for i in 1 2 3; do
        ID=$(gh run list --repo "$REPO" --status success --limit 5 --json databaseId --jq ".[$i].databaseId")
        [ -z "$ID" ] && continue
        gh run download "$ID" --repo "$REPO" -D "$OUT" && break
    done
}

echo ""
echo "── APK files ──"
find "$OUT" -name "*.apk" -exec ls -lh {} \;

APK=$(find "$OUT" -name "*.apk" | head -1)
if [ -n "$APK" ]; then
    echo ""
    echo "── Installing $APK ──"
    termux-open "$APK"
fi

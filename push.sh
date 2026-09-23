#!/data/data/com.termux/files/usr/bin/bash
# SKB quick-push helper — Termux never builds.
set -e
cd "$(dirname "$0")"

MSG="${1:-sync: $(date '+%Y-%m-%d %H:%M')}"

echo "── Status ──"
git status --short
echo ""

if git diff --quiet && git diff --cached --quiet && [ -z "$(git ls-files --others --exclude-standard)" ]; then
    echo "Nothing to commit."
    exit 0
fi

git add -A
git -c user.name="SKB Dev" -c user.email="msakibalmhamud5@gmail.com" commit -m "$MSG"
git push

echo ""
echo "✅ Pushed to GitHub — Actions will build."
echo "   Watch: https://github.com/skbsakib500/SKB-Audio-Superpower/actions"

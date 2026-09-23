# Changelog

## [0.1.0-alpha.1] — 2026-09-23

**Phase 1B — Skeleton · VALIDATED**

### Added
- Android project (AGP 8.5.2, Kotlin 1.9.24, Compose BOM 2024.06)
- Compose 2080 dark shell (cyan glow, monospaced typography)
- JNI native bridge + CMake target `skb_audio_core`
- Native C++20 skeleton (arm64-v8a only)
- Termux-independent `gradle.properties` (device overrides moved to user-level)
- GitHub Actions: `build.yml` (APK) + `test.yml` (unit tests)
- Scripts: `bootstrap.sh`, `build.sh`, `test.sh`, `clean.sh`
- `.skb/` machine-readable project memory
- Repo docs: ARCHITECTURE, ROADMAP, DECISIONS, KNOWN-ISSUES, DEVELOPMENT, SECURITY

### CI
- ✅ Build APK — run 35821906464
- ✅ Test — run 35821906484

### APK
- Size: 8.7 MB
- SHA256: `fa9a27169f10cc77e7f84f89fc5090ef01b3ce42c7b2f7bb3a4904fdf9e17f08`

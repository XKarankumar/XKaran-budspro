# KARAN BUDS — PREMIUM EDITION

**Ultimate Audio Experience**  
Copyright © Karan

KARAN BUDS PREMIUM is a flagship-grade Bluetooth earbuds companion application for Android built with Kotlin, Jetpack Compose, Material Design 3, Room, DataStore, Android AudioFX, Android 13+ Spatializer, Bluetooth Classic/A2DP/HFP, and BLE GATT.

---

## Core Architecture & Features

1. **Flagship AMOLED & Glassmorphic UI**
   - Pure `#000000` AMOLED luxury canvas with ColorOS 17, Apple AirPods popup, and Nothing minimal futuristic styling.
   - Custom adaptive launcher icon and original 3D studio earbuds & case renders.
   - Bundled geometric typography (`Space Grotesk`, `Plus Jakarta Sans`, `JetBrains Mono`).

2. **Live Earbuds Dashboard & Smart Brand Detection**
   - Automatically identifies 13 brand profiles (`Apple AirPods`, `Samsung Galaxy Buds`, `OnePlus Buds`, `Nothing Ear`, `Realme Buds`, `Oppo Enco`, `Vivo TWS`, `Sony`, `JBL`, `Redmi Buds`, `Xiaomi`, `boAt`, and `KARAN BUDS Universal`).
   - Live connection duration timer, A2DP/HFP/LE Audio profile badges, codec & sample-rate inspection (`SBC`, `AAC`, `aptX`, `aptX HD`, `LDAC`, `LC3`, `Opus`), and separate animated Left/Right/Case battery rings.

3. **Apple-Inspired Floating Connection Popups**
   - 5 customizable popup styles: `Brand-Style`, `Large Card`, `Glassmorphic`, `Compact Pill`, `Minimal Dot`.
   - Animates connection, disconnection, reconnection, and low-battery warnings.

4. **ANC Control Center & Spatial Audio Studio**
   - Noise Cancellation, Transparency, Normal/Off switching, strength tuning, Adaptive ANC, Wind Reduction, and Room-persisted Noise Profiles.
   - 360° Soundstage visualizer with Cinema / Music / Gaming profiles, Android 13+ `AudioManager.spatializer` hardware & head-tracking inspection, plus Android AudioFX Virtualizer.

5. **Pro Audio Lab & Local AI Audio Assistant**
   - 10-band Graphic Equalizer (`31 Hz` – `16 kHz`) with interactive spline curve, 11 built-in presets + custom Room preset saving, Bass Boost, Vocal Clarity, Treble Boost, Loudness Enhancement, L/R Balance, and Sound Stage Width.
   - Built-in real-time `AudioTrack` Acoustic Audition player routed through Android AudioFX.
   - Offline rule-based AI Audio Assistant requiring zero paid API keys.

6. **Battery Intelligence, Find My Earbuds & System Integration**
   - Room-backed Battery History & Connection History logs (`KaranBudsDatabase` v2 with non-destructive `MIGRATION_1_2`).
   - Live BLE RSSI proximity scanner (`dBm`), Stereo Left/Right/Both acoustic chime finder, and permission-gated coarse last-known location.
   - Quick Settings Tile (`TileService`), Home Screen Widget (`AppWidgetProvider`), Android 13+ Notification Channels, Biometric App Lock, and SAF JSON Backup & Restore.

7. **Centralized Haptics Engine (`com.example.haptics`)**
   - Uses `VibratorManager` (Android 12+) / `Vibrator` + `VibrationEffect` amplitude envelopes with `Light`, `Medium`, and `Strong` intensity levels.
   - Respects Android system touch vibration and Battery Saver mode, with per-feature toggles (Connection, ANC/Spatial, UI/Theme, Updates, Low Battery & Errors) and an interactive Tactile Cue Preview Lab.

8. **Verified GitHub Releases Auto-Updater (`com.example.updater`)**
   - Checks official GitHub Releases via `https://api.github.com/repos/<owner>/<repo>/releases/latest` and compares SemVer + `versionCode`.
   - Resumable HTTP `Range` downloads (Pause / Resume / Retry), mandatory SHA-256 verification, package identity & signing certificate verification, and safe handoff via `FileProvider` to Android's native Package Installer (never silent/bypassed installs).

9. **Real Hardware Rule**
   - KARAN BUDS never fabricates battery numbers, GPS coordinates, or unsupported vendor ANC/firmware features. Any capability not exposed by the connected device's Bluetooth profile or Android API clearly displays `"Not supported by this device"`.

---

## Continuous Feature Update & Release Workflow

### Package Modularity
- `com.example.bluetooth`: Device state, brand detection, A2DP/HFP/LE Audio, BLE GATT
- `com.example.audio`: 10-band Equalizer, Spatializer, Acoustic Audition player
- `com.example.haptics`: `HapticsManager`, `HapticEvent`, `HapticsSettings`, intensity scaling
- `com.example.updater`: `GitHubReleaseChecker`, `ApkDownloaderAndVerifier`, `SemVerComparator`, `BuiltInChangelog`
- `com.example.data`: Room `KaranBudsDatabase` (with schema migrations) & DataStore `KaranPreferencesRepository`
- `com.example.ui`: ViewModel, theme system (`KaranBudsTheme`), reusable glass components & screens

### Releasing a New Version
1. Update `version.properties` at the repository root (`VERSION_MAJOR`, `VERSION_MINOR`, `VERSION_PATCH`, `VERSION_CODE`, `GITHUB_OWNER`, `GITHUB_REPO`).
2. Add release highlights to `CHANGELOG.md` and `BuiltInChangelog` (`UpdateModels.kt`).
3. If adding Room tables/columns, increment `@Database(version = ...)` in `KaranBudsDatabase.kt` and add a `Migration(old, new)` so user settings and history are preserved.
4. Commit and push to `main`, then create and push a matching version tag:
   ```bash
   git tag v1.1.0
   git push origin v1.1.0
   ```
5. `.github/workflows/release.yml` automatically runs tests, builds the APK, computes `.sha256` and `version.json`, and publishes a GitHub Release ready for the in-app Update Center.

### Optional GitHub Actions Signing Secrets
To publish production-signed Release APKs from CI without committing keys:
- `RELEASE_KEYSTORE_BASE64`: Base64-encoded `.jks` keystore (`keyAlias = "upload"`)
- `STORE_PASSWORD`: Keystore password
- `KEY_PASSWORD`: Key password

If these secrets are not set, workflows gracefully build and upload Debug APKs without failing.

---

## Build & Setup Instructions

### Prerequisites
- Android Studio Ladybug / Meerkat or newer
- JDK 17+ (Temurin 17 or 21 compatible with AGP 9.1.1 and Gradle 9.3.1)
- Android SDK 36 (`minSdk = 24`, `targetSdk = 36`)

### Build Debug APK
```bash
./gradlew :app:assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Build Release APK
Set keystore environment variables (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`) and run:
```bash
./gradlew :app:assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

### Run Unit & Robolectric Tests
```bash
./gradlew :app:testDebugUnitTest
```

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
   - Room-backed Battery History & Connection History logs.
   - Live BLE RSSI proximity scanner (`dBm`), Stereo Left/Right/Both acoustic chime finder, and permission-gated coarse last-known location.
   - Quick Settings Tile (`TileService`), Home Screen Widget (`AppWidgetProvider`), Android 13+ Notification Channels, Biometric App Lock, and SAF JSON Backup & Restore.

7. **Real Hardware Rule**
   - KARAN BUDS never fabricates battery numbers, GPS coordinates, or unsupported vendor ANC/firmware features. Any capability not exposed by the connected device's Bluetooth profile or Android API clearly displays `"Not supported by this device"`.

---

## Build & Setup Instructions

### Prerequisites
- Android Studio Ladybug / Meerkat or newer
- JDK 17+ (Temurin 17 or 21 compatible with AGP 9.1.1 and Gradle 9.3.1; project compiles Java 11 bytecode)
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

### GitHub Actions CI
Includes `.github/workflows/android-apk.yml` to automatically run unit tests and build Debug APKs on every push/pull request.

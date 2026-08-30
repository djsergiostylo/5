# Project Memory

## Purpose
Professional Android application for thermal and battery monitoring. Repository 5 is the active development workspace.

## Architecture
- Kotlin + Jetpack Compose + Material 3 + MVVM.
- Battery telemetry is acquired directly on Android with `ACTION_BATTERY_CHANGED` and `BatteryManager` properties.
- 3D rendering is native Android with SceneView/Filament; the previous HTML/WebView frontend has been removed.
- Device orientation uses native `SensorManager` / `TYPE_ROTATION_VECTOR` with smoothing.
- Room stores bounded local snapshots and charging sessions for historical analysis.
- There is no remote backend/API in this product; the data path is entirely on-device.

## Product rules
- Show real Android measurements only.
- Derived power is clearly labelled as derived from voltage × current.
- A metric that the device does not expose is shown as `No disponible`.
- No simulation/test controls in the production UI.
- No AdMob, notification permission or unnecessary background service in the current release candidate.
- Full battery state is distinguished from active charging.

## Verification state
- SceneView `4.33.0` was rejected because CI showed Kotlin metadata 2.4.0 while the project compiler expects 2.1.0.
- SceneView `3.1.1` is pinned because its upstream build uses Kotlin `2.1.21`, compatible with this project line.
- A CI run must still finish with: unit tests pass, debug APK assembles, release APK assembles, APKs are verified and artifacts are published.
- Do not call the app Play Store ready until a signed release artifact is produced and manually tested on the target phone.

## Working rules
1. Diagnose failures from actual code/CI before changing versions.
2. Keep manifest permissions minimal and justified.
3. Keep the telemetry pipeline lifecycle-safe.
4. Never fabricate measurements.
5. Never claim successful APK generation without CI evidence and an actual artifact.

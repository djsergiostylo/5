# ABATERI

Professional Android battery and thermal telemetry dashboard built with Kotlin, Jetpack Compose, Material 3, MVVM, Room and WorkManager.

## Current scope

- Live battery temperature, voltage, current and battery-side power.
- Battery level, technology, charging state and Android-reported health.
- Persistent local snapshot history through Room.
- Charge-session tracking (plug-in → unplug/full).
- Experimental charging-time prediction using the current session curve.
- Experimental state-of-health estimate from completed sessions with usable charging telemetry.
- Thermal alert when the battery remains at or above 42 °C for 2 minutes.
- AdMob banner and rewarded-ad integration using Google test ad identifiers for development.

## Architecture

`MainActivity` → `BatteryDashboard` → `BatteryViewModel` → `BatteryMonitorRepository` + Room/WorkManager services.

The repository owns the dynamically registered `ACTION_BATTERY_CHANGED` receiver and uses the application context. No foreground service is required for the core dashboard.

## Build target

- Kotlin 2.1.20
- Android Gradle Plugin 8.9.2
- Gradle 8.11.1
- Compile SDK 35
- Target SDK 35
- Min SDK 26
- Jetpack Compose BOM 2025.05.00
- Room 2.7.1
- WorkManager 2.11.2
- Google Mobile Ads SDK 25.4.0

## Permissions

The app requests `POST_NOTIFICATIONS` on Android 13+ so persistent thermal alerts can be delivered. AdMob dependencies may contribute the networking permissions required by their SDK.

## Measurement limitations

Battery current and other telemetry fields are device-dependent. Current falls back to `BATTERY_PROPERTY_CURRENT_AVERAGE` when instantaneous current is unavailable. Charger input power and true charger-to-battery efficiency are not directly exposed by standard Android battery APIs, so the app does not fabricate an efficiency value.

The SOH and charging-time features are estimates derived from observed telemetry; they should be treated as experimental indicators rather than laboratory-grade measurements.

## Release note

AdMob currently uses Google test application/ad-unit identifiers. Replace them with production identifiers and implement the applicable consent flow before public monetized release.

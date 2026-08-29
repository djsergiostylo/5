# Stylo Battery Monitor

Professional Android battery and thermal telemetry dashboard built with Kotlin, Jetpack Compose, Material 3, and MVVM.

## Scope

The first release focuses on live device battery telemetry:

- Battery temperature in °C.
- Battery voltage in mV.
- Instantaneous battery current in mA, with a signed value where supported.
- Derived battery power in mW.
- Battery level in %.
- Battery technology.
- Charging state.
- Battery health.

The app intentionally does **not** run a foreground service. `ACTION_BATTERY_CHANGED` is a protected sticky broadcast that must be received through a context-registered receiver, so no manifest broadcast receiver or notification permission is required for the core dashboard.

## Architecture

`MainActivity` → `BatteryDashboard` → `BatteryViewModel` → `BatteryMonitorRepository` → Android `BatteryManager` / `ACTION_BATTERY_CHANGED`.

The repository owns the dynamically registered receiver and is backed by the application context. `BatteryViewModel.onCleared()` unregisters the receiver, preventing receiver/context leaks.

## Build target

- Kotlin 2.4.10
- Android Gradle Plugin 9.1.2
- Gradle 9.3.1
- Compile SDK 37
- Target SDK 36
- Min SDK 26
- Jetpack Compose BOM 2026.08.00
- Material 3 1.4.x through the Compose BOM

## Permissions

No dangerous permissions are required. The app does not request location, storage, phone, Bluetooth, notification, or foreground-service permissions.

## Measurement limitation

Android exposes battery telemetry through platform APIs, but exact availability and accuracy of instantaneous current and some other fields are device-dependent. When instantaneous current is unavailable, the app falls back to `BATTERY_PROPERTY_CURRENT_AVERAGE`; if neither is available, current and derived power are shown as unavailable rather than fabricated.

A true charger-to-battery efficiency percentage cannot be calculated from the standard battery APIs alone because charger input power is not exposed as a direct measurement. The dashboard therefore reports battery-side power instead of inventing an efficiency value.

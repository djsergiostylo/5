# Project Memory

## Purpose
Professional Android application for thermal and battery monitoring. This repository is the active development workspace for the real-world comparison against other AI development agents.

## Current stack
- Kotlin
- Jetpack Compose
- Material 3
- MVVM
- Android API 35 for CI/build compatibility
- GitHub Actions for reproducible builds

## Core requirements
- Efficient battery monitoring using `Intent.ACTION_BATTERY_CHANGED`.
- Temperature in °C, voltage in mV, current in mA, battery percentage, technology, charging/status and health.
- Avoid sensor/broadcast leaks. Receiver lifecycle must be tied to the application/view-model lifecycle.
- No unnecessary background service. Therefore no foreground-service or notification permissions unless a future requirement explicitly needs them.
- Dashboard: pure dark visual language, `#121212` background and `#1E1E1E` cards, rounded corners, prominent temperature/battery values, thermal orange/red accents and green charging/healthy indicators, compact 2x2 secondary metrics.
- Do not invent battery efficiency when Android does not expose the required input-power measurement. Clearly distinguish measured values from derived values.

## Build/verification history
- Initial CI attempt failed because Android API 37 was unavailable on the runner.
- CI was changed to API 35 and explicit SDK/build-tools installation.
- Subsequent CI runs reached unit tests but failed there, so APK assembly was skipped.
- The project is not considered successful until unit tests pass, `assembleDebug` succeeds, the APK exists and GitHub Actions uploads the APK artifact.

## Working rules for future agents
1. Inspect the current repository before changing architecture or versions.
2. Diagnose the exact failing build/test step before making dependency changes.
3. Prefer minimal, evidence-based fixes over repeated version changes.
4. Never claim a successful build without an actual successful CI result and APK artifact.
5. Preserve working functionality while fixing failures.
6. After every significant build fix, rerun CI and inspect the result.
7. Keep commits small and descriptive (`feat:`, `fix:`, `test:`, `chore:`, `ci:`, `docs:`).
8. Treat this as a professional production-quality engineering exercise, not a demo.

## Next immediate objective
Find and fix the failing unit tests, then obtain a clean GitHub Actions run that executes tests, assembles the debug APK, verifies it exists, and uploads `app-debug.apk`.

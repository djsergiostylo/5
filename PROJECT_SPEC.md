# Master Project Specification

## 1. Mission
Build a production-quality Android application for real-time thermal and battery monitoring. The repository is the active development workspace and must remain understandable and reproducible for any future development agent.

Quality is judged by clean architecture, compilation without errors, correct Android declarations and permissions, reliable battery telemetry, and faithful, polished UI/UX.

## 2. Product scope
The primary dashboard must monitor and present values exposed by Android battery telemetry:
- Battery temperature in °C, converted from Android's deci-degrees Celsius representation.
- Battery voltage in mV.
- Battery current in mA when the device exposes it.
- Battery level in %.
- Battery technology.
- Charging/status information.
- Battery health.

The app must clearly distinguish measured values from derived values. Never fabricate input power, charging efficiency, or other measurements that Android does not actually expose. If a requested metric cannot be measured reliably on a device, show an explicit unavailable state rather than an invented number.

## 3. Architecture
- Kotlin.
- Jetpack Compose.
- Material 3.
- MVVM.
- Separate battery/telemetry data acquisition from presentation.
- Use `Intent.ACTION_BATTERY_CHANGED` efficiently.
- Receiver lifecycle must be safe and leak-free.
- Prefer lifecycle-aware collection/state propagation and avoid unnecessary background services.
- Do not add a foreground service, notification permission, or other service-related permission unless a future requirement genuinely requires persistent background monitoring.

## 4. UI/UX
Create a compact, technical battery/thermal dashboard:
- Pure dark visual language.
- Background: `#121212`.
- Cards: `#1E1E1E` with rounded corners.
- Strong visual hierarchy with prominent temperature and battery values.
- Thermal state uses orange/red accents where appropriate.
- Charging/healthy states use green accents.
- Secondary telemetry arranged compactly as a 2x2 grid or equivalent responsive layout.
- Secondary metrics include voltage and current; any efficiency display must only appear when its required measured inputs are genuinely available.
- UI should feel precise, technical, readable and uncluttered, with inspiration from KWGT-style technical widgets and modern charging-dashboard concepts, without copying proprietary artwork.
- Mobile-first and suitable for the target Android device.

## 5. Android configuration
- Use a stable Android SDK/toolchain compatible with the project's CI environment. Current project target/build compatibility is API 35.
- Keep the AndroidManifest minimal and explicit.
- Declare only permissions actually required by implemented functionality.
- Do not request notification or foreground-service permissions for functionality that does not need a background service.

## 6. Engineering and verification
Before changing architecture, dependency versions, or build configuration, inspect the current repository and identify the exact failure or requirement driving the change.

Verification is mandatory. A project is not considered finished merely because source code looks correct. The acceptance chain is:
1. Unit tests pass.
2. Android debug build succeeds.
3. `assembleDebug` produces the expected APK.
4. The APK is verified to exist at the expected path.
5. GitHub Actions completes successfully.
6. GitHub Actions publishes the debug APK artifact.

Never claim success without actual evidence from the build/CI result.

Prefer minimal, evidence-based fixes over repeated dependency/version changes. Preserve working functionality while correcting defects. Keep commits small and descriptive using conventional prefixes such as `feat:`, `fix:`, `test:`, `chore:`, `ci:`, and `docs:`.

## 7. Research standard
When a technical decision requires verification, consult current official Android documentation and relevant open-source implementations where appropriate. Useful reference categories include battery-monitoring applications such as Voltascano on F-Droid and Energy Bar on GitHub, while treating their implementations as references rather than specifications. Validate Android API behavior against authoritative documentation before relying on undocumented assumptions.

## 8. Agent operating rules
Any development agent working in this repository must:
- Read `PROJECT_SPEC.md`, `MEMORY.md`, and the current code before making substantive changes.
- Treat the code and CI results as the source of truth for implementation state.
- Diagnose exact failures before changing dependencies or architecture.
- Check imports, manifest declarations, lifecycle behavior, tests and build configuration.
- Make the smallest coherent change that advances the project.
- Run or trigger verification after significant fixes.
- Never silently replace a working implementation with an unrelated architecture.
- Never declare the APK finished until the complete acceptance chain passes.

## 9. Relationship with repository memory
`PROJECT_SPEC.md` defines what the product is supposed to be.
`MEMORY.md` records the current project state, completed work, known failures and next objective.
`AGENTS.md`, when present, defines operational instructions for agents working on the repository.

These documents must remain consistent with the actual source tree and CI state. When they diverge, update the documentation as part of the same coherent maintenance task rather than allowing stale project context to accumulate.

## 10. Original brief preserved
The original development brief called for an autonomous Principal Android Software Engineer approach, a complete compilable project, strict manifest/permission handling, Kotlin + Compose + Material 3 + MVVM, efficient `ACTION_BATTERY_CHANGED` telemetry, accurate mathematical conversions, a compact dark dashboard, and verification against real build results. It also requested research of official Android practices and relevant open-source battery-monitoring projects before implementation decisions.

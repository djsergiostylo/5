# Agent Instructions

## Start here
1. Read `PROJECT_SPEC.md` for the product contract.
2. Read `MEMORY.md` for current state and next objective.
3. Inspect the actual source tree and latest CI results before changing code.

## Non-negotiable engineering rules
- Do not assume the repository is empty or recreate it from scratch without evidence.
- Do not change architecture or dependency versions merely to make an error disappear.
- Diagnose the exact failing task, test, compiler error, or runtime issue first.
- Preserve existing working functionality.
- Keep AndroidManifest permissions minimal and justified by implemented features.
- Keep battery telemetry lifecycle-safe and leak-free.
- Do not invent measurements that Android does not expose.
- Prefer small, coherent commits with conventional messages.
- After a significant fix, run or trigger CI and inspect its actual result.
- Never report a successful APK build unless the build and artifact are actually verified.

## Definition of done
The project is complete only when unit tests pass, the debug APK assembles successfully, the APK exists, and GitHub Actions successfully publishes the APK artifact.

## Documentation consistency
Keep `PROJECT_SPEC.md`, `MEMORY.md`, `AGENTS.md`, source code and CI configuration coherent. If implementation state changes materially, update `MEMORY.md` so the next agent can continue without guessing.

# QA run request — 24 October 2026

The owner explicitly requested: “Check start koro”.

This documentation commit intentionally triggers the `Android candidate checks`
workflow on `main` via its existing `push` trigger. Treat the corresponding
workflow run as **pending/unverified** until the exact run, commit SHA, step logs,
emulator reports, APK signing, checksums, and artifact provenance are available.

Requested validation scope:
1. Compile debug Kotlin with JDK 17/SDK 36/Gradle 8.11.1.
2. Run unit tests, Android lint, debug APK assembly and instrumented test assembly.
3. Run release tests/lint and unsigned AAB bundle (no production signing claim).
4. Verify exact debug APK certificate, zip alignment, binary metadata and artifacts.
5. Run Android API 29, 35 and 36 emulator suites, including story/media/sheet regressions.
6. Run Deno edge type-check and private media boundary tests.
7. Reconcile results with `docs/VIDEO_REVIEW_20261024_IMPLEMENTATION_HANDOFF.md`.

Never mark workflow/device/prod status passing solely because this request commit
exists. Real signed-in two-phone/backend journeys need separate verified evidence.

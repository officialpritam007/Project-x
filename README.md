# Liquid Chat

## Owner PDF upgrade — 4 phases

The [PDF four-phase execution tracker](docs/PDF_FOUR_PHASE_TRACKER.md) records all 12 supplied PDF sections as **243 individual requirements** and cross-lists all **346 original engineering acceptance items**. Phase 1 (UI, navigation, Settings, General configuration) is in source implementation; Phases 2–4 remain open. Unchecked means not accepted/verified, even when supporting code exists.

**Current owner instruction (23 October 2026):** Implement the requested PDF/346-item scope with tests, CI and an installable Android app when actually verified. Source changes may go directly to `main` without pull requests. Candidate workflow builds, unit tests, lint and emulator instrumentation are authorized. Production release signing, provider acceptance and private backend deployment still require appropriate credentials and evidence; never claim a production-ready release on the basis of a compile alone. Do not check unfinished items just to reach a stated percentage.
Current Codex ZIP source-level port: [MBWhatsApp reference transfer](docs/MB_TRANSFER_IMPLEMENTATION.md).
**Build and test permission granted:** owner explicitly requested tests and APK delivery on 23 October 2026. Candidate CI must pass and APK signature/artifact provenance must be checked before distribution. No deployment or signed production release is implied by this authorization.
Native Android one-to-one messaging by Pritam Pal. Kotlin + Compose, Supabase Free, FCM and protected media fallback/optional authenticated Cloudinary. The test candidate enables experimental one-to-one voice/video calls and photo/video Stories. No groups or E2EE claim.

Current scope, verification and explicit limitations: [All-in-One implementation report](docs/ALL_IN_ONE_IMPLEMENTATION_REPORT.md), [one-round two-phone checklist](docs/ALL_IN_ONE_TWO_PHONE_TEST.md), and [Master Tracker #24](https://github.com/officialpritam007/Project-x/issues/24). Candidate CI enables calls; owner-signed production remains separately gated.

## Build / install
JDK 17, Android SDK 36, Gradle 8.11.1. Run `./gradlew testDebugUnitTest lintDebug assembleDebug`. Run the candidate workflow for the complete test, lint, emulator and package checks.
From a phone: open **Actions → Android debug APK → successful run → Artifacts → Liquid-Chat-debug**. Extract the ZIP and install `app-debug.apk`.

## Implementation
Current `app/build.gradle.kts` version is **1.0.0-rc12-liquid-preview (19)**, an **unverified debug candidate** containing new sticker navigation, favorites/recents and privacy regression tests. Historical development notes: [modern messaging upgrade](docs/MODERN_UPGRADE.md).

See [backend setup and remaining release gates](docs/BACKEND_SETUP.md), [progress](docs/PROGRESS.md) and the [PRD](Liquid_Chat_PRD-1.md). Verified upgrades are merged to `main` after CI.


## Verification
- CI: debug build, lint, JVM delivery-state tests, and API 35 emulator Room/cache/migration/Keystore and conversation tests.
- `supabase/tests/authorization.sql`: run as DB owner in an isolated validation session; all fixture effects are rolled back. It verifies the access-control invariants listed in its final result.
- Real Google/SMTP/FCM/Cloudinary and physical-device journeys remain required before public release.

## Production stabilization

See [release audit](docs/PRODUCTION_RELEASE_AUDIT.md), [bug ledger](docs/BUG_LEDGER.md), [QA evidence](docs/QA_MATRIX.md), [release engineering](docs/RELEASE_ENGINEERING.md) and [Play submission drafts](docs/play/STORE_LISTING.md). This is a release candidate, not approval for production publication. Signing, provider/physical-device acceptance and final policy/deletion hosting remain explicit gates.

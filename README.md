# Liquid Chat
Native Android one-to-one messaging by Pritam Pal. Kotlin + Compose, Supabase Free, FCM, Cloudinary. No groups, calls, or E2EE claim.

## Build / install
JDK 17, Android SDK 35, Gradle 8.11.1. Run `./gradlew testDebugUnitTest lintDebug assembleDebug`.
From a phone: open **Actions → Android debug APK → successful run → Artifacts → Liquid-Chat-debug**. Extract the ZIP and install `app-debug.apk`.

## Implementation
Latest: **0.7.0 (7)** — [modern messaging upgrade, verification and remaining gates](docs/MODERN_UPGRADE.md).

See [backend setup and remaining release gates](docs/BACKEND_SETUP.md), [progress](docs/PROGRESS.md) and the [PRD](Liquid_Chat_PRD-1.md). Verified upgrades are merged to `main` after CI.


## Verification
- CI: debug build, lint, JVM delivery-state tests, and API 35 emulator Room/cache/migration/Keystore and conversation tests.
- `supabase/tests/authorization.sql`: run as DB owner in an isolated validation session; all fixture effects are rolled back. It verifies the access-control invariants listed in its final result.
- Real Google/SMTP/FCM/Cloudinary and physical-device journeys remain required before public release.

## Production stabilization

See [release audit](docs/PRODUCTION_RELEASE_AUDIT.md), [bug ledger](docs/BUG_LEDGER.md), [QA evidence](docs/QA_MATRIX.md), [release engineering](docs/RELEASE_ENGINEERING.md) and [Play submission drafts](docs/play/STORE_LISTING.md). This is a release candidate, not approval for production publication. Signing, provider/physical-device acceptance and final policy/deletion hosting remain explicit gates.

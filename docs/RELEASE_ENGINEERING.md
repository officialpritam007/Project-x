# Release candidate engineering

Candidate: `com.pritam.liquidchat`, 1.0.0-rc1 / versionCode 8. Android 10+ (min 29), compile/target 36. AGP 8.10.1, Gradle 8.11.1 and Java 17; existing Kotlin 2.1.20 retained. Official compatibility: https://developer.android.com/build/releases/agp-8-10-0-release-notes . Target policy: https://support.google.com/googleplay/android-developer/answer/11926878 .

## Automatic candidate checks

`android.yml` runs debug and release unit tests/lint, debug APK, minified/shrunk unsigned release AAB, Edge checks, and instrumentation on API 29/35/36. It checks APK signing with apksigner, ZIP native alignment with zipalign -P 16, and all bundled native ELF LOAD alignments with scripts/verify_artifacts.py. Provenance contains source SHA, file sizes and SHA-256; public certificate/config-presence reports contain no private key or configuration values.

The normal release-bundle artifact is explicitly **UNSIGNED / inspection only**. Build/R8 success is not release runtime or physical-device certification. Keep debug APK distribution separate; verify the certificate before attempting an update over an existing install. Do not uninstall with pending unsent messages. An incompatible signing certificate prevents a preserving in-place update; decide a migration/backup route before destructive local changes.

### Development identity continuity correction

Main run39043775751 exposed a real cache-path failure: `~/.android/debug.keystore` did not exist at cache-save time, despite a signed debug APK. Its public certificate differed from baseline and PR7. The follow-up prepares that exact development path and explicitly supplies it to Gradle through LIQUID_DEBUG_KEYSTORE_PATH, so generated or owner-provided keys are actually used. Owner-provided development keys remain excluded from cache. A missing cache may generate a new **development-only** identity; cache is not a durable signing backup. Use the owner DEBUG_KEYSTORE_BASE64 secret for controlled continuity, and inspect certificate reports before every update. This correction cannot recover missing prior private keys or establish a safe upgrade from old installations. Production release signing remains separate and owner-controlled.

## Owner upload signing — blocked until configured

Create/retain an owner-controlled upload key and backup outside this repo. Use Play App Signing as appropriate. Do not paste keys/passwords/service accounts into chat, logs, issue bodies or source. Configure GitHub Actions secrets:

- RELEASE_KEYSTORE_BASE64
- RELEASE_STORE_PASSWORD
- RELEASE_KEY_ALIAS
- RELEASE_KEY_PASSWORD

The workflow creates RELEASE_KEYSTORE_PATH only inside runner temporary storage, disables Gradle caching for the signing job, removes the file even after failure, and uploads only signed APK/AAB, checksums and public certificate metadata. It never substitutes a debug key or auto-generates an owner production key.

Manually run `release.yml` on **main only** after candidate CI for that exact main SHA passes and the four FCM public Actions variables are configured. It checks verifyReleaseSigning, release tests/lint, bundleRelease/assembleRelease, jarsigner/apksigner and alignment. It **does not upload to Google Play, publish a GitHub release or distribute to users**. Missing credentials fail closed with configuration names only. Ownership/recovery of the upload key remains an owner gate.

## Acceptance before any Play upload

Install the signed/minified release via bundletool or a specifically approved Play internal track, test clean install and compatible update, OAuth callback, FCM token registration, private media/voice, 16 KB/Android 16 behavior and two physical accounts. Record actual device models/OS/IME/network cases and signed artifact checksum. Play Console access, account verification/type/date, target audience/content rating, production eligibility, final policies/deletion URL and owner approval are still required.

## Recovery

Every phase uses reviewable PRs; never roll back by deleting user data or editing applied migrations. Revert app source in a new reviewed commit and use a higher versionCode for a hotfix. Keep the new reporting RPC compatible with older clients. Its specific recovery plan is in MODERATION_OPERATIONS.md. Do not automatically downgrade a Room schema or replace a production signing identity. Back up the real database/storage inventory off-repository and rehearse restore before launch; that rehearsal has not run here.

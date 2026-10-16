# Liquid Chat
Native Android one-to-one messaging by Pritam Pal. Kotlin + Compose, Supabase Free, FCM, Cloudinary. No groups, calls, or E2EE claim.

## Build / install
JDK 17, Android SDK 35, Gradle 8.11.1. Run `./gradlew testDebugUnitTest lintDebug assembleDebug`.
From a phone: open **Actions → Android debug APK → successful run → Artifacts → Liquid-Chat-debug**. Extract the ZIP and install `app-debug.apk`.

## Implementation
See [progress](docs/PROGRESS.md) and the [PRD](Liquid_Chat_PRD-1.md). Each implementation step is committed separately to `main`.

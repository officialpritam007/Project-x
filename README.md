<div align="center">

# 💬 Liquid Chat

### A modern, privacy-conscious Android messaging experience

**Project-X · Native Android · Kotlin · Jetpack Compose**

[![Android](https://img.shields.io/badge/Android-10%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack-Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![License](https://img.shields.io/badge/Release-Preview%20Candidate-orange?style=for-the-badge)](#project-status)

**Built by [Pritam Pal](https://github.com/officialpritam007)**

[Features](#-features) · [Tech Stack](#-tech-stack) · [Getting Started](#-getting-started) · [Build & Test](#-build--test) · [Roadmap](#-development-roadmap) · [Connect](#-connect-with-the-developer)

</div>

---

## ✨ About Liquid Chat

**Liquid Chat** is a native Android one-to-one messaging project focused on responsive conversations, practical privacy controls, local-first reliability, and a refined *Liquid Glass-inspired* interface.

The source combines Kotlin, Jetpack Compose, a Room-backed conversation cache, a durable WorkManager outbox, Supabase authentication/data services, and Firebase Cloud Messaging integration. Private media uses a protected Supabase Storage fallback, with optional server-authorized Cloudinary support.

> [!IMPORTANT]
> **Development / preview project — not a production release.** Features listed here are represented in the source, but real-provider integration, complete two-device acceptance, production signing, and final release approval have **not** all been verified. Calls are experimental. **Liquid Chat does not claim end-to-end encryption (E2EE)** and does not offer group messaging.

## 🚀 Features

| Area | In-source capabilities | Verification boundary |
| --- | --- | --- |
| 💬 **One-to-one chat** | Text conversations, replies, reactions, edits, search, message states, local cache, offline outbox | Full physical-device/network acceptance pending |
| 🖼️ **Private media** | Photos, videos, media previews, sharing and protected access paths | Provider-specific and real-device checks required |
| 🎤 **Voice messages** | Recording, playback, seek and audio-focus management | Device/audio-route validation required |
| 📞 **Voice & video calling** | Experimental WebRTC one-to-one calls | Not production-certified; restricted NAT may fail without configured TURN |
| ⏳ **Stories & Posts** | Media-based Stories and Posts flows | Final two-device and provider tests pending |
| 🔐 **Accounts & privacy** | Supabase Auth, profile management, contact controls, blocking, reporting and account erasure paths | Google/SMTP credentials and live acceptance required |
| 🔔 **Notifications** | FCM client integration, notification controls and server dispatch paths | Actual FCM delivery requires provider setup/testing |
| 🎨 **Personalization** | Light/dark appearance, glass-inspired UI, wallpapers and display preferences | Accessibility, performance and visual-parity checks ongoing |

**Design priorities:** smooth navigation, legible chat bubbles, clear message status, account isolation, accessible controls, and resilient behavior on unreliable networks.

## 🧰 Tech Stack

| Layer | Technology |
| --- | --- |
| Language | **Kotlin** |
| Android UI | **Jetpack Compose**, Material 3 |
| Local persistence | **Room** |
| Background processing | **WorkManager**, Kotlin Coroutines |
| Authentication & backend | **Supabase Auth**, PostgreSQL, Realtime, Edge Functions |
| Push notifications | **Firebase Cloud Messaging (FCM)** |
| Protected attachments | **Supabase Storage** fallback; optional authenticated **Cloudinary** |
| Calling | **WebRTC** (experimental) |
| Media | **AndroidX Media3**, **Coil** |
| Build tooling | **Gradle 8.11.1**, **Android Gradle Plugin 8.10.1**, **JDK 17** |
| Android support | **minSdk 29 (Android 10)** · **compileSdk/targetSdk 36** |

## 🏗️ Project Structure

~~~text
Project-x/
├── app/                          # Native Android app (Compose, data, calls, tests)
├── supabase/                     # Database migrations, Edge Functions & SQL tests
├── docs/                         # Engineering, QA, operations and release docs
├── .github/workflows/            # CI, test and APK workflows
├── Liquid_Chat_PRD-1.md          # Product requirements
├── build.gradle.kts              # Android/Kotlin plugin configuration
├── settings.gradle.kts           # Gradle module configuration
└── README.md
~~~

## ⚡ Getting Started

### Prerequisites

- **Android Studio** with **Android SDK 36**
- **JDK 17**
- Android emulator or phone running **Android 10+**
- Network access for dependency resolution and supported backend features

### 1. Clone

~~~bash
git clone https://github.com/officialpritam007/Project-x.git
cd Project-x
~~~

### 2. Build the Android app

On macOS/Linux:

~~~bash
./gradlew assembleDebug
~~~

On Windows:

~~~powershell
.\gradlew.bat assembleDebug
~~~

The debug APK is normally generated at **app/build/outputs/apk/debug/app-debug.apk**.

### 3. Configure services for real-account testing

Backend-dependent flows are **not** guaranteed to work from an APK alone. Read the [Backend Setup Guide](docs/BACKEND_SETUP.md) and verify:

1. Supabase authentication, allowed OAuth redirects, and relevant backend migrations/functions.
2. The FCM Android configuration and corresponding GitHub Actions variables (**FCM_APP_ID**, **FCM_PROJECT_ID**, **FCM_API_KEY**, **FCM_SENDER_ID**) for push-enabled builds.
3. The matching server-side FCM service account for actual notification delivery.
4. Protected media access and the optional Cloudinary **server-side** configuration.
5. Real-device sign-in, messaging, notification, media and call paths.

> [!CAUTION]
> Do not commit keystores, private API secrets, service account JSON, passwords, user chat exports, or real user data. Cloudinary private credentials belong only in trusted server-side secrets. See [Backend Setup](docs/BACKEND_SETUP.md) for configuration and security boundaries.

## 🧪 Build & Test

Run the standard Android debug checks:

~~~bash
./gradlew testDebugUnitTest lintDebug assembleDebug
~~~

For integrated validation, use [GitHub Actions](https://github.com/officialpritam007/Project-x/actions) and inspect the **exact commit**, test results, signing certificate, and APK provenance. CI and emulators do not substitute for physical-device acceptance.

### 📲 Install a debug candidate

1. Open [**Actions**](https://github.com/officialpritam007/Project-x/actions).
2. Open a **successful Android debug APK** workflow run.
3. Download the **Liquid-Chat-debug** artifact (or the similarly named candidate artifact in that run).
4. Extract the ZIP and install **app-debug.apk** on a compatible test device.

**Important:** Debug APKs are development candidates, not signed production releases. Installing a build with a different signing certificate may prevent an in-place update; do not uninstall an app containing unsent messages without a safe migration plan.

## 📌 Project Status

**Current source configuration:** **1.0.0-rc12-liquid-preview** (versionCode **19**), Android **10+**. This is an **unverified debug candidate**, not an approved public release.

The owner-requested PDF expansion is tracked across **4 phases / 12 sections (243 PDF requirements)** alongside a separate **346-item engineering acceptance inventory**. In the trackers, **unchecked means not accepted/verified**, even if supporting source exists.

| Area | Status |
| --- | --- |
| Native Android source and core messaging implementation | In repository |
| Experimental voice/video calls and Stories | Preview source; acceptance pending |
| Automated unit/lint/emulator/SQL verification | Workflows and tests available; confirm exact current-main run |
| Provider acceptance, two-phone tests, accessibility and performance | Still required |
| Production signing, policy/release gates and Play publication | **Not approved / not complete** |

See [Master Tracker #24](https://github.com/officialpritam007/Project-x/issues/24) for source/build evidence and open gates. Do not interpret past CI successes as validation of an untested newer commit.

## 🗺️ Development Roadmap

- [ ] **Phase 1:** UI, navigation, Settings and general configuration — validate against tracker
- [ ] **Phase 2–4:** Complete the remaining PDF requirements and cross-check the engineering matrix
- [ ] Run current-main unit tests, lint, emulator checks, backend checks and candidate APK validation
- [ ] Complete genuine two-phone, Google/SMTP, FCM, protected-media, voice/video and accessibility acceptance
- [ ] Verify owner-controlled release signing, final policy/deletion hosting and all release requirements

**Owner workflow note (23 October 2026):** Source changes are authorized directly on **main**; pull requests are not required. The owner authorized candidate builds, CI checks, unit tests, lint, emulators and an installable **debug** APK when actually verified. Production signing, deployment and distribution require separate evidence and credentials. Do **not** mark unfinished requirements complete merely to improve a progress percentage.

## 📚 Documentation

| Document | Purpose |
| --- | --- |
| [Product Requirements](Liquid_Chat_PRD-1.md) | Product goals, journeys and acceptance criteria |
| [4-Phase PDF Tracker](docs/PDF_FOUR_PHASE_TRACKER.md) | Owner-requested PDF and engineering requirements |
| [All-in-One Implementation Report](docs/ALL_IN_ONE_IMPLEMENTATION_REPORT.md) | Implemented behaviors and evidence boundaries |
| [Backend Setup](docs/BACKEND_SETUP.md) | Supabase, auth, FCM, private media and configuration |
| [Two-Phone Test Checklist](docs/ALL_IN_ONE_TWO_PHONE_TEST.md) | Physical-device scenarios and acceptance |
| [Production Release Audit](docs/PRODUCTION_RELEASE_AUDIT.md) | Outstanding release gates |
| [QA Evidence](docs/QA_MATRIX.md) | Test coverage and qualification |
| [Bug Ledger](docs/BUG_LEDGER.md) | Known issues and investigation |
| [Release Engineering](docs/RELEASE_ENGINEERING.md) | Artifact, signing and recovery guidance |
| [Progress Log](docs/PROGRESS.md) | Historical development milestones |

## 👨‍💻 Developer

<div align="center">

### Pritam Pal
**Android Developer · Kotlin · Jetpack Compose**

Building modern Android apps with thoughtful UI, practical features, and an emphasis on reliability.

[![GitHub](https://img.shields.io/badge/GitHub-officialpritam007-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/officialpritam007)

### 🌐 Connect with the Developer

[![Facebook](https://img.shields.io/badge/Facebook-Follow-1877F2?style=for-the-badge&logo=facebook&logoColor=white)](https://www.facebook.com/officialpritam.007)
[![Instagram](https://img.shields.io/badge/Instagram-Follow-E4405F?style=for-the-badge&logo=instagram&logoColor=white)](https://www.instagram.com/officialpritam.07)
[![YouTube](https://img.shields.io/badge/YouTube-Subscribe-FF0000?style=for-the-badge&logo=youtube&logoColor=white)](https://www.youtube.com/@officialpritam07)
[![Telegram](https://img.shields.io/badge/Telegram-Connect-26A5E4?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/Officialpritam007)

---

**⭐ If you're interested in Liquid Chat, star the repository and follow its development.**

**Made with ❤️ by [Pritam Pal](https://github.com/officialpritam007)**

</div>

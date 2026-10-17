# LIQUID CHAT — PRODUCT-READY v1.0: MASTER WORK PLAN

**Prepared:** 17 October 2026  
**Repository:** https://github.com/officialpritam007/Project-x  
**Default branch:** `main` (observed baseline `db1b6d6085b508e15756a1cdfe3c0d1f82782029`)  
**Known app:** `com.pritam.liquidchat`, version `0.7.0` (`versionCode=7`), `minSdk=29`, `targetSdk=35`  
**Stack:** Kotlin, Jetpack Compose, Material3, Room 7, WorkManager, Supabase Free (Auth/Postgres/RLS/RPC/Realtime/Storage/Edge Functions), Firebase Cloud Messaging (push only), optional private Cloudinary; currently private Supabase Storage fallback  
**Important:** This is a work queue, not an assertion that every item is a confirmed defect or that the app is already production-ready.

## Instructions to ChatGPT Work / coding agent

Act as the senior Android architect, Kotlin/Compose engineer, backend engineer, security reviewer, QA lead and Google Play release engineer for this repository. **Execute this checklist phase by phase, not just produce another plan.** For every step:

1. Inspect actual repository files, project configs, migrations, Actions workflows and connected backend before editing. Re-check branch/PR status; old state can change. Categorize each issue as `REPRODUCED`, `SOURCE-CONFIRMED`, `RISK / NEEDS TEST`, `ALREADY FIXED`, `BLOCKED BY OWNER/PROVIDER` or `DEFERRED P2`.
2. Preserve requirements: **one-to-one conversations only, no group chat, no voice/video calling**, no Communities tab. Keep existing Stories/Posts unless the owner explicitly changes scope. Bottom tabs **Chats / Contacts / Settings / Profile**, icon-only; consistent iOS-26-inspired Liquid Glass. Keep Android 10+ support unless infeasible and owner approves a change.
3. **Keep Supabase Free**, using secure provider-side configuration. Do not create paid projects, add billing, upgrade subscriptions, enable paid services, or introduce secret leakage. FCM remains for push; Cloudinary is optional, current protected Supabase Storage fallback must keep working. Do not claim end-to-end encryption; no E2EE currently exists.
4. Write small, reviewable fixes. Use short-lived feature branches/PRs, run static checks, unit/instrumentation tests and any relevant Supabase integration/regression checks, then merge passing phases to `main`. Record commit SHA, failing/passing test output and APK/AAB artifacts after every phase. **Do not merge broken work to `main`.** Never edit previously applied Supabase migrations; add new additive migration(s) only after a backup/rollback plan.
5. No destructive SQL, data wipes, production-account deletion tests, secret rotation, changes to billing, or irreversible Play publishing without explicit owner confirmation. Use isolated fixture accounts and rollback-only server tests, clean up fixtures and never expose secret values, tokens or private messages in PR/logs.
6. Verify results on at least two physical Android devices/accounts where available; if unavailable, mark as **blocked, not passed**. Build success is not proof of real-world reliability. Do not promise a same-day public Play Store release if Google review/closed testing is required.
7. At end of each phase update `docs/PRODUCTION_RELEASE_AUDIT.md`, `docs/BUG_LEDGER.md`, `docs/QA_MATRIX.md`, `docs/RELEASE_NOTES_v1.0.md` (or appropriate existing docs), including checklists, evidence links and release blockers.
8. Start with P0 reliability and publishing gates, then P1 quality/security/UI, then conditional P2. New features must not delay a stable first release.

## Phase 0 — Audit and triage (P0)

- [ ] P0-01 Fetch `main`, branch status and open PRs; inspect PR #2 `fix/composer-keep-keyboard-open` (keyboard closing on send), CI and merge eligibility.
- [ ] P0-02 Map app modules/screens/navigation, data models, background workers, Supabase RPCs/RLS, media providers and FCM pipeline.
- [ ] P0-03 Inventory every feature as implemented / partial / broken / unverified / future scope.
- [ ] P0-04 Re-run full Android CI baseline (`testDebugUnitTest`, `lintDebug`, `assembleDebug`, `connectedDebugAndroidTest` where emulator available) and Edge checks.
- [ ] P0-05 Audit current Supabase migration versions, security/performance advisors, cron jobs, Edge status and free-plan usage. Previous check: project healthy, 25 migrations and 4 active functions.
- [ ] P0-06 Inventory owner/provider setup gates without reading or printing secrets: FCM public Android config in Actions variables, server FCM service account, Google OAuth, SMTP, Cloudinary optional credentials.
- [ ] P0-07 Create severity-based bug ledger: exact reproduction steps, impacted code, symptoms, fix/owner/test link, status.
- [ ] P0-08 Identify Play Console access, developer account type/creation date, existing closed-test/production eligibility and app package registration; do not assume.
- [ ] P0-09 Freeze initial v1.0 scope; prioritize non-crashing 1:1 text/image/voice conversations, auth, privacy, notification and stable UI.

## Phase 1 — Critical messaging reliability (P0)

- [ ] MSG-01 Fix keyboard dismissing after tapping Send. Preserve IME focus and input usability; evaluate and test PR #2 before merging.
- [ ] MSG-02 Preserve text entered while the prior message is still queueing; do not clear a newer draft or reply target.
- [ ] MSG-03 Ensure rapid repeated taps cannot send duplicate messages; preserve stable client message UUID and idempotent RPC semantics.
- [ ] MSG-04 Prove queued → sending → sent → delivered → read state is monotonic, accurate and consistent between both accounts.
- [ ] MSG-05 Handle network loss/timeout, server errors, expired JWTs and restart without losing unsent text/image/voice messages.
- [ ] MSG-06 Ensure failed messages stay FAILED with visible Retry/Discard instead of cycling endlessly through SENDING.
- [ ] MSG-07 Test retry after unknown server acknowledgment; deduplicate on server and reconcile local snapshot reliably.
- [ ] MSG-08 Fix missing/delayed second tick or read tick under app background, reconnect and cross-device sessions.
- [ ] MSG-09 Check recipient-side incoming ordering, time labels, ownership IDs and wrong-sender/wrong-conversation search attribution.
- [ ] MSG-10 Test delete-for-me (server/private + Room suppression) across app restart, log-in again, another device and worker races; hidden items must not reappear.
- [ ] MSG-11 Test delete-for-everyone tombstones across clients, media URL revocation and 48-hour ownership/window enforcement.
- [ ] MSG-12 Test edit (15-minute window), reaction toggling, reply references and starred/private mutation catch-up.
- [ ] MSG-13 Test chats with 1,000+ messages: pagination, first page, older history, stable viewport and unread count.
- [ ] MSG-14 Verify new messages never jump the viewport while the user is reading older history; jump-to-latest works.
- [ ] MSG-15 Avoid full-refresh flicker on each sync, duplicate rows, blank message placeholders and excessive notifications of Room observers.
- [ ] MSG-16 Ensure contact block in either direction prevents disallowed messaging/media/presence and works against direct RPC bypass attempts.
- [ ] MSG-17 Make message preview correct for pending/failed/edited/deleted/hidden/media messages; no old encrypted placeholder.
- [ ] MSG-18 Test manual unread vs actual read receipts and privacy preference interactions.

## Phase 2 — Auth, session, privacy, lifecycle (P0/P1)

- [ ] AUTH-01 Verify email sign up, confirmation link, sign in, validation, verification-required errors and recovery via actual SMTP provider.
- [ ] AUTH-02 Verify Google OAuth PKCE + mobile deep link callback against actual Supabase redirect URL and Google provider settings.
- [ ] AUTH-03 Test password reset link on the initiating device, expired link, link replay and user reauthentication.
- [ ] AUTH-04 Fix sign-out when offline/server unavailable: ensure local session, workers, Room cache and sensitive state are cleared securely; report remote revocation status honestly.
- [ ] AUTH-05 Test account switch and pending WorkManager/FCM jobs for cross-account data leaks.
- [ ] AUTH-06 Validate serialized token refresh, revoked-session handling, expired refresh tokens and recovery from transient HTTP errors.
- [ ] AUTH-07 Resolve cached-profile absence while offline without indefinite loading state; provide retry/offline explanation.
- [ ] AUTH-08 Verify app process death, relaunch, force-stop and device reboot, with safe session restoration.
- [ ] AUTH-09 Verify in-app account erasure including auth identity, associated user-controlled data and media cleanup; handle partial retries idempotently.
- [ ] AUTH-10 Provide an accessible external web account deletion request path for Google Play, independent of app install.
- [ ] AUTH-11 Validate online/last seen privacy flags, hidden presence, typing TTL, stale timestamp handling and visibility after blocking.
- [ ] AUTH-12 Confirm Android Keystore encrypted session/PKCE tokens; no service-role secrets in app, source, logs or unprotected assets.
- [ ] AUTH-13 Review local chat history exposure, device backups (`allowBackup=false`), screenshot policy and realistic privacy disclosures.
- [ ] AUTH-14 Ensure report/block, contacts, favorite, mute, pinned chat, hidden messages and settings are account-scoped and survive expected reinstall/login scenarios.

## Phase 3 — FCM, realtime, background delivery (P0)

- [ ] PUSH-01 Confirm FCM app initialization values in **GitHub Actions vars** (not secrets in repo): APP_ID, PROJECT_ID, API_KEY, SENDER_ID; avoid printing values unnecessarily.
- [ ] PUSH-02 Confirm Supabase notify function has correct server-side FCM HTTP v1 service account permissions; do not upload credentials to app or repository.
- [ ] PUSH-03 Test foreground active chat: do not show redundant notification; message arrives directly in timeline.
- [ ] PUSH-04 Test foreground different chat, background, screen off, terminated process, device restart and Doze; document Android OEM limitations.
- [ ] PUSH-05 Verify muted/blocked/deleted/hidden messages and previews-off preferences are enforced by fresh authorization check.
- [ ] PUSH-06 Verify notification taps open only recipient's currently authenticated account and correct conversation; never leak old account content.
- [ ] PUSH-07 Handle FCM token rotation, device registration/removal, logout, multiple accounts and duplicate dispatch IDs.
- [ ] PUSH-08 Inspect queued/failed push jobs, retry/backoff/lease expiry and server notification dispatch cron.
- [ ] PUSH-09 Ensure read/delivered ticks are advanced only by legitimate client acknowledgments, not merely by push submission.
- [ ] PUSH-10 Realtime connection reconnect, hints and polling should converge to server state without 100%-reliable-push assumptions.
- [ ] PUSH-11 Ask notification permission when appropriate; provide in-app explanation and settings recovery after denial.

## Phase 4 — Media, voice, Stories/Posts, quotas (P0/P1)

- [ ] MEDIA-01 Verify image gallery/camera permission path, capture file-provider URI, cropping/size handling and orientation/EXIF handling.
- [ ] MEDIA-02 Verify image upload pending → complete → failure/retry, caption preserved and file cleanup after upload or deletion.
- [ ] MEDIA-03 Verify voice hold/release/send, swipe-cancel, lock, interruption, AAC format, duration/size bounds and empty/error states.
- [ ] MEDIA-04 Verify background/navigate-away during recording/playback cleans up mic/player safely.
- [ ] MEDIA-05 Test private photo/voice URL access for owner, recipient, unrelated account, blocked account and after delete.
- [ ] MEDIA-06 Validate short-lived signed URLs expire and are not copied into public app logs/caches.
- [ ] MEDIA-07 Test Cloudinary optional private-provider configuration only if secrets are available; otherwise preserve tested Supabase private fallback.
- [ ] MEDIA-08 Validate Supabase fallback upload limits (documented 100 MB finalized per account / 512 MB global fallback) and friendly quota-exceeded UI.
- [ ] MEDIA-09 Verify abandoned upload cleanup, expiry, orphan prevention and proper cron recovery.
- [ ] MEDIA-10 Test media viewer loading/zoom/pan/dismiss without flicker or invalid account/session cache.
- [ ] MEDIA-11 Test 24-hour Story expiration, contact audiences, owner deletion, feed refresh, private media and storage cleanup.
- [ ] MEDIA-12 Test Post create/browse/archive/restore/delete, private contacts audience, block revocation and pagination.
- [ ] MEDIA-13 Test shared-media images/voice/starred tabs, pagination, account switching, error/empty states and avoid unnecessary dialog reload.
- [ ] MEDIA-14 Handle poor network quality, interrupted downloads and reduced-data/network behavior gracefully.

## Phase 5 — Liquid Glass design system and final UI polish (P1)

- [ ] UI-01 Preserve Figma-based iOS-26-inspired style; unify design tokens for colors, 20–30dp rounded squircle corners, stroke, highlight, shadow, blur, elevation and spacing.
- [ ] UI-02 Real source-backed blur on Android 12+; readable low-cost translucent fallback on Android 10/11; no blurred text.
- [ ] UI-03 Make 4 icon-only bottom tabs Chats/Contacts/Settings/Profile, with proper accessibility labels, selected state, navigation-bar insets and spring motion.
- [ ] UI-04 Chats header, search, All/Favorite/Archive/Unread capsules, online ring/avatar, unread badges, timestamp, preview and pinned state.
- [ ] UI-05 Conversation header avatar/online/last seen, no top-bar typing; only inline typing bubble in timeline.
- [ ] UI-06 Message bubble adapts to short/long content and media; no oversized blank cards or clipped edges.
- [ ] UI-07 Consistent outgoing/incoming spacing, date chips, readable timestamps, sent/delivered/read/error statuses.
- [ ] UI-08 Smooth swipe reply, quote display, long-press actions, reactions, edit, delete, stars and selection feedback.
- [ ] UI-09 Smooth navigation and timeline anchoring; no avatar flicker, screen flashes, repeated image loads or keyboard jumps.
- [ ] UI-10 Composer text field remains focused after send, expands to multiple lines, restores drafts and handles emoji cursor insertion.
- [ ] UI-11 Default new sentence capitalization, and requested initial-message uppercase, without mutating intentional lowercase, URLs, usernames, code or other composing IME text. Verify Gboard/Vivo IME.
- [ ] UI-12 Polished attachment sheet, image preview and upload indicators; voice record, lock, cancel and playback UI.
- [ ] UI-13 Profile hero/avatar/username/bio, Contacts screen, Stories, Posts, Account settings, Privacy and Data/Storage consistent styling.
- [ ] UI-14 Light and dark modes, three animation settings, reduced motion and reduced transparency are respected.
- [ ] UI-15 Skeleton/loading/error/retry/empty/offline, quota-exceeded and blocked-state visual feedback on all applicable screens.
- [ ] UI-16 Back gestures, modal dismissal, system IME and edge-to-edge behavior on Android 10 through 16.
- [ ] UI-17 Support 200% font scaling, TalkBack labels, contrast, 48dp touch targets where appropriate and RTL/long labels where relevant.
- [ ] UI-18 Profile/login branding consistent; app icon, notification small icon, adaptive icon and splash screen look production-grade.
- [ ] UI-19 Eliminate fake/inert controls; explicitly label deferred features unavailable rather than pretending they work.

## Phase 6 — Performance and architecture (P1)

- [ ] PERF-01 Profile actual Compose recompositions, LazyColumn keys, item content types and layout/measurement at 1,000+ messages.
- [ ] PERF-02 Bound Room message queries and large-list allocations; adopt incremental loading/Paging if metrics justify.
- [ ] PERF-03 Debounce draft persistence while maintaining crash/process-death durability; guard IME composing text and race-free send.
- [ ] PERF-04 Reduce polling every 3 sec for contact state and full chat synchronization load; favor correct realtime + bounded fallback and battery-aware strategy.
- [ ] PERF-05 Optimize Flow distinctUntilChanged, state scoping and cancellation; avoid UI thread I/O.
- [ ] PERF-06 Prevent image re-download/flicker; ensure user/account-safe caching and proper cache invalidation.
- [ ] PERF-07 Measure frame times, jank, cold start, memory, battery and data usage on real lower-end/older devices.
- [ ] PERF-08 Refactor large `ChatScreens.kt` and `ChatRepository.kt` into testable UI/repository modules without feature regression.
- [ ] PERF-09 Audit WorkManager unique-work behavior and outbox growth under burst sends; avoid unnecessary endless chains.
- [ ] PERF-10 Ensure battery optimization and Android background restrictions do not corrupt data or give false status.

## Phase 7 — Security, abuse and policy (P0/P1)

- [ ] SEC-01 Run Supabase RLS/security advisor and tests for public table access; enforce per-user/per-conversation ownership including RPCs and views.
- [ ] SEC-02 Review `SECURITY DEFINER`, grants, session_id enforcement and block checks; no public/excessive EXECUTE permission.
- [ ] SEC-03 Confirm only publishable Supabase key in APK, no Supabase service key/FCM service account/Cloudinary API secret in client or public GitHub.
- [ ] SEC-04 Adversarial test for private stars, hidden messages, contact prefs, shared media and unauthorized download/replay.
- [ ] SEC-05 Review passwords, email verification, reset flow, brute-force protections and current Auth leaked-password warning. Do not buy a plan silently.
- [ ] SEC-06 Implement visible Terms/User Policy acceptance prior to uploading user-generated content, with prohibited-content rules.
- [ ] SEC-07 Provide in-app Report **content** and Report/Block **user** where applicable for chats, Stories/Posts; build owner-operable moderation triage/report review workflow.
- [ ] SEC-08 Add abuse rate limiting/quotas for account creation, message operations, uploads and reports; explain recoverable limits to users.
- [ ] SEC-09 Publish correct privacy policy covering chat text, profile, images, voice, device tokens, authentication, storage processors, deletion, retention and third-party providers.
- [ ] SEC-10 Accurate Data Safety declaration; never claim E2EE or zero data collection. State limitations of delete for everyone and downloaded copies.
- [ ] SEC-11 In-app and external web account deletion routes function; retention exceptions documented; audit deletion completeness.
- [ ] SEC-12 Review sensitive logs/crash reports; never log message body, OTP, password, access token, signed media URL or PII.
- [ ] SEC-13 Check dependency licenses, vulnerabilities, Gradle/Deno locks, Java 17 and required Android permissions only.
- [ ] SEC-14 Check Supabase advisors: previous check showed 1 Auth warning (leaked password protection disabled) and 3 unused-index informational notices; don't remove new indexes without workload evidence.

## Phase 8 — Release engineering and distributable app (P0)

- [ ] BUILD-01 Confirm app ID `com.pritam.liquidchat`, chosen versionCode increment and versionName e.g. 1.0.0; decide branding/release name.
- [ ] BUILD-02 Update compile SDK/toolchain and `targetSdk` to at least 36 for October 2026 Google Play submissions, then verify Android 16 behavior and policy exceptions only if owner has actual extension.
- [ ] BUILD-03 Preserve Android 10 (minSdk 29) and validate OS-specific behavior, permissions and UI.
- [ ] BUILD-04 Implement secure **release signing**, preferably with Play App Signing upload key; key managed outside repo, encrypted and recoverable by owner.
- [ ] BUILD-05 Add CI `bundleRelease` for signed `.aab`; keep separate `assembleDebug` APK for beta/QA and optionally signed universal release APK for direct authorized distribution.
- [ ] BUILD-06 Test R8/minification/resource shrinking cautiously; keep required reflective/serialization/FCM classes; verify release build logs.
- [ ] BUILD-07 Android 15/16 edge-to-edge, foreground service/microphone limitations, notification permission, deep link and target API compatibility checks.
- [ ] BUILD-08 Check 16 KB page-size/native-library compatibility if app bundles include native libraries; inspect APK analyzer/dependencies.
- [ ] BUILD-09 Validate application label, adaptive and monochrome icon, splash branding, only necessary manifest permissions, links and network config.
- [ ] BUILD-10 Generate SHA-256, size, version, commit SHA, checksum and reproducible GitHub Actions artifacts; never upload signing keys as artifacts.
- [ ] BUILD-11 Verify signed **release** builds on clean install and update from existing install where signing keys match; document debug-key mismatch migration procedure and safeguard pending messages.
- [ ] BUILD-12 Provide artifact links, installation instructions, changelog, known limitations, rollback/hotfix path and a production readiness report.

## Phase 9 — Automated QA + physical device acceptance (P0)

- [ ] QA-01 All unit, lint, Edge typechecks and media parsers tests green.
- [ ] QA-02 Supabase authorization/message-actions/voice/erasure rollback-only tests on compatible deployed schema, no orphan fixture accounts.
- [ ] QA-03 Android emulator instrumentation: Room v1→v7 migrations, drafts, work retries, navigation, 1k messages, accessibility and state restore.
- [ ] QA-04 Physical device A & B with distinct accounts: text, image, voice, replies, edits, deletes, stars, online and read receipts.
- [ ] QA-05 Offline/Wi-Fi/cellular/airplane mode, network flap, timeout, process death, force stop, restart and device reboot.
- [ ] QA-06 Repeat login/logout/account switch; no previous-account cached images/chat/messages/notification deep links.
- [ ] QA-07 Camera/microphone interrupts; background/notification permission; Gboard and Vivo IME focus after every send.
- [ ] QA-08 Test Android 10, 11, 12, 13, 14, 15, 16 where available; at minimum API 29/35/36 emulators plus current physical target device.
- [ ] QA-09 Portrait, small phone, high font scale, dark mode, reduced animations and accessibility/TalkBack.
- [ ] QA-10 Supabase quota/edge outage/email/Google provider unavailable: honest error messages, offline integrity, no paid fallback.
- [ ] QA-11 Profile and search isolation, stranger/blocked participant access denial and media link revocation after delete.
- [ ] QA-12 Release AAB installation via Play internal track (or bundletool before upload); confirm release build behavior vs debug.
- [ ] QA-13 Capture reproducible evidence screenshots/logs with no private user data, annotated statuses, device models and pass/fail outcomes.
- [ ] QA-14 No open P0, and P1 issues either fixed or explicitly accepted with rationale; no flaky core acceptance.

## Phase 10 — Play Console readiness (P0 / owner steps)

- [ ] PLAY-01 Check actual developer-account verification, package registration, payment profile and account-level Play policy tasks; do not invent access.
- [ ] PLAY-02 Check personal-account closed-testing eligibility: new personal accounts created after 13 Nov 2023 generally need >=12 continuously opted-in testers for >=14 days before production-access application.
- [ ] PLAY-03 Upload signed AAB to **Internal testing** first and install it, if the owner has Play Console access and accepts submission.
- [ ] PLAY-04 Prepare Closed Testing release as needed, tester emails, opt-in link, feedback questions and measured test period.
- [ ] PLAY-05 Accurate listing: Liquid Chat name, short/long description, developer contact/support, category, country/language, distribution details.
- [ ] PLAY-06 Build final 512x512 app icon, 1024x500 feature graphic and actual-device screenshots complying with Play asset requirements; no misleading feature claims.
- [ ] PLAY-07 Host valid public Privacy Policy and Account Deletion URL and link them inside app and Play Console.
- [ ] PLAY-08 Fill Data Safety declarations (collected/shared data, security, deletion), app access demo login, content rating, audience/age and ads declaration as applicable.
- [ ] PLAY-09 Confirm user-generated-content terms acceptance, report/block/moderation workflow; content policy is actually enforced.
- [ ] PLAY-10 Check Play Pre-launch report, policy warnings and app compatibility; address rejection messages before trying production.
- [ ] PLAY-11 Record owner approval for release to actual users; submit production only when eligible, with Google review completion treated as external dependency.
- [ ] PLAY-12 If public rollout can't happen today, complete verified release candidate plus internal/closed testing instead; give transparent gating reason and next action.

## Phase 11 — Post-launch operations (P1)

- [ ] OPS-01 Define free-tier thresholds/alerts at 70%, 85%, 95% for DB, storage, egress, edge invocations and Realtime. No automatic paid upgrade.
- [ ] OPS-02 Monitor failed notify jobs, erasure/retention queues, media cleanup cron, edge errors and auth sign-in failures without exposing private content.
- [ ] OPS-03 Establish crashes/ANRs and performance instrumentation with opt-in/privacy-aware configuration and free-tier checks.
- [ ] OPS-04 Keep secure off-repository database backup/restore rehearsal and owner-controlled release signing backup.
- [ ] OPS-05 Add support/bug-report workflow, severity SLA and hotfix release process; document rollback for backend client schema compatibility.
- [ ] OPS-06 Establish abuse/content moderation ownership, reports review, retention policy and ability to handle escalations.
- [ ] OPS-07 Setup operational notes for paused Supabase Free projects, service interruptions and downgrade paths.
- [ ] OPS-08 Verify pricing/limits periodically and warn user before any nonfree action.

## Phase 12 — P2 upgrades AFTER v1.0 stabilization (no launch blockers)

- [ ] UP-01 User-selectable custom conversation wallpapers and curated glass palettes.
- [ ] UP-02 Biometric local chat lock and hiding sensitive content in recents/notifications.
- [ ] UP-03 Time-bound disappearing messages with explicitly documented limits.
- [ ] UP-04 Documents and more media file formats with per-file authorization/quotas and safe preview.
- [ ] UP-05 Message multi-select, batch share/copy/delete and protected forwarding.
- [ ] UP-06 Voice playback speeds, waveform, audio focus and resume usability.
- [ ] UP-07 Safe link previews with SSRF prevention, consent and network budget.
- [ ] UP-08 Personalized stickers/emoji reaction picker and refined animations.
- [ ] UP-09 Per-chat notification sounds/settings and richer unread management.
- [ ] UP-10 Optional export/restore and device migration designed around account privacy and secure storage.
- [ ] UP-11 Low-data auto-download settings and smart thumbnail/caching configuration.
- [ ] UP-12 Smart contact handling and folder customization without adding groups/calling.
- [ ] UP-13 Android predictive back navigation, refined haptics, app startup speed and accessible motion.
- [ ] UP-14 Optional localization (Bengali + English) with proper plural/time-zone handling.
- [ ] UP-15 Continuous visual regression screenshot tests, feature-flagged beta rollouts and performance thresholds.

## Required acceptance gates / STOP rules

**No production publication unless all are true:**

- [ ] GATE-01 Latest `main` is clean and all 3 Android CI jobs + backend checks pass; no open P0.
- [ ] GATE-02 Signed release AAB targets the current Play-required API level; install/upgrade path validated.
- [ ] GATE-03 Two real devices/accounts tested for core 1:1 messaging, FCM, offline/retry, voice/media, auth and account privacy; evidence recorded. If not possible, beta-only.
- [ ] GATE-04 Supabase RLS/server session/media rules tested; no credentials exposed; providers configured and tested.
- [ ] GATE-05 Privacy Policy, external deletion request path, Terms/UGC moderation, Data Safety and reviewer login ready.
- [ ] GATE-06 Play Console access/eligibility confirmed. If closed testing required, no claim of immediate public launch.
- [ ] GATE-07 Owner authorizes specific final release artifact/Play Console publish action.

## Required outputs from ChatGPT Work

1. **Concise baseline audit:** current commit, PR #2 outcome, latest CI, provider status, actual release blockers.
2. **Changed code committed:** phase-by-phase passing PRs merged to `main`, with commit SHAs and tests linked.
3. **Current risk ledger:** fix status and reproducible tests; no false success claims.
4. **Android artifacts:** validated debug APK and signed release AAB only when signing owner-controlled key exists; links, checksums and installation notes.
5. **Backend verification:** migration list, security/RLS results, Edge Function deployment checks; any owner-only configuration clearly marked.
6. **Play release pack:** privacy/account-deletion/terms content drafts, store listing copy, screenshot spec, Play policy checklist, app access/test account instructions (no real passwords in public docs).
7. **Final go/no-go report:** Ready for Internal Testing / Ready for Closed Testing / Ready for Production, with a separate status for each; missing items and exact owner tasks.
8. **Post-launch backlog:** P2 upgrades separated from launch-critical tasks.

## Current verified context (as of initial audit, re-check before work)

- `main` was v0.7.0 with passing Android debug Actions run 39020781262; it produced an APK artifact.
- Keyboard-focus fix was committed in PR #2 on branch `fix/composer-keep-keyboard-open`, **not yet verified/merged** at the last audit. Recheck CI and merge status.
- Supabase project `qsdmspylumxysczzqnqc` was `ACTIVE_HEALTHY`; 25 applied migrations and 4 active Edge Functions; Auth advisor warned that leaked password protection is disabled (plan constraints may apply), plus 3 informational unused-index notices.
- Cloudinary server credentials were not configured at last documented check. The protected Supabase private Storage fallback was implemented; limits and free-tier quotas apply.
- Existing automated tests covered SQL security, Android build/lint, selected Room/Compose/emulator flows; they did **not** certify real physical-device end-to-end production release.
- Important official requirements: new standard Play app submissions in October 2026 target Android 16 API 36+; AAB required for new apps; new personal developer accounts created after 13 Nov 2023 have 12 testers / 14 consecutive days closed-testing requirement; privacy and external account-deletion access; UGC terms, content reporting, and user blocking.

## Official references for Work to verify

- API target: https://support.google.com/googleplay/android-developer/answer/11926878?hl=en
- New-personal-account closed testing: https://support.google.com/googleplay/android-developer/answer/14151465?hl=en
- Android App Bundle: https://developer.android.com/guide/app-bundle
- User Data & Privacy: https://support.google.com/googleplay/android-developer/answer/10144311?hl=en
- Account deletion web requirement: https://support.google.com/googleplay/android-developer/answer/13327111?hl=en
- User-generated-content moderation: https://support.google.com/googleplay/android-developer/answer/9876937?hl=en
- Play app listing assets: https://support.google.com/googleplay/android-developer/answer/9866151?hl=en

**Execution directive:** Begin at Phase 0. Complete the highest-priority feasible tasks within this session, with actual code/test/commit artifacts, instead of merely drafting another PRD. Stop at any owner/provider/Google external gate and report it accurately. Never fabricate passing tests, live notifications, successful Play approval or release links.

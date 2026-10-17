# Implementation progress

## Step 1 — Android foundation
- Native Compose project, Android 10 minimum, SDK 35 development target.
- Shared shapes, readable light/dark palette, translucent tinted fallback.
- Chats / Contacts / Settings navigation; no calling or group UI.
- GitHub Actions tests + lint + debug APK artifact.
- SDK target must be reviewed before Play Store release.
- CI run 38945736138: lint + assembleDebug passed; unit test task had no tests in step 1. APK artifact generated.

## Next steps
2. Supabase auth, profile, schema + RLS + adversarial authorization tests.
3. Room history + outbox, idempotent sends, receipts, pagination, filters and local search.
4. Presence, typing, FCM durable dispatcher, protected Cloudinary image flow and user controls.
5. Accessibility and device/network acceptance checks; P1 only after P0 passes.

No milestone is release-certified until the PRD device gates have passed.

## Step 2 — Auth, profiles and secure data foundation
- Email registration/login/reset and Google OAuth PKCE; tokens encrypted with Android Keystore.
- Session refresh serialized; loading/error states; no forced logout on transient network failure.
- Unique usernames, exact discovery RPC, deterministic pair uniqueness, participant RLS, controlled message insertion, both-way blocks, deletion watermark.
- Live authorization.sql passed with rolled-back fixture users; no test accounts retained.
- Google provider credentials and `liquidchat://auth/callback` redirect allow-list must be configured in Supabase dashboard before OAuth/device reset testing.
- Real-user email delivery remains a release gate. No email provider or paid service was enabled.

## Step 3 — Cached text messaging
- Room is the UI source for messages and chat rows; immutable IDs and incremental upserts.
- WorkManager durable outbox: bounded backoff, failed/manual retry, stable ID across retries, account-scoped cleanup.
- Separate server sync cursor (never infer catch-up from the most recent locally accepted outgoing message).
- RLS-authorized Realtime hints, authoritative catch-up, cursor pagination, conservative delivery/read acknowledgments.
- Exact username discovery, All/Favorite/Archive/Unread filters, downloaded-message search with stable sender/conversation IDs.
- Server-persisted delete-for-me, archive/mute/favorite and block controls.
- Authorization regression SQL passed again after owner-only preference policies.
- Device/process-death/performance tests remain unmeasured; these are implemented behaviors, not release certification.

## Step 4a — Presence, privacy and protected images
- Private presence/typing RPCs; immediate background offline attempt, 30-second stale expiry; typing throttled and expired in bubble area.
- Privacy preferences persist server-side. Hidden heartbeat metadata is inaccessible directly.
- Server-proxied Cloudinary authenticated images; bounded size and decoded format checks; participant authorization and 60-second download URLs.
- Durable local image queue, caption preview, retry and aspect-ratio reservation; profile photo edit/remove.
- Media Edge Function deployed, but intentionally returns 503 until Cloudinary secrets are configured. No public-media workaround.
- Authorization regression tests passed for hidden presence and nonparticipant image denial. Real Cloudinary access/expiry tests remain blocked on secrets.

## Step 4b — Trusted push and account lifecycle
- FCM data-message receiver, exact foreground-conversation suppression, permission handling and stable notification IDs.
- Durable Postgres notification outbox, leases, bounded retry, mute/block checks, invalid-token removal, immediate authenticated dispatch plus a Vault-authenticated five-minute cron.
- Account erasure requires a session created in the last five minutes. Access is disabled immediately; owned images and Auth identity are removed through tracked jobs. Recipient text copies retain an anonymized sender.
- Restricted report queue; reports include only explanation unless explicitly selected IDs are passed.
- Revoked Auth sessions are rejected in both RPCs and table RLS. No service credentials enter the APK.
- System/light/dark appearance, reduced effects, motion options, 200%-text-friendly filters, image dimensions reserved before download.
- Abandoned upload cleanup is scheduled every 30 minutes only when work exists.
- Database authorization tests passed for erasure, recent reauthentication, report isolation and dispatcher denial.
- Cloudinary/FCM runtime tests remain blocked on owner-provided secrets/configuration; Google/email require dashboard setup. See BACKEND_SETUP.md.


## Step 5 — Hardening and reviewable beta
- Completed saved contacts, contact details, downloaded shared-media entry and full-screen image viewer.
- Queued messages update the cached chat preview immediately; snapshots retain pending previews.
- Added Room v1→v2→v3 migrations, account isolation, outbox ID/failure, sync-cursor gap, deletion watermark, literal-search and Keystore instrumentation tests.
- Fixed a lint failure caused by reading StateFlow.value during composition; refreshes now observe lifecycle state.
- Fixed heartbeat replies causing redundant history refreshes and serialized device registration with logout to prevent token reattachment races.
- SQL tests passed after contacts and avatar-access corrections; security advisor returned zero findings. Four live Edge endpoints rejected anonymous requests with HTTP 401.
- CI run 38954417697 passed: Android build/lint, 3 JVM tests, 7 Android 10 emulator tests and all 4 Edge Function type checks. The verified generated Deno dependency lockfile is committed. Physical-device performance/acceptance and external credential setup remain release blockers.


## Verified beta artifact
- Source commit: `80f52fb4d1da5383c93ee267439e9db66df71aa1`.
- Validation run: https://github.com/officialpritam007/Project-x/actions/runs/38954417697
- APK: version 0.3.0 (3), debug, Android 10+.
- APK SHA-256: `eb14093eb7ffcfb741e26596cc96edeba3557d54bc1d5e2145ca8e475f8e49f3`.
- No SQL fixture users/jobs were retained. SQL authorization suite and anonymous HTTP denial checks passed; latest security advisor had zero findings. One INFO about a fresh unused index is not a measured performance failure.
- This is a reviewable beta baseline, not a public-release certification. Provider configuration, real media expiry/push/auth journeys, physical-device performance and backup restore rehearsal remain required.


## Step 6 — Figma UI and remaining features (0.5.0)
- Implemented the five-screen Liquid Chat design with original navigation assets and source-backed Haze blur; Android 10/11 use the readable scrim fallback.
- Completed private Stories (24-hour expiry), Posts (archive/restore/delete), and durable AAC voice messages with recording cancellation/lock and seekable playback. Room v4 preserves older data.
- Deployed five additive migrations and media/account/cleanup updates. Missing Cloudinary credentials no longer block uploads: permission-checked private Supabase Storage is the bounded fallback. No paid service was enabled.
- Rollback authorization and moments/voice suites, real signed-in image/story/voice/block/account-erasure HTTP checks, media parser tests, Android build/unit/lint, and ten API 35 emulator instrumentation tests passed. All live test fixtures were removed.
- Reviewed screenshots and corrected compact-screen bottom insets and scrollable Profile posts. See REMAINING_FEATURES.md for scope and validation limits.
- Earlier Cloudinary-only runtime blockers above are superseded by the private fallback. Google OAuth/email delivery/FCM provider setup and physical-phone acceptance remain external release requirements, not completed device certification.

## Modern messaging — 0.7.0 (7)
- Upgraded the conversation, durable drafts, inbox filters/pins, reply/reactions/edit/star/delete, private mutation catch-up, notification authorization, wallpapers, camera/gallery, zoom, shared media and Data and Storage.
- Four additive server migrations deployed. All three rollback-only SQL suites pass; no fixture users remain.
- Actions run 22 passed build/unit/lint, Edge checks and 13 API 35 emulator tests, including Room v1→v7 and 1,000-message viewport/batch-follow coverage. Screenshots reviewed.
- APK SHA-256: `b23d2fb5c8685fecb0555b2a5687aef568c5b8470ddc01c9d51945aa188df7cc`.
- Full scope, migration inventory, artifact link, requirement mapping, bug ledger and physical/provider/P2 gates: [MODERN_UPGRADE.md](MODERN_UPGRADE.md). This remains a debug beta, not physical-device public-release certification.

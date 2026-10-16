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

# Stories, Posts, voice and live glass — 0.5.0

- Stories: publish compressed photos and optional captions; server-enforced 24-hour expiry; remove expired tiles while open; owner delete; pagination; scheduled Cloudinary cleanup every 30 minutes.
- Posts: publish, browse, archive, restore and delete; private audience is the author’s saved contacts. Removing a contact or blocking in either direction denies future feeds and signed URL requests. Existing signed image links expire within 60 seconds.
- Voice: microphone permission, foreground-only AAC/M4A capture (64 kbps, 120 s / 2 MB maximum), hold to record, release to send, swipe left to cancel, swipe up to lock, accessible hands-free action, discard, single-player playback, pause/resume and seek. URLs last 180 seconds to cover a full recording; playback and capture are released on background/navigation/account change. Room v4 preserves voice metadata and local files in the existing durable retry queue.
- Glass: pinned Haze 1.5.4; source-backed 30 dp blur on Android 12+, readable scrim fallback on 10/11, opaque reduced-transparency option. This is backdrop blur with reflective layers, not optical lens refraction.

## Backend and free-tier controls

Two additive migrations and backward-compatible media/account/cleanup functions are deployed. Media stays authenticated in Cloudinary. Client code contains no service-role or Cloudinary secret. The database validates owner, contact audience, session, block, media type, expiry, idempotency and archiving before granting media access. Per-account finalized media is capped at 100 MB, publishing at 100 active/archived moments, and uploads/publishing are rate-limited. No paid plan or billing change was made. Free quotas still apply; these controls do not promise unlimited usage.

`supabase/tests/moments_voice.sql` and the existing `authorization.sql` are rollback-only fixture tests, both verified on the deployed schema. Android CI includes Room v1→v4 migration, cache isolation, real AAC encoder/cancel checks, five-screen rendering, navigation/search checks and before/after live-backdrop screenshots on an API 35 emulator. Physical-phone microphone quality, OEM lifecycle behaviour and real-user visual approval cannot be substituted by emulator checks.

The pre-existing Supabase Auth warning about leaked-password protection remains; no paid Auth upgrade is enabled. Reference: https://supabase.com/docs/guides/auth/password-security#password-strength-and-leaked-password-protection

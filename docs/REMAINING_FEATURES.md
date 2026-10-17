# Stories, Posts, voice and live glass — 0.5.0

- Stories: publish compressed photos and optional captions; server-enforced 24-hour expiry; remove expired tiles while open; owner delete; pagination; scheduled media cleanup every 30 minutes.
- Posts: publish, browse, archive, restore and delete; private audience is the author’s saved contacts. Removing a contact or blocking in either direction denies future feeds and signed URL requests. Existing signed image links expire within 60 seconds.
- Voice: microphone permission, foreground-only AAC/M4A capture (64 kbps, 120 s / 2 MB maximum), hold to record, release to send, swipe left to cancel, swipe up to lock, accessible hands-free action, discard, single-player playback, pause/resume and seek. URLs last 180 seconds to cover a full recording; playback and capture are released on background/navigation/account change. Room v4 preserves voice metadata and local files in the existing durable retry queue.
- Glass: pinned Haze 1.5.4; source-backed 30 dp blur on Android 12+, readable scrim fallback on 10/11, opaque reduced-transparency option. This is backdrop blur with reflective layers, not optical lens refraction.

## Backend and free-tier controls

Five additive migrations and backward-compatible media/account/cleanup functions are deployed. Media uses authenticated Cloudinary when its server credentials are configured. A signed-in HTTP smoke test found those credentials absent in this project (the old endpoint returned 503), so new JPEG/AAC uploads now automatically use the private Supabase `liquid-media` bucket instead. Restrictive Storage policies deny all direct client reads/writes; Edge Functions issue signed URLs only after the same session/participant/contact checks. Existing Cloudinary records retain their provider. Pending object formats are stored before upload so interrupted uploads are also cleanable. Client code contains no service-role or Cloudinary secret. The database validates owner, contact audience, session, block, media type, expiry, idempotency and archiving before granting media access. Per-account finalized media is capped at 100 MB, the fallback has a serialized global 512 MB budget, publishing at 100 active/archived moments, and uploads/publishing are rate-limited. No paid plan or billing change was made. Free quotas still apply; these controls do not promise unlimited usage.

`supabase/tests/moments_voice.sql` and the existing `authorization.sql` are rollback-only fixture tests, both verified on the deployed schema. Android CI includes Room v1→v4 migration, cache isolation, real AAC encoder/cancel checks, five-screen rendering, navigation/search checks and before/after live-backdrop screenshots on an API 35 emulator. Physical-phone microphone quality, OEM lifecycle behaviour and real-user visual approval cannot be substituted by emulator checks.

The pre-existing Supabase Auth warning about leaked-password protection remains; no paid Auth upgrade is enabled. Reference: https://supabase.com/docs/guides/auth/password-security#password-strength-and-leaked-password-protection

## Verified integration and visual checks

Authenticated HTTP checks passed against the deployed backend: private JPEG upload, story publishing/feed, signed image download, AAC upload/finalization, idempotent voice sending, signed participant audio download, and both media types denied after blocking. Account erasure removed fixture files and identities; all test accounts and storage objects were confirmed removed. Valid AAC/JPEG and malformed media parser checks also passed.

API 35 emulator screenshots were reviewed for all five screens, message bubbles and changing live blur. A compact-screen Profile issue was corrected by scrolling its header and posts together, with an instrumentation assertion that the Add a post action remains reachable by scrolling.

Final validation: [Actions run 17](https://github.com/officialpritam007/Project-x/actions/runs/39008275099), runtime commit `325797365576a4ec4b26c89dde0730a3b5a5987d`, all three CI jobs passed and all ten instrumentation tests passed (zero failures/errors). The APK is debug 0.5.0 (5), Android 10+. APK SHA-256: `7ca12ec4a4c4c8a2e4db0cf46ce3cfb7f8eae4a0c26112b0b056ace8472bba18`.

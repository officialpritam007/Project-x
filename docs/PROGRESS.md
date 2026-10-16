# Implementation progress

## Step 1 — Android foundation
- Native Compose project, Android 10 minimum, SDK 35 development target.
- Shared shapes, readable light/dark palette, translucent tinted fallback.
- Chats / Contacts / Settings navigation; no calling or group UI.
- GitHub Actions tests + lint + debug APK artifact.
- SDK target must be reviewed before Play Store release.

## Next steps
2. Supabase auth, profile, schema + RLS + adversarial authorization tests.
3. Room history + outbox, idempotent sends, receipts, pagination, filters and local search.
4. Presence, typing, FCM durable dispatcher, protected Cloudinary image flow and user controls.
5. Accessibility and device/network acceptance checks; P1 only after P0 passes.

No milestone is release-certified until the PRD device gates have passed.

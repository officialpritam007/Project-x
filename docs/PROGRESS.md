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

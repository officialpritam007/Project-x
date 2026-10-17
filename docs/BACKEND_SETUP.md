# Backend setup and remaining release gates

Supabase project: `qsdmspylumxysczzqnqc` (existing Free organization).
The migrations and four Edge Functions have been deployed. The repo includes their source. No paid upgrade, paid branch, or add-on was enabled. Cloudinary reported Free, 0.2 of 25 credits used during the initial check.

## 1. Supabase Auth
In [Auth URL configuration](https://supabase.com/dashboard/project/qsdmspylumxysczzqnqc/auth/url-configuration), allow `liquidchat://auth/callback` as an additional redirect URL. This callback is protected by PKCE and a verifier encrypted on the initiating device.

Enable Google in [Auth providers](https://supabase.com/dashboard/project/qsdmspylumxysczzqnqc/auth/providers). Configure your Google OAuth client with the Supabase callback shown there. Test account creation and reauthentication on two phones. Existing Firebase sessions/passwords are not automatically migrated; Project-x starts with Supabase identities.

Configure a separately validated SMTP service for public registration/reset email. Confirm its free limits, sender domain, link redirects and delivery. The bundled development email service is not public-launch acceptance. No SMS authentication is used.

## 2. Optional Cloudinary secrets — server only
Media currently works through the private Supabase Storage fallback because the Cloudinary credentials are absent. Direct client bucket access is denied; Edge Functions authorize and sign private URLs. The fallback has a serialized 512 MB global budget and 100 MB finalized-media limit per account. To prefer Cloudinary for new uploads, configure these values in [Edge Function secrets](https://supabase.com/dashboard/project/qsdmspylumxysczzqnqc/functions/secrets):

- `CLOUDINARY_CLOUD_NAME`
- `CLOUDINARY_API_KEY`
- `CLOUDINARY_API_SECRET`

Do not put the API secret in GitHub, an APK, an unsigned upload preset or a chat message. The media function performs signed `authenticated` uploads itself. No unsigned preset is required.

Verify: upload an image between accounts A/B; C cannot obtain its download URL. A copied temporary URL must stop working after 60 seconds. Verify blocked and deleted-history access denial, cancellation, retry and abandoned upload cleanup. Cloudinary account restrictions can prevent private download API calls: verify the real account configuration before release. No fallback to public URLs is allowed.

## 3. FCM
Register Android app `com.pritam.liquidchat` in the Firebase project used only for FCM. Add these **GitHub Actions repository variables** from its Android public config:

| Variable | google-services.json field |
|---|---|
| `FCM_APP_ID` | `client[...].client_info.mobilesdk_app_id` |
| `FCM_PROJECT_ID` | `project_info.project_id` |
| `FCM_API_KEY` | matching client's `api_key[0].current_key` |
| `FCM_SENDER_ID` | `project_info.project_number` |

In Supabase Edge Function secrets, set `FCM_SERVICE_ACCOUNT_JSON` to the service account JSON for that same Firebase project with permission to send FCM HTTP v1 messages. Never commit that JSON. Build a new APK after setting the four public variables. Without them, the text app runs while push initialization stays disabled.

The `notify` function verifies app JWTs with Auth.getUser; cron calls use a generated Vault secret. Gateway JWT verification is disabled only because the function performs these explicit checks itself. Service credentials are environment-provided inside Edge Functions.

Verify foreground exact-chat suppression, other-chat notifications, background, process termination, muted contacts, preview-off, notification denial, logout, account switch and two-device delivery acknowledgments. FCM submission never advances delivery/read states. OS restrictions can delay background WorkManager processing.

## 4. Operations
- `liquid-notification-dispatch`: every five minutes, only while a pending/leased job exists. Send workers request immediate dispatch.
- `liquid-account-cleanup`: every five minutes, only while an erasure job remains pending.
- `liquid-media-cleanup`: every 30 minutes, only while abandoned unpublished uploads older than one hour exist.
- Inspect failed notification rows and pending deletion jobs as the database owner; never expose these queues to clients.
- Review reports in `private.reports` as the owner. A moderation owner/workflow and 90-day resolved-report retention need operational sign-off.
- Monitor dashboard database size, egress, Realtime and functions at 70/85/95% of Free allowances. No automatic paid upgrade or history deletion exists. Traffic limits are not a guarantee of unlimited free usage.
- Free project pausing is downtime: resume from the dashboard; clients retain cached history/outbox. Do not promise continuous service.
- Arrange access-controlled exports and rehearse restores. No backup or restore has been performed in this implementation session; do not put chat exports in this public repo.

## 5. Release gate — still required
CI verifies compilation/lint, unit tests and ten API 35 emulator checks. The SQL suite verifies the listed database authorization scenarios. Live authenticated HTTP checks also passed for private images, Stories, voice, blocking and media-bearing account erasure, with fixtures removed. They do not prove full product acceptance.

Before release, complete two physical-device journeys, offline/process-death/outbox/Room migration checks, Cloudinary-specific expiry if that optional provider is enabled, FCM delivery, real SMTP/Google flows, account erasure with owned media, TalkBack, 200% text, gesture/keyboard layouts and low-memory frame profiling. Confirm the remaining PRD policy defaults and current Play target SDK requirements. Stories, Posts and voice are implemented in the reviewable beta; this does not replace physical-device release acceptance.

The implemented contact-details/shared-media dialog and full-screen viewer operate on downloaded images. Saved contacts are stored independently of conversations. Remaining hardening work requires device-tested loading/permission states and measured quota degradation behavior. The current glass treatment uses source-backed live blur on Android 12+ and a readable scrim on Android 10/11.


## Debug APK updates
CI caches its generated development signing key. For durable signing across cache eviction, supply an owner-controlled `DEBUG_KEYSTORE_BASE64` GitHub Actions secret using the Android debug alias/password convention. A provided private key is never included in the public cache. Never use a production release key for debug APKs. Early APKs built before this cache setup may require uninstalling before an update; export/send pending messages first. Production signing and Play distribution are separate release work.

The project is bound to the existing project URL/public key. To deploy elsewhere, update `Backend.kt` and project URLs/keys in cron migrations explicitly. Do not rerun already-applied migrations against the current database.

# Liquid Chat — voice/video calling beta

## Current status (18 October 2026)

- Calling is **experimental**, 1:1 only; no calls or video data are recorded/stored by the backend.
- Supabase call signaling migration **20261018045813** and one-shot call push migration **20261018045819** are **deployed** to the connected Supabase Free project. Their existing data is preserved.
- The optional **call-notify** Edge Function has been deployed and reports ACTIVE, version 1. It uses self-checked Firebase Auth JWTs and a `service_role`-only call claim.
- `supabase/tests/calls_authorization.sql` passed as a rollback-only transaction against the deployed schema. No synthetic test users/calls remain.
- Calling UI remains **disabled by default**. It is not production-ready until owner credentials and physical-device validation pass.
- All 1:1 chats, messages, media, moments, profile and history features remain unchanged.

## Build an explicitly opt-in beta APK

Once the stabilization pull request is merged into `main`, open **GitHub Actions → Android candidate checks → Run workflow** and select **Enable experimental calls** = **true**. This enables calling **only for that manual test build**. Ordinary PR/main builds and the **signed release** deliberately force `ENABLE_EXPERIMENTAL_CALLS=false`. Avoid setting a repository-wide flag.

Download the `app-debug.apk` artifact only from a completed, green workflow for the exact commit. Developer/debug signing is not Play signing. After a successful build, use two **different real user accounts on two different phones**.

## Owner-only Firebase setup (do not paste credentials in issues or chat)

Firebase Cloud Messaging is free, but configured Android identifiers and server credentials are necessary.

1. GitHub → Project-x → Settings → Secrets and variables → Actions → **Variables**: set `FCM_APP_ID`, `FCM_PROJECT_ID`, `FCM_API_KEY`, `FCM_SENDER_ID` for the same Android Firebase application/package. These are client configuration, not a private service key.
2. Supabase project → Edge Functions → Secrets: set `FCM_SERVICE_ACCOUNT_JSON` to the Firebase **service-account JSON**; never put this into Android, GitHub files, a PR, or a message. Ensure its `project_id` matches Android Firebase configuration.
3. Install the test APK on both phones; allow notifications, microphone and camera permissions when prompted. Verify FCM registration and data-only incoming-call push with app foreground/background.
4. The receiver treats incoming FCM data as **untrusted**. It queries `call_poll` with the signed-in user token before displaying a short-lived call notification; notification taps re-check the same call before offering Answer/Decline.

If FCM secret is not configured, the optional wakeup endpoint returns 503, while foreground signaling calls may still work. Do not claim background/locked-screen delivery until verified.

## Networking and zero-charge operation

Native WebRTC media uses DTLS/SRTP with Cloudflare **STUN-only** (`stun.cloudflare.com`). Carrier-grade NAT/restrictive firewalls may block direct connections. **TURN has not been configured or paid service provisioned.** Cloudflare TURN has a shared no-cost allowance but may be usage-billed above that and require a billing account. Zero unexpected charges takes priority: never enable TURN until the owner explicitly approves a strict quota/billing strategy.

The free plan still has rate/egress quotas. Experimental calls use foreground polling, so limit beta testers and monitor Supabase usage.

## Security and device acceptance

- Database-side caller/callee/block checks and RLS; call SDP/ICE is retained temporarily (can include IP information). Scheduled cleanup removes expired call records/candidates after a grace interval.
- Service-only, atomic push claim blocks duplicates; only caller-authenticated Edge requests can trigger the server claim.
- Media stops immediately when the app leaves the foreground. There is **no** Telecom integration, persistent foreground calling service, Android full-screen call permission or group calling.
- App kill, power saver, Android 14–16 background restrictions, notification permission denial, mobile data, Wi-Fi/NAT, Bluetooth, speaker/camera, mic mute, second incoming call, decline, block, account switch, user logout and real reconnect must all be checked manually.
- CI, emulator tests and server-side SQL tests **cannot verify microphones, speakers, video quality, echo or device-to-device notification delivery.**
- Do not publish until the beta acceptance checklist passes and an owner-signed release, privacy/disclosure review and usage-cost plan are completed.

## GitHub CI and migration synchronization

- Original migration file `20261018000000_one_to_one_call_signaling.sql` was renamed to `20261018045813_one_to_one_call_signaling.sql` to match the **actual** applied Supabase migration version.
- Next migration is `20261018045819_call_push_claim.sql`, also matching deployed history.
- Future migrations must be additive and never replay or overwrite previous production data.

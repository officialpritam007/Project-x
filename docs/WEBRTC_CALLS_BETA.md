# Liquid Chat — voice/video calling beta

## Current status (19 October 2026)

> This document covers the experimental calling beta. PR #13 introduces
> user-started microphone/camera foreground service support, an ongoing-call
> notification and Android background media continuity. **It does not** add
> full system Telecom integration, guaranteed background delivery, or TURN.
> See [CALL_PRODUCTION_RELEASE_GATES.md](CALL_PRODUCTION_RELEASE_GATES.md)
> for the actual deployment and two-phone acceptance gates.

- Calling is **experimental**, 1:1 only; no calls or video data are recorded/stored by the backend.
- Supabase call signaling migration **20261018045813** and one-shot call push migration **20261018045819** and call foreign-key indexes migration **20261018050349** are **deployed** to the connected Supabase Free project. Their existing data is preserved.
- The **call-notify** Edge Function is deployed and reports ACTIVE, version 4 at the latest read-only review. It validates Supabase user JWTs and uses a `service_role`-only call claim. FCM delivery still needs real-device acceptance.
- `supabase/tests/calls_authorization.sql` passed as a rollback-only transaction against the deployed schema. No synthetic test users/calls remain.
- Calling UI is **enabled in Android candidate CI APKs** and verified in the installed candidate by instrumentation. Owner-signed production releases retain their separate disabled-call gate until provider/device/release requirements pass.
- All 1:1 chats, messages, media, moments, profile and history features remain unchanged.

## Build the calling-enabled test candidate

`.github/workflows/android.yml` sets `ENABLE_EXPERIMENTAL_CALLS=true` for candidate PRs, main pushes and manual candidate runs. No extra opt-in input is needed. The separate owner-signed production workflow remains gated. Development uses automated checks and verified merges; the owner receives one final integrated APK and one physical test round, not an installation request after each PR.

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
- Accepted/started call media may continue in the background through a user-started microphone/camera foreground service. Recents/task removal or service termination ends capture. Full-screen invitation access is subject to Android permission/OEM restrictions; it is not guaranteed. Full Telecom integration, process-death call restore and group calling are not implemented.
- App kill, power saver, Android 14–16 background restrictions, notification permission denial, mobile data, Wi-Fi/NAT, Bluetooth, speaker/camera, mic mute, second incoming call, decline, block, account switch, user logout and real reconnect must all be checked manually.
- CI, emulator tests and server-side SQL tests **cannot verify microphones, speakers, video quality, echo or device-to-device notification delivery.**
- Do not publish until the beta acceptance checklist passes and an owner-signed release, privacy/disclosure review and usage-cost plan are completed.

## GitHub CI and migration synchronization

- Original migration file `20261018000000_one_to_one_call_signaling.sql` was renamed to `20261018045813_one_to_one_call_signaling.sql` to match the **actual** applied Supabase migration version.
- Next migration is `20261018045819_call_push_claim.sql`, also matching deployed history.
- Foreign-key indexes are tracked in \`20261018050349_call_foreign_key_indexes.sql\`; performance advisor reports no remaining unindexed call foreign keys.\n- Future migrations must be additive and never replay or overwrite previous production data.

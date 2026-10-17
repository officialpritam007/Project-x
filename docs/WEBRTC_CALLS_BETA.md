# Liquid Chat — 1:1 WebRTC calls (draft beta)

## Status / rollback boundary

Implementation branch: feature/one-to-one-webrtc-calls. This is **NOT a production release**.
No existing chat/moment/storage migration is rewritten. Do not merge or enable it until CI, RLS
fixture tests and two physical phones with different user accounts pass. The new migration
is committed only; its presence in GitHub does **not** deploy it to Supabase.

By default, ENABLE_EXPERIMENTAL_CALLS=false. This intentionally disables the new buttons,
incoming-call polling, and media capture. To test after the backend schema is deployed,
set GitHub Actions variable ENABLE_EXPERIMENTAL_CALLS=true for the test build and rebuild.
Keep the flag false for normal user distribution until calls are proven.

## Architecture

- Android 10+ Kotlin Compose: CallOverlay and CallController separate from the messages UI.
- Native WebRTC (Maven Central io.github.webrtc-sdk:android:150.7871.01) uses DTLS/SRTP
  transport and actual local mic/camera tracks.
- Supabase: private membership/block-checked public RPCs; no audio/video flows into the DB.
  RLS permits participant-only SELECT on metadata and ICE. SQL RPCs handle writes.
- Signaling is retrieved by short foreground-only polling; **not** background incoming calls.
- Network uses public Cloudflare STUN only. No TURN key, server credential, paid plan,
  SMS, video server, Cloudinary integration or extra database is introduced.
- A call lasts only while the Activity is foreground. Leaving the app ends the call.
  There is no Android Telecom/ConnectionService, persistent notification,
  Bluetooth audio route selector, ringing when killed/locked, or call history yet.
- No end-to-end cryptography claim beyond WebRTC transport encryption. SDP and ICE can
  include participants' IP/network identifiers; retention removes expired sessions
  and ICE via a 15-minute pg_cron job after a 1-hour grace period.

## Required staging steps

1. Back up and inspect the existing Supabase schema. Review the additive migration
   supabase/migrations/20261018000000_one_to_one_call_signaling.sql against currently
   deployed migrations. Test it **first** in a disposable/staging database.
2. Run supabase/tests/calls_authorization.sql under the DB owner in that staging schema;
   its effects roll back. Check anon, outsider, participants, blocked pairs, direct
   table-write denial, candidate privacy, declined and ended call behavior.
3. Re-run original authorization.sql, message_actions.sql, moments_voice.sql and
   content_reporting.sql; inspect Supabase Security/Performance advisors.
4. Only after explicit acceptance deploy the additive migration via the normal
   migration process. No previous migration edits and no destructive reset.
5. Enable the experimental flag for a controlled beta build. CI must pass on exactly
   that SHA, including Android API29/35/36 and minified release.
6. Install the matching debug APK on two devices with different verified users and
   open the same conversation on both. Accept mic/camera runtime permission.
7. Test audio answer/decline/hangup, video camera/mute, speaker, screen rotation,
   no echo, network reconnect, app background/return, block, account switch/logout,
   RLS unauthorized RPC probes, media/data usage, and multiple device OEMs.
8. Only then consider FCM incoming-call signaling and Telecom integration. Background
   microphone/camera foreground-service rules and Android 14–16 restrictions require
   explicit implementation/tests. Do not add FGS permissions blindly.

## Blocking limitations / next milestones

- **TURN required for reliable cellular/restrictive NAT connectivity.** Cloudflare TURN
  may offer a no-cost allowance but may require account/billing setup; never enable
  pay-as-you-go without the owner's informed permission. TURN keys remain server-only;
  use ephemeral credentials via an authenticated Edge endpoint with per-user quotas.
- Current polling is intended for a few beta testers, not large-scale production.
  Replace with authorized Realtime notifications + reconciling RPC polls and ensure
  accurate missed-call behavior.
- FCM config/credentials and true foreground/background incoming notification delivery
  must pass a two-device test; do not claim feature ready based solely on Android CI.
- Review expiry behavior, ICE retry, answering two concurrent devices, Bluetooth/headset
  routing, audio focus, Wi-Fi-to-cell handoff, rate limits, privacy retention and
  accessibility before public rollout.
- Only merge after CI and production security/QA owner approval.

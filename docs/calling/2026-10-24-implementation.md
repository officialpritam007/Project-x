# Calling protocol and Android lifecycle update

Base: main `8f2ddb0a693f87997feaa988d3dc403a2a5f1827`.
Production calling release gate is unchanged. No backend deployment, provider provisioning,
billing activation or physical-device acceptance is implied by this commit.

## Implemented scope

1. Prefer connected call-capable Bluetooth, then wired, then video/voice built-in fallback.
   Preserve successful manual route identity until it disappears. Audio-device and communication-route
   callbacks update UI; Android 29/30 uses SCO. Bluetooth/phone-state permissions are optional,
   requested while visible; denied Bluetooth permission leaves platform routing/fallback available.
2. Video uses composited TextureViews, with identity-checked sink replacement/disposal for both peers.
   Native surface windows no longer obscure peer video after swapping the self preview.
3. Outgoing starts Calling. Only authenticated callee presentation acknowledgement permits Ringing.
   Notification acknowledgement requires an actual active notification, not FCM provider acceptance.
   Busy, decline, unanswered, connecting, reconnecting and ended states remain distinct.
4. Temporary audio-focus/cellular interruptions suspend microphone and actual capture and publish hold.
   Focus gain can resume only while visible with permissions and preserved user intent. Permanent loss
   and leaving video without PiP require explicit Resume. Hangup invalidates all callbacks.
5. Connected-video controls time out after the Android-recommended control timeout (at least four
   seconds). Any pointer interaction resets the timer. Open routing menu, permission UI, errors,
   incoming/connecting/held states and enabled accessibility services prevent automatic hiding.
6. Only resumed active video keeps the Activity window awake; pause/end clears the flag. Voice
   proximity remains scoped to actual earpiece routing, including Android 29/30.
7. Optional authenticated Coturn REST credentials live for ten minutes. APK/source contains no shared
   TURN secret. Initial setup and ICE restart request fresh credentials; failure retains STUN fallback.
8. Default-network changes and ICE disconnection request caller-led ICE restart/offer-answer exchange.
   Caller alone offers, callee answers the matching revision. Three local recovery requests and the
   existing connection/reconnect watchdogs bound recovery. Generation/account/call guards remain.
9. Media and signaling availability are separate. Candidate delivery exhaustion reports signaling
   degradation rather than immediately ending healthy media. Polling retries up to forty seconds;
   authorization denial and server session expiry still terminate. The next successful poll reconciles
   remote hangup. Accepted sessions retain the existing 45-second expiry/heartbeat contract.
10. Camera off invokes stopCapture, with user camera intent independent of hold. Resume restarts the
    same capturer/track; native operations share the teardown gate. Switching is disabled while stopped.
11. Peer camera/hold metadata and recent frame receipt drive distinct placeholders. A five-second
    frame watchdog marks missing video unavailable. State is resent with heartbeats after outages;
    monotonically ordered media updates prevent late requests from overwriting new intent.
12. Only front-facing local video is mirrored; camera-switch callback updates its current renderer.
    Remote video always remains unmirrored.
13. Issue #143: all eligible tokens are visited with at most twelve concurrent sends (no device cap).
    A 25-second send deadline plus bounded database completion reports attempted/unattempted work within the 35-second lease. Failed tokens cannot prevent later
    token visits; next claims prioritize less-attempted devices. Delivered tokens are excluded from retries,
    stale tokens are removed, completion is calculated from current eligible DB tokens, and claim UUIDs
    reject delayed completion of newer leases. Three claims and invitation expiry remain bounded.
14. Supported devices enter Android system PiP for a connected video call via the existing user-started
    camera/microphone service. An immutable service End action is provided. PiP content is isolated from
    chat/navigation and policy/account checks remain required. A stopped/hidden video Activity is held, including hidden PiP when the screen locks. Existing
    in-app floating video and compact pill remain available.
15. Native texture outline clipping and Compose clipping use the self-preview's actual 22dp bounds,
    rather than merely painting a rounded border. Inset renderers have independent sink ownership.

## External rollout blockers

- Apply tracked migration `20261024182306_call_resilience.sql` to the intended Supabase project only
  after its existing authorization suite and this isolated protocol suite pass for that environment.
- Deploy updated `call-notify` and new `call-ice` Edge Functions with JWT gateway settings from
  `supabase/config.toml`. Functions validate the user themselves. This repository push is not deployment.
- Configure an owner-selected Coturn-compatible TURN endpoint (UDP/TCP/TLS as appropriate), its
  REST-auth shared secret in Edge Function secret `TURN_REST_SECRET`, and comma-separated
  `TURN_URLS`. Configure the same secret on Coturn. No provider has been purchased or enabled.
- Keep server-only `FCM_SERVICE_ACCOUNT_JSON` and the existing public Android FCM configuration
  complete. Confirm actual multi-device delivery; an HTTP 200 means provider acceptance only.
- Deploy protocol before distributing the updated calling beta. Existing conversations/messages,
  authentication, notifications, history retention and account isolation are not migrated or rewritten.

## Physical/provider acceptance — NOT VERIFIED

Use two independent accounts/phones (plus additional logged-in recipient devices for push). Test
Android 29/30 legacy SCO and Android 31+ communication devices; grant and deny optional permissions;
connect/disconnect Bluetooth before/during call; select speaker manually before reconnecting headphones.
Test cellular incoming/active/end, transient/permanent focus loss, permission revocation, background,
locked device, explicit hangup during interruption, muted/camera-off interruption and explicit resume.
Test repeated swap/drag/minimize/compact/expand, rotation, front/back camera, remote camera/hold,
first frame, missing frames and PiP entry/exit/close/End/return with actual camera video. Inspect clipping
pixels and verify camera privacy indicator goes out when capture is stopped. Confirm timeout restoration
and voice proximity with physical sensors. Test Wi-Fi/mobile handoff, airplane-mode interruption,
signaling-only outage with healthy peer media, peer hangup during outage, expired session and blocked
contact. Verify relay-only connectivity through configured TURN (including restrictive carrier NAT),
credential expiry/refresh, and FCM delivery to more than twelve devices with stale/failed tokens mixed in.

Automated results are recorded in the delivery message; none substitute for these tests.

## Reproducing automated checks

- `python3 -m unittest discover -s scripts/tests -p 'test_*.py' -q`
- `cd scripts/call-protocol && npm ci --ignore-scripts && npm test`
- From `supabase/functions`, run Deno 2.5.4 `check --frozen --lock=deno.lock`
  on `media/index.ts notify/index.ts account/index.ts media-cleanup/index.ts call-notify/index.ts call-ice/index.ts`.
- From the same directory, run `deno test --allow-env=WS_NO_BUFFER_UTIL,WS_NO_UTF_8_VALIDATE --frozen --lock=deno.lock media/inspect_test.ts account/admin_input_test.ts call-notify/dispatch_test.ts`.
- With JDK 17, Android SDK 36/build-tools 35.0.0 and Gradle 8.11.1:
  `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest testReleaseUnitTest lintRelease bundleRelease`.
  The release bundle is unsigned without owner signing credentials; this does not pass the production
  calling or owner-signing release gate. Instrumentation APK compilation is not an instrumentation run.

The isolated PostgreSQL/WASM fixture executes the tracked call migrations with test authentication
helpers. It covers protocol authorization and state transitions, not a deployed Supabase project's full
session infrastructure, FCM delivery, TURN connectivity, or physical Android behavior. CI now includes
this fixture and the push batching tests; the existing Android API 29/35/36 emulator jobs remain intact.

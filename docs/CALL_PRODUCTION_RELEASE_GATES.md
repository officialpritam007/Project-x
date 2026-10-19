# Liquid Chat — Calling release gate (1:1 only)

## Implemented and available in PR #13
- Native WebRTC DTLS/SRTP voice/video with Supabase-authenticated one-to-one signaling, block checks, expiring SDP/ICE, and rate limits.
- One-shot service-authorized FCM wakeup and server-validated incoming invitation with Android notification. Requires owner Firebase setup.
- User-initiated microphone/camera foreground service while **active** calls run, ongoing privacy-safe notification with an End action, and app-task removal teardown. Android never starts mic/camera service from FCM or background workers.
- Voice-call audio focus acquisition/release; on permanent focus loss end safely. Bounded pre-answer ICE candidates and video sink disposal.
- Existing messages, account erasure, Supabase Free plan, Cloudinary fallback, and 1:1 chat scope unchanged. No paid TURN/SFU/call provider.

## Release blockers (must be met before the calling flag is shipped)
1. **Exact commit CI**: Android debug/release lint, unit tests, artifact signature and 16 KB checks, emulator APIs 29/35/36, Edge type checks and Supabase auth test fixtures all green. Inspect GitHub Actions for the HEAD commit, never rely on green checks from previous commits.
2. **Firebase config**: set GitHub Actions variables FCM_APP_ID, FCM_PROJECT_ID, FCM_API_KEY, FCM_SENDER_ID for the Android package. Set FCM_SERVICE_ACCOUNT_JSON as a **Supabase project secret**, not GitHub source or a chat message. Verify with real FCM device tokens. Failure is expected before setup.
3. **Network reliability**: Cloudflare public STUN is free, but STUN-only does **not** guarantee connectivity through carrier-grade NATs/restricted Wi-Fi. A TURN relay with authentication, egress monitoring and a hard no-charge cap/owner approval must be in place and tested before claiming production call reliability. Do not silently enable billing or third-party relay services.
4. **Real device tests**: two distinct signed-in test accounts on two physical Android handsets (one API 29/30 if available, one API 35/36) across home Wi-Fi, separate mobile carriers and Wi-Fi↔mobile handoff. Test: dial, 45s timeout, accept, reject, hangup, double-call busy, blocked peers, mic mute, speaker, cameras, Bluetooth/headset, audio interruption, notification tap, phone locked, foreground→home/background→return, removal from Recents, logout, OEM battery saving, denied/revoked permissions, lost network and reconnection. Verify media stops when End pressed or service dies and no account-crossing leakage.
5. **Android Telecom/phone integration**: core Telecom APIs/remote headset actions and robust ongoing-call lifecycle must be reviewed and accepted; the current service alone is not system-dialer integration. No video-call recording or group calling.
6. **Production operations**: crash/ANR monitoring, documented quotas, privacy policy updated for microphone/camera/background use and ICE/IP metadata, user reporting, live migration/retention audit, owner signing keys, Play data-safety disclosure and account deletion URLs.
7. **Controlled rollout**: signed release currently forces ENABLE_EXPERIMENTAL_CALLS=false. Keep this fail-safe until every gate above is passed and the owner authorizes a new release build with calling enabled. No CI/build success replaces actual devices or consent.

## Test APK
The Android candidate workflow sets `ENABLE_EXPERIMENTAL_CALLS=true` for PR/main/manual candidate builds. Use only the final integrated artifact identified in #24 after all five checks pass; its installed calling flag is tested directly. The separate owner-signed release workflow remains gated. Download `Liquid-Chat-debug-...` as the test APK, not the unsigned AAB, and use `ALL_IN_ONE_TWO_PHONE_TEST.md` for one consolidated acceptance round. This is not a Play-ready production release.

## References
- Android microphone/camera FGS and Android 14+ restrictions: https://developer.android.com/develop/background-work/services/fgs/service-types
- Android Telecom VoIP integration: https://developer.android.com/develop/connectivity/telecom/voip-app/telecom
- FCM high-priority delivery behavior: https://firebase.google.com/docs/cloud-messaging/android-message-priority
- Cloudflare TURN pricing and credentials: https://developers.cloudflare.com/realtime/turn/faq/ and https://developers.cloudflare.com/realtime/turn/generate-credentials/

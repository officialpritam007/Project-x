# Liquid Chat P0 implementation and acceptance plan

Baseline: main `7880faabe4c26075b84fbf9e55384f6e7aa2afe8`, 18 October 2026.
Scope: one-to-one messaging and experimental voice/video calls. Preserve accounts,
messages, deployed migrations and the Liquid Glass UI. No new paid services.

## Batch 1: push recovery

Implemented in `fix/push-recovery-p0`; acceptance depends on exact-commit CI and devices.

- Retry transport errors and HTTP 408/429/5xx for call polling with 10-second linear
  backoff, at most three retries and a 45-second persisted local budget. Never
  authorize an invitation from FCM alone; revalidate server state and expiry.
- Coalesce concurrent call pushes per recipient/call. Use the remaining server
  lifetime for notification timeout. An already posted call notification uses
  `setOnlyAlertOnce`; this is not persistent deduplication after user dismissal.
- Use regular work on API 29/30; expedited work without foreground information
  is supported only from API 31. No microphone/camera service from incoming push.
- Use `notification_preview` as the notification authorization source after sync.
  Do not drop authorized pushes because Room's initial history page excludes the
  message or a sync/list-chats race left the local cache empty. Preserve the
  persistent message notification receipt and final account/active-chat checks.
- Add JVM tests for retry boundaries, exhausted/expired budgets, server-suppressed
  messages, empty-cache delivery, account/active-chat changes and cancellation.

## Remaining P0 work

| Work | Primary source | Acceptance evidence |
| --- | --- | --- |
| Build flag and install provenance | `.github/workflows/android.yml`, `app/build.gradle.kts` | Calling-enabled beta metadata; normal build still gated; update installation on both phones |
| Incoming call delivery | `data/Push.kt`, `calls/CallOverlay.kt` | Foreground, background, lock screen, denied notifications, OEM battery restrictions; stale invite never opens an active call |
| Server call-push dispatch recovery | `supabase/functions/call-notify/index.ts`, call-push claim migration | Fault-inject FCM failure after claim; bounded retry without repeated invites; separate reviewed migration if needed |
| Message notification regression | `data/MessageNotificationDedupe.kt`, `data/Push.kt` | Duplicate FCM before/after restart and notification dismissal; one alert per account/message; muted/blocked/deleted messages suppressed |
| Remote video black frames | `calls/CallEngine.kt`, `calls/CallOverlay.kt` | Actual remote frames both directions; camera switch, background/return and rotation; no leaked renderer |
| Call state races | `calls/CallController.kt`, call-signaling migration | Accept/reject/cancel/timeout crossing, simultaneous dial, duplicate ICE, logout/block; terminal states stay terminal |
| Audio routing | `calls/CallEngine.kt` | Earpiece/speaker/Bluetooth/headset, unplug/replug, mute, focus loss; two-way audio without echo regression |
| Network recovery | `calls/CallEngine.kt`, `calls/CallController.kt` | Wi-Fi/mobile handoff, ICE disconnected/failed, bounded reconnect or clear end reason; restricted-NAT limitation documented |
| Composer focus and cursor | `ui/ChatScreens.kt` | Gboard and Vivo keyboard remain open after send; delayed send does not erase newly typed text or move its cursor |
| Outbox idempotency/order | `data/ChatRepository.kt`, `data/Delivery.kt` | Offline send, process death, reconnect, duplicate response; one server message per ID, stable ordering and explicit failed retry |
| Delete consistency | `data/ChatRepository.kt`, `data/LocalDatabase.kt` | Delete for me/everyone, reconnect, older pagination, reinstall/login; hidden content never reappears |
| Account and call isolation | `data/Backend.kt`, repository, call controller | Switch account/logout during RPC, upload and active call; no previous account's data, notification or media resources |
| Provider validation | FCM variables, device registration, Supabase RPCs | Real token registration/rotation, configured project identity and two-way delivery; no secret values in reports |
| Crash/process/service death | manifest, `calls/ActiveCallService.kt`, application | API 29/35/36 emulator checks plus physical process/task termination; camera/mic released and no orphaned call |

## Execution order

1. Pass Batch 1 unit/lint/build checks and review its diff.
2. Address call dispatch recovery and lifecycle races in separate changes with
   deterministic failure fixtures. Do not mark a server claim as delivered merely
   because a request was attempted.
3. Complete two-phone push, message and call matrix; record commit, APK, account
   fixtures, OS/device, network, steps and observed result for each case.
4. Complete audio/video/network and permissions work before advanced messaging
   or UI polish. Use `CALL_PRODUCTION_RELEASE_GATES.md` for public-release gates.

CI is not physical-device acceptance. This plan does not certify production
readiness or enable calling in the default production build.

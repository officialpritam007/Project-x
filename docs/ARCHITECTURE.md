# Liquid Chat — Architecture

**Updated:** 2026-10-23 | **Status:** source-grounded system map; deployment/runtime still require verification

## System overview

Single-module native Android app using Kotlin, Jetpack Compose/Material3, ViewModel/StateFlow, Room, WorkManager, OkHttp and WebRTC. Backend services are Supabase Auth, Postgres/RLS/RPC, Realtime, private Storage and Edge Functions. Firebase Cloud Messaging delivers push notifications. Authenticated Cloudinary media is optional; protected Supabase Storage is the current fallback.

\`\`\`mermaid
flowchart TD
  UI[Compose screens / LiquidApp] --> VM[AppViewModel + CallController]
  VM --> REPO[ChatRepository / domain state]
  REPO <--> ROOM[(Room account-scoped cache, drafts, outbox)]
  REPO --> WM[WorkManager workers]
  REPO --> API[Backend / authenticated HTTP + session]
  WM --> API
  API <--> SUP[(Supabase Auth / Postgres RLS, RPC, Realtime)]
  API --> EDGE[Supabase Edge Functions]
  EDGE --> MEDIA[Private Supabase Storage / optional Cloudinary]
  EDGE --> FCM[FCM transport]
  FCM --> ANDROID[Android device notifications]
  VM --> RTC[CallController / CallEngine / WebRTC]
  RTC --> SUP
\`\`\`

The arrows describe logical responsibilities, **not a guarantee** that every production integration is provisioned or tested.

## Code ownership map

| Concern | Source |
|---|---|
| Application initialization, Room migrations, backend/repository providers | [LiquidApplication.kt](../app/src/main/java/com/pritam/liquidchat/LiquidApplication.kt) |
| Entry, auth/profile gating, navigation | [MainActivity.kt](../app/src/main/java/com/pritam/liquidchat/MainActivity.kt), [LiquidApp.kt](../app/src/main/java/com/pritam/liquidchat/ui/LiquidApp.kt) |
| UI orchestration, flows, account actions | [AppViewModel.kt](../app/src/main/java/com/pritam/liquidchat/AppViewModel.kt) |
| Local state, syncing, drafts, outbox | [ChatRepository.kt](../app/src/main/java/com/pritam/liquidchat/data/ChatRepository.kt), [LocalDatabase.kt](../app/src/main/java/com/pritam/liquidchat/data/LocalDatabase.kt) |
| Auth, API, refresh, media RPC | [Backend.kt](../app/src/main/java/com/pritam/liquidchat/data/Backend.kt), [SessionStore.kt](../app/src/main/java/com/pritam/liquidchat/data/SessionStore.kt) |
| Background send and notification work | [SendWorker.kt](../app/src/main/java/com/pritam/liquidchat/data/SendWorker.kt), [Push.kt](../app/src/main/java/com/pritam/liquidchat/data/Push.kt) |
| Calling and lifecycle | [CallController.kt](../app/src/main/java/com/pritam/liquidchat/calls/CallController.kt), [CallEngine.kt](../app/src/main/java/com/pritam/liquidchat/calls/CallEngine.kt) |
| SQL, RLS, privileged operations | [supabase/migrations](../supabase/migrations), [supabase/functions](../supabase/functions), [supabase/tests](../supabase/tests) |
| CI and packaging | [Android workflow](../.github/workflows/android.yml), [release workflow](../.github/workflows/release.yml) |

## Main data flows

**Message send:** Compose action → ViewModel → repository inserts account-scoped Room message with stable logical ID → WorkManager/authorized Backend RPC → server acceptance → Room status reconciliation → realtime/refresh/recipient acknowledgments. Do not equate backend acceptance with recipient delivery or read. On interruption, preserve draft/outbox and resume with the same ID.

**Incoming message:** authorized backend/realtime or FCM hint → authenticated refresh/sync → Room upsert keyed by account/message → StateFlow → Compose. Reject stale snapshots that resurrect deleted content; suppress notifications only when the exact chat is visible under the correct account.

**Media:** validate intended audience and object → authorized Edge/RPC path → private Storage or optional authenticated Cloudinary → short-lived authorization URLs for eligible users. Do not expose public direct bucket access or privileged provider keys in Android source.

**Call:** call controller and engine manage permissions, session ownership, signaling, foreground service/audio resources and peer teardown. Real network traversal, TURN and two-device media reliability are explicit acceptance tasks.

## Persistent state and trust boundaries

- **Local:** Room stores chats/messages/contacts/drafts and other data keyed by account; display preferences are device-local. SessionStore protects authentication material. On logout, cancel account jobs and clear only that account's sensitive state without deleting other accounts.
- **Server:** Postgres is authoritative for identity, message membership, receipts, privacy and allowed mutations. RLS and secured RPC/Edge checks form the actual security boundary.
- **Ephemeral:** Realtime/FCM indicate potential changes; they must not override trusted durable state.
- **Secrets:** server/service-role keys, FCM service-account JSON and Cloudinary secret belong only in trusted provider configuration; do not commit or log them.

## Constraints and failure design

Android package \`com.pritam.liquidchat\`; API 29 minimum, compile/target API 36 at snapshot; JDK 17 and Gradle 8.11.1. Preserve Supabase Free compatibility and existing data. Applied SQL migrations are immutable; changes require additive migrations and rollback-safe fixture tests. Network loss, session expiry, process death, permission denial and duplicate events need explicit outcomes. Do not silently requeue permanent failures or erase unsent user data.

## Verification and known limitations

See [implementation gap review](IMPLEMENTATION_GAP_REVIEW_20261023.md), [QA matrix](QA_MATRIX.md) and [release audit](PRODUCTION_RELEASE_AUDIT.md). The 2026-10-23 gap review reports no local Android compilation because SDK/Gradle networking was unavailable, despite historical CI. Root cause of [Compose teardown issue #91](https://github.com/officialpritam007/Project-x/issues/91) remains open. No production approval is inferred from this architecture map.

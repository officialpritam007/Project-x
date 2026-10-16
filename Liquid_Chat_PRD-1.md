# Liquid Chat — Product Requirements Document

Version: 1.1 · Date: 16 October 2026 · Owner: Pritam Pal

Status: Proposed implementation baseline. All performance numbers and limits below are product targets, not measured results.

## 1. Product vision

Build a native Android application for reliable person-to-person messaging with an iOS 26 Liquid Glass-inspired interface. Users should be able to open a conversation, send a message, and understand its delivery state immediately, without repeated loading, visual jitter, or confusing connection errors.

The visual experience should feel refined: white translucent surfaces, soft depth, rounded shapes, subtle highlights, and fluid transitions. Reliability, accessibility, and readable messages take priority over decorative effects.

## 2. Audience and primary journeys

Primary users are individuals chatting privately with friends, partners, and family on Android phones, including affordable devices and unreliable mobile networks.

- A new user signs in, creates a profile, finds another user by username, and sends a first message.
- A returning user opens a cached conversation immediately and continues chatting while updates synchronize.
- An offline user writes messages, sees a clear queued state, and receives reliable confirmation after reconnection.
- A recipient receives a notification, opens the correct conversation, and sees accurate unread and read states.
- A user controls who can contact them, see their presence, and view notification previews.

## 3. Scope and priorities

P0 is mandatory for MVP release. P1 follows after P0 meets its acceptance criteria.

| Priority | Scope |
|---|---|
| P0 | Email/password and Google sign-in; password reset; profile and unique username |
| P0 | One-to-one text messages; durable offline queue; explicit send, delivery, and read states |
| P0 | Chat list, username-based contacts, search, unread counts, favorites, archive |
| P0 | Online/last seen controls, in-conversation typing indicator, push notifications |
| P0 | Image sharing, profile photos, blocking, reporting, account deletion |
| P0 | Light/dark appearance, accessible glass components, performance and security verification |
| P1 | Voice messages, swipe reply, reactions, starred messages, delete for everyone |
| P1 | Video and document sharing, custom wallpapers, biometric chat lock |

Excluded: group chats, audio/video calls, communities, public feeds, disappearing status updates, advertising, payments, and an iOS client.

Baseline decisions carried forward for Liquid Chat: Kotlin/Compose; Supabase Free for Auth, Postgres and Realtime; FCM for Android push notifications; Cloudinary media hosting; no end-to-end encryption in this version. The app must not claim end-to-end encryption. Any later change to this decision requires a separate design and migration plan.

## 4. Navigation and screens

Primary bottom navigation: **Chats · Contacts · Settings**. Hide it inside conversations and full-screen media.

| Screen | Required contents |
|---|---|
| Welcome/login | Consistent logo, email, password, show/hide password, sign-in, Google sign-in, create account, forgot password |
| Create account | Name, email, password, confirmation, field validation, progress and actionable errors |
| Password reset | Email entry and neutral confirmation that does not expose account existence |
| Profile setup | Avatar, display name, unique username, optional bio |
| Chats | App name, search, All/Favorite/Archive/Unread filters, chat rows, new-chat action |
| Contacts | Saved contacts, username lookup, empty state, start-chat action |
| Conversation | Avatar/name, permitted presence, message history, bubble-area typing, composer, attachment action |
| Contact details | Profile, shared media entry, mute, favorite, archive, block, report |
| Settings | Profile hero, appearance, notifications, privacy, storage, help, account actions |
| Media preview | Image preview, optional caption, upload progress, cancel/retry; media viewer after send |

All asynchronous screens need distinct loading, empty, offline, permission-denied, and recoverable-error states. A failed network request must not replace already loaded content with a blank screen.

## 5. Liquid Glass design requirements

Apple describes Liquid Glass as a dynamic material for controls and navigation. This PRD adapts that visual direction to Android; it does not require Apple platform rendering APIs or pixel-identical behavior.

Design references:
- https://developer.apple.com/documentation/TechnologyOverviews/liquid-glass
- https://developer.apple.com/design/human-interface-guidelines/materials

### Visual system

- Use a soft pearl-white background in light mode and charcoal in dark mode, with restrained blue accents.
- Apply glass mainly to navigation, headers, the composer, floating controls, sheets, and profile cards.
- Keep message bubbles sufficiently opaque to preserve contrast over every supported wallpaper.
- Glass consists of restrained background blur where supported, translucent tint, a fine highlight border, and soft shadow. Provide a tinted opaque fallback.
- Use a shared token system: 8/12/16/24/32 dp spacing; 16 dp compact controls, 24 dp cards and message bubbles, 28 dp hero cards and sheets, and capsule navigation.
- Components of the same role use identical corner treatment across all screens.
- Proposed typography: 28–32 sp main titles, 20 sp section titles, 16 sp message/body text, 12–13 sp metadata; support system font scaling.
- Use one logo and one coherent icon family throughout. Include “Developed by Pritam Pal” in About.

### Motion and accessibility

- Use short, restrained spring transitions for selected controls, sheet presentation, and message insertion.
- Do not animate the entire history when a new message arrives.
- Never change scroll position because an image finishes loading; reserve its dimensions.
- Offer Normal, Balanced, and Extra animation settings, plus reduced motion and reduced transparency. Accessibility choices override decorative settings.
- Require at least 48 dp touch targets, TalkBack labels, logical focus order, and sufficient text contrast: target 4.5:1 for normal text.
- Test 200% text scaling, keyboard visibility, gesture navigation, and large display scaling.
- Avoid nested live blur layers. Reduce effects automatically when the device cannot sustain smooth interaction.

## 6. Functional requirements and acceptance criteria

### FR-01 — Authentication and account lifecycle

Persist valid sessions across restarts. Handle token refresh without unexpected navigation to login. Provide field-level validation and disable duplicate submissions.

Acceptance: registration, sign-in, password reset and sign-out succeed on real accounts. Logout removes account-scoped local data, listeners, drafts, queued jobs and notification-token association; warn before discarding unsent messages. Another account must never see the previous account’s content.

### FR-02 — Profiles and discovery

Require a unique, case-insensitive username, display name, optional bio and avatar. Enforce username uniqueness atomically. Discover users through an explicit username query; do not expose an unrestricted directory download. Phone contact upload is outside MVP.

Acceptance: concurrent attempts to claim the same username yield one owner. Avatar updates appear consistently without repeated downloading on every navigation.

### FR-03 — Conversation identity and history

Create exactly one conversation for each pair of distinct users. Use immutable message IDs, server ordering metadata and cursor pagination. Preserve local scroll position when older history loads.

Acceptance: simultaneous first messages create one conversation. Reconnection, process recreation and repeated backend events never duplicate messages.

### FR-04 — Sending and receipts

Persist a message locally before sending. Maintain explicit queued, sending, sent, delivered, read and failed states.

- Sent: backend has accepted the message.
- Delivered: a recipient app instance has acknowledged receipt; push submission alone is insufficient.
- Read: the recipient has displayed the conversation under the read-receipt policy.
- Failed: permanent failure or exhausted retry budget; show an explicit retry action.

Retry transient failures with bounded backoff and an idempotency key. A manual retry retains the logical message ID. Do not silently cycle failed messages back into sending.

Acceptance: airplane mode, app termination and reconnection preserve queued messages. Duplicate retry attempts produce one server message. No sender sees a delivered/read indicator without the matching acknowledgment.

### FR-05 — Chat list and search

Show avatar, name, latest visible message preview, timestamp, unread badge and applicable delivery state. Support All, Favorite, Archive and Unread filters. Favorite/archive are per-user preferences.

Search contact names/usernames and downloaded message history. Label local history search clearly; full server-history search is outside MVP. Results retain the correct conversation and sender identifiers.

Acceptance: sending updates the row without refreshing the entire list. Incoming messages keep archived conversations archived but update their unread badge. Search results open the correct conversation/message.

### FR-06 — Presence and typing

Show permitted online/last-seen information in the header. Show typing only in the message area.

Request an offline update immediately when the app backgrounds; use a short server-validated expiry for crashes, force stops and lost networks. Proposed stale-presence expiry: 30 seconds. Instant remote offline status cannot be guaranteed when connectivity is absent.

Throttle typing updates and expire them after five seconds without refresh. Hide typing after send, navigation or backgrounding. Presence restrictions must also hide underlying timestamps from unauthorized readers.

Acceptance: backgrounding starts offline transition without an intentional grace period. Stale presence expires. Users who cannot view presence cannot retrieve heartbeat metadata through direct backend reads.

### FR-07 — Notifications

Send notifications through a trusted backend; never embed server messaging credentials in the APK. Open the correct conversation from notification taps.

Suppress the notification only when that exact conversation is visible and the app is in the foreground. Continue showing notifications for other conversations unless muted. Respect Android notification permission and user preview settings.

Acceptance: foreground/background/terminated-state tests route correctly. Denied notification permission does not block messaging. No placeholder such as “Encrypted message” appears. Delivery latency under OS battery restrictions is treated separately from foreground message latency.

### FR-08 — Media

For MVP, support images and profile photos through Cloudinary. Validate file type, size and ownership on the trusted side. Proposed message image limit: 10 MB before upload; compress sensibly and preserve aspect ratio.

Show preview, optional caption, progress, cancellation and retry. Publish the final attachment only after upload succeeds; clean up abandoned uploads.

Private chat attachments require authenticated or time-limited access. A public media URL alone does not satisfy participant-only privacy. If the selected hosting configuration cannot meet this requirement within budget, private attachments remain a release blocker pending a storage decision.

Acceptance: interruption cannot create a broken attachment or duplicate message. Nonparticipants cannot retrieve private attachments after access expiry.

### FR-09 — User controls and deletion

Provide mute, favorite, archive, block/report and delete-for-me. Enforce blocks on the server in both directions; hiding a composer is insufficient. Reports include only explicitly selected content and go to a restricted review queue.

Persist delete-for-me state remotely per user so cache rebuilds and reinstall do not restore hidden content. Deleting a conversation hides history up to a deletion watermark; a later new message may create a fresh visible row.

P1 delete-for-everyone: sender-only, proposed 24-hour window, backend-enforced tombstone, removal of media access, and truthful confirmation of success/failure.

Acceptance: blocked sends fail even through direct backend requests. Deleted-for-me history stays hidden after resync. Deletion cannot recall screenshots or copies already saved by a recipient.

### FR-10 — P1 conversation enhancements

Voice: hold to record, slide to cancel, lock for hands-free recording, preview before send, playback progress and interruption handling. Proposed limit: five minutes.

Reply: swipe or long-press to quote, with a stable reference to the original message. Reactions: one current reaction per user per message. Long messages: expand/collapse without losing position. Biometric lock: protect content in app previews and notification settings as well as navigation.

## 7. Data and architecture constraints

Use native Kotlin with Jetpack Compose and a unidirectional state flow. Separate UI, domain behavior and backend access so delivery logic does not depend on screen lifetimes.

Baseline components: Supabase Auth for identity, Supabase Postgres for durable messaging data, Supabase Realtime for authorized change notifications and ephemeral events, FCM for Android push, Cloudinary for media, Room for account-scoped local history/outbox, and Supabase Edge Functions for privileged operations. Supabase Free is the required plan; paid upgrades and add-ons are outside authorization.

Suggested logical entities:

| Entity | Required information |
|---|---|
| User profile | User ID, display name, username, avatar reference, bio |
| Private settings | User ID, privacy controls, notification preferences |
| Conversation | ID, exactly two participant IDs, server timestamps |
| Message | ID, conversation ID, sender ID, type, content/media reference, client ordering data, server timestamp |
| Per-user conversation state | Favorite, archive, mute, read watermark, deletion watermark |
| Receipt | Message/conversation reference, recipient ID, delivered/read watermark |
| Presence/typing | Authorized user/conversation, state, expiry |
| Block/report | Actor, target, timestamps, minimal report evidence |
| Local outbox | Message ID, account ID, payload, attempt count, state |

Use atomic operations for message creation and dependent metadata where necessary. Protect concurrent last-message updates from older messages overwriting newer previews. Prefer per-user last-read watermarks over rewriting every message.

UI requirements: stable list keys, immutable models, lifecycle-aware state collection, incremental updates, cached image loading, no listener creation during recomposition, and no expensive I/O on the main thread.

## 8. Privacy, security and operations

- Enforce participant authorization for every conversation, message, receipt and attachment operation.
- Validate sender identity, immutable participant lists, allowed fields, content limits and state transitions server-side.
- Separate public profile information from email, device tokens, private settings and presence internals.
- Keep privileged credentials out of the client and repository. Restrict upload operations and apply abuse limits.
- Use secure transport and appropriate provider storage controls; describe protection truthfully without an end-to-end-encryption claim.
- Account deletion requires reauthentication and removes profile, tokens and owned media through a tracked job. Specify in the UI whether previously sent text remains in the other participant’s history.
- Proposed operational retention: diagnostic events 30 days; resolved abuse reports 90 days; raw message bodies excluded from analytics and logs.
- Rate-limit discovery and sends; provide restricted moderation access and auditable administrative actions.

## 9. Quality targets

Measure on a representative 4 GB RAM Android phone and one lower-end device, using a 1,000-message conversation and 200-chat fixture. Proposed minimum OS: Android 10; confirm before implementation. Select target SDK against distribution requirements at release.

| Metric | Release target and measurement |
|---|---|
| Cached conversation display | p95 ≤ 300 ms after tap in a warm app |
| Local send feedback | p95 ≤ 100 ms after pressing Send |
| Foreground text propagation | p95 ≤ 2 seconds with both devices online on a controlled network |
| Scrolling | Under 5% janky frames in a defined 60-second scroll on a 60 Hz reference device |
| Stability | At least 99.5% crash-free sessions during a meaningful beta sample |
| Reliability | Zero lost or duplicated accepted messages in controlled retry/process-death tests |
| Correctness | Zero wrong-conversation search or notification routing in release tests |

Measure tap-to-render, send acknowledgment, failures by category, queue age, crashes and frame performance without collecting message content. Track sign-up-to-first-message conversion and seven-day sender retention as learning metrics; set growth targets after beta baselines exist.

## 10. Implementation milestones

1. **Foundation:** approved screen states and design tokens; auth; profile; data model; authorization rules; local cache and deterministic conversation identity.
2. **Messaging core:** text delivery, durable outbox, receipts, pagination, chat filters, local search and deletion state.
3. **Release features:** presence, typing, notifications, image flow, privacy controls, block/report and account lifecycle.
4. **Hardening:** device profiling, reduced-effects fallback, accessibility, abuse tests, interrupted-network tests and monitored beta.
5. **P1 expansion:** voice, replies, reactions, additional attachments and chat lock only after the messaging core passes.

Each milestone ends with a runnable debug APK, a concise change list, and evidence for its acceptance criteria. Schedule estimates require a repository audit and team capacity; this PRD does not assume the existing implementation is complete.

## 11. Release gates

- Two physical devices complete registration, discovery, messaging and notification journeys.
- Offline send, app restart, reconnect and duplicate-event tests pass without data loss.
- Logout, account switching and deletion do not leak cached data.
- Backend tests prove nonparticipant denial, block enforcement and private presence protection.
- Images cannot bypass the chosen private-access policy.
- No recurring blank containers, list-wide reloads, unexpected scrolling or avatar flicker.
- Light/dark mode, TalkBack, 200% text and reduced effects remain usable.
- No unresolved critical/high-severity bugs; all P0 requirements pass.
- Provide installable APK, reproducible build configuration, backend deployment instructions and documented remaining limits.

## 12. Decisions to resolve before production

The core backend must remain on Supabase Free. Do not enable paid upgrades or add-ons without explicit owner approval. Independently verify Cloudinary and authentication email provider limits. Quota exhaustion may reduce availability; it must never silently trigger a paid upgrade. See the Free-plan requirements below.

Confirm the exact minimum Android version, username-change policy, sender deletion window, attachment limits, account-deletion treatment of recipient copies, and the owner of abuse-report review. Defaults in this PRD allow design and implementation planning to start; these decisions must be finalized before public launch.

## 13. Supabase Free implementation requirements

This section supersedes the previous Firebase Auth/Firestore architecture. The Android UI and one-to-one scope remain unchanged. This document updates the plan; it does not certify that application code or live data has been migrated.

### Verified plan baseline — 16 October 2026

| Resource | Free allowance |
|---|---|
| Database | 500 MB per project |
| Authentication | 50,000 monthly active users |
| Egress | 5 GB; separate 5 GB cached egress allowance |
| Supabase file storage | 1 GB |
| Realtime | 2 million messages per month; 200 peak connections |
| Edge Functions | 500,000 invocations |
| Active projects | 2 |
| Inactivity | Free projects pause after one week of inactivity |
| Automatic backups | Not included |

Sources: https://supabase.com/pricing and https://supabase.com/docs/guides/platform/billing-on-supabase

Realtime messages are service events, not an allowance of two million user-written chat messages. Fan-out, receipts, typing and presence can also consume resources. Peak connections are connections, not a guaranteed number of distinct people. These quotas do not establish tested messaging capacity.

### Authorization and durability

- Enable and test Row Level Security on every exposed table. Restrict messages, receipts and conversation state to authorized participants; protect private settings and device tokens by owner.
- Apply block checks to backend send operations and validate authenticated sender identity. Realtime channels must enforce the same privacy rules; private channel access must be re-evaluated when membership or blocking changes.
- Keep service-role credentials exclusively in trusted backend secrets. The APK uses the appropriate public client key with authenticated sessions and RLS.
- Use database constraints and transactions to enforce unique usernames, one conversation per user pair and idempotent message insertion.
- Persist messages in Postgres before notifying recipients. Treat Realtime events as synchronization hints, not durable message storage.
- After reconnect, query changes using server ordering and deduplicate by message ID. Reconcile deletions and receipts as well as new messages. Room remains the UI source of cached history and the durable outbox.
- Implement a durable notification outbox and an idempotent Edge Function dispatcher to FCM, with retry tracking. Validate authorization and mute/block rules before dispatch. Never treat an FCM response as proof of recipient delivery.

### Free-plan resource controls

- Use one shared Realtime connection per active app session where practical; subscribe only to authorized, necessary events and release unused subscriptions.
- Paginate history, cache avatars and thumbnails, throttle typing updates and avoid database writes on every keystroke.
- Keep media binaries out of Postgres. Store validated metadata and protected media references; Cloudinary remains subject to the private-media requirement in FR-08.
- Monitor database size, egress, Realtime traffic/connections and function invocations. Proposed owner warning thresholds: 70%, 85% and 95% of allowance.
- Reduce nonessential typing/presence traffic near limits. Preserve unsent messages locally if writes become unavailable and show a clear service-unavailable state.
- Do not delete user message history automatically to meet a quota. Require an explicit retention policy and export option.
- Stay on the Free organization plan and avoid paid add-ons. Do not depend on paid-plan spend caps as a zero-cost guarantee.
- Plan explicit database exports and tested restores because Free does not include automatic backups. Keep backups access-controlled and never place private chat exports in a public repository.
- Handle paused projects as service downtime; show cached history and retain queued sends. Document owner recovery steps rather than promising continuous availability.

### Authentication delivery dependency

Keep Google sign-in as an initial supported path. Email verification and password reset require a separately validated email-delivery setup and its free allowance; do not assume the bundled development email service supports public launch. Avoid SMS authentication in the zero-cost baseline. Email/password release remains gated on successful real-user delivery tests.

### Existing-app migration gate

1. Audit current Firebase account IDs, conversations, media references and pending messages before changing adapters.
2. Prepare SQL migrations, indexes, RLS policies and negative authorization tests in an isolated environment.
3. Establish a verified mapping between legacy user IDs and Supabase Auth IDs. Do not assume existing sessions or passwords transfer automatically; choose and test a supported migration or reauthentication flow.
4. Back up existing data, run a migration rehearsal and verify counts, ownership, message order, deletion markers and attachments.
5. Test Supabase adapters on two Android devices, including offline queues, blocked sends, presence, notifications and account switching.
6. Define a controlled cutover that prevents diverging writes between Firebase and Supabase; reconcile any pending sends.
7. Keep a rollback plan and the old backend intact until migration acceptance is complete. Remove Firebase Auth/Firestore dependencies only after confirming they are unused; retain FCM integration.

Supabase Free is suitable for initial development and a bounded beta under these requirements. Public-release acceptance must include measured load and quota headroom; continuous availability and unlimited growth are not promised.

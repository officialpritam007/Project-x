# Production release audit — 17 October 2026

## Phase 0 baseline

Reviewed attached master plan completely. Repository baseline: `d2817bf790126b564f151cfccad8c3e73049f190`, app 0.7.0 (7), package `com.pritam.liquidchat`, min 29 / target 35. PR #2 already merged by owner; PR run 39022353411 and main run [39022973604](https://github.com/officialpritam007/Project-x/actions/runs/39022973604) passed build, API-35 instrumentation, and Edge checks. No second merge attempted.

## Architecture and feature inventory

Single Android app module. LiquidApp controls auth/profile gating and Chats/Contacts/Settings/Profile navigation. ChatScreens and ConversationParts render conversations, drafts, message actions, search and timeline. Moments retains Stories/Posts. VoiceMessages and VoiceRecorder implement foreground recording/playback. GlassComponents/Theme/DisplayOptions preserve light/dark Liquid Glass and accessibility preferences.

AppViewModel owns UI state; ChatRepository owns Room v7, account-scoped drafts/hidden messages, RPC sync, realtime hints and WorkManager outbox. Backend uses encrypted SessionStore and serialized refresh/PKCE. SendWorker reuses client UUID; ReceiveWorker authorizes previews with fresh RPC before notifying. FCM is push only. Media Edge Function uses protected storage fallback or optional authenticated Cloudinary. No groups/calls added; no E2EE claim.

Implemented but not physically verified: email/Google auth/recovery, profiles/contacts, 1:1 text/image/voice, replies/reactions/edit/delete/hide/stars, pagination, pinned/manual unread, search, mute/block/report-user, Stories/Posts, privacy settings, account erasure. Partial: content-specific moderation/reporting, terms acceptance, external deletion route, release signing/AAB, final Play listing. P2 remains deferred until P0 acceptance.

## Connected backend evidence

Project `qsdmspylumxysczzqnqc`: ACTIVE_HEALTHY; 25 migrations match repository inventory; four ACTIVE Edge Functions (media, notify, account, media-cleanup). Four active cron jobs: account cleanup/notification dispatch every 5 minutes, operational retention daily, media cleanup every 30 minutes. DB size observed 14,538,419 bytes; this is not total usage/egress/plan verification. No public ordinary table lacks RLS. Security advisor: leaked-password protection disabled; do not upgrade billing. Performance advisor: three unused-index INFO notices; no index removed. No migration or production user data changed.

SQL fixture suites are being re-run in rollback-only transactions; results are recorded in QA_MATRIX. Provider credentials, SMTP delivery, OAuth console configuration and FCM device delivery remain unverified. Keys/tokens/private messages were not read or printed. Current plan/subscription and usage-dashboard limits are not independently exposed by the project listing.

## Release gates

NO-GO for production. API 35, no secure release signing pipeline, no signed AAB, no two-physical-device acceptance, missing hosted privacy/deletion URLs and owner moderation setup. Play Console connection/account type/creation date/eligibility/package registration unavailable. Local SDK/emulator/Gradle unavailable; Gradle wrapper attempted and failed downloading due network restriction. Use GitHub CI for changed code, never equate prior CI with future patch validation.

Official target requirement verified: API 36 for new submissions/updates from 31 August 2026: https://support.google.com/googleplay/android-developer/answer/11926878

## Security coverage limitation

Provider advisors and selected source/SQL authorization paths reviewed. This is not an exhaustive independent Codex Security scan; that skill's referenced scan resources were unavailable. Dependency vulnerability/license inventory and adversarial full-client integration remain open.

## Phase 1 implementation

Draft text/reply submissions carry persisted monotonic revision tokens. Room consumes a draft only if account, conversation, text, reply and revision match. Text and image callbacks clear only their submitted revision; cursor-only changes do not rewrite drafts. Room rejects stale worker progress after server acceptance, and retry checks status transactionally. Existing editor stays enabled during queuing. All changes await phase-specific CI.

Live provider blocker: 62 recorded HTTP503 responses explicitly match the notify function's missing-FCM_SERVICE_ACCOUNT_JSON path. 13 jobs were pending with zero attempts. Cron's 403 successful SQL executions only prove dispatch scheduling, not successful push. Credential configuration and real-device delivery are owner/provider gates. No secret values accessed.

All three SQL suites passed and all fixture identities were verified absent. Baseline downloaded instrumentation XML contains 13 tests, zero failures/errors/skips; synthetic UI screenshots inspected. This does not establish physical-device reliability.

## Phase 2 local-session reliability

Offline logout now completes local encrypted-session, Room, media-cache, notification and work cleanup even if remote token deregistration or sign-out fails. Cancellation propagates only after local cleanup; storage cleanup failures are not reported as success. UI honestly reports unconfirmed server revocation. An isolated interceptor-backed Android test makes zero HTTP writes and verifies account/cache/token erasure plus other-account retention. Unit tests cover success/offline/cancellation/cleanup failure. Missing uncached profile offline now shows explicit error plus Retry/Sign out instead of an indefinite loading label. CI pending.

Provider-bound auth email/Google/recovery/deletion journeys, FCM and physical lifecycle testing remain blocked or unverified; these are not certified by local signout tests. No deployed migration changed.

Voice queuing now rechecks account identity inside its Room transaction so logout/account changes cannot enqueue an old-account recording after cache cleanup.

## Phase 7: policy and private content reporting

Phase 1 PR #4 and Phase 2 PR #5 each passed all three CI jobs before merge. Added explicit versioned, account-scoped User Policy consent before profile/messaging/uploads, readable policy in Settings, message evidence through existing report_contact, and visible Story/Post report/block controls. Consent is local client enforcement; old clients are not retroactively gated.

The new additive authorized_moment_reporting migration creates only private/public report_moment functions. Prototype rollback SQL passed. It verifies current session, audience, both-way blocks, active media, expiry/archive, self-report denial and shared report rate limit; the evidence ID is bound server-side. Existing 25 migrations and all user data remain intact. MODERATION_OPERATIONS documents owner-only triage, retention and recovery. Function deployment is backward compatible with existing clients. New UI still requires CI; support contact, policy/deletion hosting, operator adoption and provider/physical acceptance remain open.

Privacy follow-up: account cleanup now deletes camera capture scratch and image disk cache as well as account media, performs file IO off the UI thread, and propagates deletion failure. An Android fixture checks both file paths disappear. The report RPC was deployed as migration 20261017103232; its deployed rollback suite passed, all reporting fixtures were absent afterward, and advisors show only the pre-existing password warning / three unused-index INFO notices.

## API36 candidate engineering and release pack

Policy/privacy PR #6 passed all CI jobs before merge (run 39025444471). Candidate engineering upgrades AGP to supported 8.10.1 / compile+target 36, keeps Java17/Gradle8.11.1/min29, versions 1.0.0-rc1 (8), enables R8/resource shrinking and adds debug/release tests/lint, unsigned AAB inspection and API29/35/36 CI. APK signature, ZIP alignment, native LOAD alignment and SHA/size/source/version metadata are checked. Owner-only manual signing uses runner-temporary upload keys and never publishes. New auth interceptor tests exercise concurrent expired-token and 401 refresh recovery without real HTTP writes. These new candidate checks require successful CI before merge.

Existing logo identity is preserved in adaptive/themed launcher and monochrome notification icons; 512px Play icon and 1024x500 feature graphic were generated and visually inspected. Listing/privacy/Data Safety/deletion drafts, moderation operations, signing/recovery, free-tier operations and a complete original-checklist status register are included. Final support/privacy/deletion hosting, owner moderation adoption, provider setup, signed runtime, physical devices and Play eligibility remain blocked. P2 remains deferred.

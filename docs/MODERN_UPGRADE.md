# Modern messaging upgrade — 0.7.0 (7)

Upgrade baseline: `75e956553fa4fb83f78eaae26c7cf5ff50ee5bd1` (17 October 2026).
Implementation: PR #1, `upgrade/modern-messaging-v11`. Native Android 10+, Supabase Free, private media, one-to-one only. No billing/service-plan changes.

## Implemented scope

- Visible All/Favorite/Archive/Unread capsules, long-press inbox actions, server-owned three-pin budget and manual unread without rolling back receipts.
- Account/conversation-scoped Room drafts including reply target, atomic draft-to-outbox persistence, saveable conversation scroll, cursor-aware emoji insertion, multiline sentence-capitalization hints and 4,000-character limit. IME behavior still needs Gboard/OEM verification.
- Reverse-layout, stable-key timeline with automatic older-page loading/retry, jump-to-latest, date chips, typing bubble, adaptive blue glass-era bubbles and accessible clock/tick/error indicators. A 1,000-message viewport regression is included.
- Long-press copy/share text, reply, reaction toggle/replace, private star, edit and deletion. Swipe reply supports directional layout and threshold haptics. Own reaction has a distinct border. Reply quotes resolve current visible local originals; unavailable originals show a neutral fallback.
- Text edits preserve ID/sequence and have a server-enforced 15-minute window. Delete-for-everyone is sender-only within 48 hours; it clears media access and leaves a tombstone. Existing screenshots, exports or previously downloaded content cannot be recalled.
- Delete-for-me is a private server preference; Room suppression records and history watermarks guard against stale outbox/cache re-insertion. Queued/failed local messages can be discarded. Retry and failure previews update locally.
- Monotonic per-conversation mutation catch-up handles edits, deletions, reactions and private preferences independently of send/history cursors. Writers serialize on the conversation before allocating mutation IDs. History/search never attribute messages using display-name guesses.
- Four local chat palettes, gallery/camera attachment choices, private camera FileProvider, caption capitalization, bounded photo sizing and pinch/pan/reset in the viewer.
- Authorized paginated Media/Voice/Starred lists in contact details. Stars are not limited to downloaded history; opening an older result fetches history until the original position is available.
- Actual Data and Storage screen with cache size/clear and honest limits. Unsent media is kept separately. Existing Stories/Posts/AAC recording/playback and the four icon-only tabs remain.
- Notification worker obtains a fresh authorized preview; it rechecks hidden/deleted/muted/blocked content and current preview preferences. The click route includes and verifies account identity before opening. Disabling read receipts suppresses the read watermark returned to the peer.

## Data rollout

All server changes are additive and were deployed before the new client:

1. `20261017075102_modern_message_actions.sql` — message action metadata, private preferences, action/history/change-feed RPCs, pin/unread, media retirement and fresh notification authorization.
2. `20261017084757_action_privacy_and_stars.sql` — explicit private-table denial, FK index, paginated star lookup and read-receipt privacy.
3. `20261017085147_erase_message_preferences.sql` — remove new private preferences during account erasure.
4. `20261017091111_private_message_preference_changes.sql` — keep star/hide mutation IDs private to the actor; only an owner-scoped state hint is emitted, with a negative test that the peer's message row and change feed stay unchanged.

Room v1→v7 upgrades preserve pending messages. New v5 drafts/appearance, v6 message actions/cursors/pins, and v7 local hide suppression use forward migrations; there is no destructive fallback. Do not edit applied migration files. Future fixes need a new migration.

## Verification record

- Deployed rollback-only `authorization.sql`: PASS (participants, sessions, blocks, deletion watermark, media, erasure, profiles/contacts).
- Deployed rollback-only `moments_voice.sql`: PASS, including deleted voice media denied to owner/recipient and excluded from shared-media browsing.
- Deployed rollback-only `message_actions.sql`: PASS (reply idempotency/cross-chat denial, ownership, reaction allowlist, windows, tombstones/catch-up, block denial, fresh previews, private stars, pin maximum, read privacy, preference erasure).
- Fixtures are rolled back. No fixture users or media are retained.
- Supabase security advisor: no new database warning after explicit private-table denial. Existing [leaked-password-protection Auth warning](https://supabase.com/docs/guides/auth/password-security#password-strength-and-leaked-password-protection) is unchanged; no paid Auth upgrade enabled.
- Final [Actions run 22](https://github.com/officialpritam007/Project-x/actions/runs/39019806725): build/unit/lint, Edge checks and all 13 API 35 emulator tests passed (0 failures/errors/skips). Runtime source commit: `d72f17639c544849a08cd21668800f368bd1819b`. The later change is server SQL/tests/documentation only; all three SQL suites were rerun and passed against all four deployed migrations. Zero fixture users remain.
- [Debug APK artifact](https://github.com/officialpritam007/Project-x/actions/runs/39019806725/artifacts/12095988874): version 0.7.0 (7), Android 10+, 64,966,223 bytes. APK SHA-256: `b23d2fb5c8685fecb0555b2a5687aef568c5b8470ddc01c9d51945aa188df7cc`.
- API 35 compact-screen screenshots reviewed for inbox, conversation, reply/reaction/edit/star states, settings/profile and live blur with sharp text. Conversation screenshot uses a signed-out rendering fixture, not a live authenticated messaging journey. No physical-phone acceptance is claimed.

## Bug closure ledger

“Source addressed” is not a substitute for a reproduced physical-device defect. Historical reports remain regression targets.

| Bug IDs | Evidence/change | Status |
|---|---|---|
| BUG-001, 019 | Reverse timeline, stable keys, per-account saveable conversation state; viewport test | Emulator viewport regression passed; physical/navigation journeys remain |
| BUG-002 | Separate mutation cursor; upsert current rows without forced count-based scroll | Source risk reduced; frame profiling/device reproduction remains |
| BUG-003, 020 | Existing sender/conversation/account-scoped literal search retained; local search tests | Regression coverage; no new wrong-sender reproduction |
| BUG-004, 017 | Adaptive bubble widths, bounded media aspect, sharp text and existing source-backed blur | Source addressed; screenshot/font/device gates remain |
| BUG-005 | Viewer state stays message-keyed; zoom/pan is local to the open dialog | Regression target; physical flicker not reproduced |
| BUG-006, 007, 029 | Existing stable-ID outbox; local retry/failure preview; existing delivery tests | Preserved and refined; two-device/offline journey remains |
| BUG-008 | Existing authoritative ack/realtime path retained; privacy watermark checked | Provider/two-device delivery timing remains |
| BUG-009, 010, 025 | Fresh authorized notification RPC, exact active-chat suppression, account-bound deep link | SQL authorization passed; real FCM/session matrix remains |
| BUG-011, 021 | Private hide tombstones, local suppression, watermark pruning, durable drafts/migrations | SQL and Room migration/draft tests passed; cross-device/reboot/worker-race journeys remain |
| BUG-012, 013, 014 | Existing private presence/session checks; action/media both-way block tests | SQL passed; OEM/background presence timing remains |
| BUG-015 | Bubble-only typing; throttle retained; focus/background/send clear | Source addressed; physical IME/TTL check remains |
| BUG-016 | Protected-image state now includes account identity; stable cache key retained | Source addressed; avatar profiling remains |
| BUG-018, 028 | Existing IME/insets preserved; flexible bubble/header/voice layout and labeled controls | Compact-screen screenshots reviewed; 200% font/TalkBack/OEM gates remain |
| BUG-022, 023, 024, 030 | Existing private uploads, quotas, AAC lifecycle, IO image preparation preserved; authorized remote media browsing | SQL/media tests passed; expiry/quota/OEM/frame tests remain |
| BUG-026 | Provider setup gates explicitly retained | Owner/provider dependency; not falsely marked fixed |
| BUG-027 | Localized calendar chips and existing instant-based time conversion | Date test included; timezone/midnight physical verification remains |

## Deferred conditional scope and release gates

P2 work remains deferred by the PRD: protected-media forwarding, documents, multi-selection, biometric chat lock, rich link previews, custom gallery wallpapers, playback speed/waveform, low-data auto-download preferences, disappearing messages and export. No disabled fake controls were added for those features. Devices/language/folders retain honest availability explanations.

Hardware/provider acceptance still requires two physical accounts/devices; offline/reboot/process-death outbox and drafts; Gboard and OEM capitalization; Android 10/11 fallback; TalkBack/200% fonts; camera/microphone/OEM lifecycle; real FCM, SMTP and Google OAuth; quota/expiry degradation; backup/restore rehearsal and performance profiling. Older APKs do not understand new mutation catch-up; both participants should update for action rendering. No public production-release certification is made.

## Requirement mapping

Source implemented, with the verification limits above: FR-INBOX-01…06; FR-CHAT-01…12, 14, 16 and curated-palette portion of 17; FR-MEDIA-01 (gallery/camera only), 02 (bounded viewer and zoom), 05 (Media/Voice); FR-PRIV-02…04 and session-safe portions of 06; FR-SET-01 (Data and Storage), 02. FR-CHAT-06A is an IME sentence-capitalization hint, with physical keyboard acceptance pending.

Preserved baseline and regression checked where stated: FR-INBOX-07, FR-MEDIA-03, FR-PRIV-01/07/08, FR-SOCIAL-01/02 and user-selected motion/transparency settings. No new physical-device pass is implied. Conditional P2 requirements and provider/device/backup gates are explicitly deferred above.

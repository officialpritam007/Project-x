# Liquid Chat — All-in-One test candidate implementation register

This report accompanies the calling-enabled test APK requested in [Master Tracker #24](https://github.com/officialpritam007/Project-x/issues/24). The tracker records the immutable final source SHA, Actions run/artifact, checksums and exact verification results after integration. A final test candidate is not a signed Play production release or physical acceptance certificate.

The original `INPUT_MASTER_WORK_PLAN.md` and historical phase reports are preserved. Their earlier no-calling/default-off instructions were superseded by the owner's current voice/video-enabled candidate scope. This register and the latest tracker evidence supersede old milestone snapshots, including old FCM absence and older CI counts.

## Current integrated baseline and verification continuation

The verified resume baseline is **1.0.0-rc8-mb-preview (15)**, main `9d03c70ef18cd3843849af2546b2f99e274ab642`, [Actions #405](https://github.com/officialpritam007/Project-x/actions/runs/39452470810), all five mandatory checks successful. Its calling-enabled APK includes private Home/conversation photo and MP4 wallpapers, theme JSON, tab reordering, row/avatar sizing, scheduled-text editing, quick-reaction presets, per-chat notification channels and text/sender privacy, secure-screen preference, stable Compose BOM 2025.11.01 and cached-avatar loading fixes. See [MB reference/implementation register](MBWHATSAPP_334_REFERENCE_AUDIT.md) and tracker #66 for the incremental evidence. Earlier rc3/rc5 entries below are historical.

This continuation adds five fresh AndroidJUnitRunner invocations of the entire protected-image test class after the mandatory full sharded suite, on **each API29/35/36**. Every round must execute exactly the protected-image methods discovered in the successful full suite, without skips, duplicates, failures or partial completion. The first failed repetition fails the job and is retained in text/JUnit/JSON output; there is no retry-to-green. These repeats exercise intercepted HTTP/Coil, image revocation, tab re-entry and Activity/composition teardown without live accounts. They add evidence for [#91](https://github.com/officialpritam007/Project-x/issues/91); even green repeats do not establish an upstream root cause or certify permanent crash freedom.

No runtime, Supabase schema/data, FCM config, RTC signaling, paid provider or calling flag changes are made by this verification change. The final exact merged-main run and artifact must be recorded in the trackers before it supersedes #405. The [consolidated two-phone checklist](ALL_IN_ONE_TWO_PHONE_TEST.md) retains all existing physical cases; no owner test is requested between development stages.

## Implementation in this continuation

| PR | Change | Evidence boundary |
|---|---|---|
| #38 | Deleted-message body/media/reply/reaction scrubbing in cache/bubble/search | Verified already merged; exact-head run `39206834716`, all five checks passed |
| #41 | Shared Media/Starred refresh only for relevant mutations; account/auth-scope guard | Reviewed and merged after all five checks in `39208283919` |
| #42 | Remove nested duplicate inbox gesture/accessibility handlers | Reviewed and merged after all five checks in `39208710482` |
| #43 | Reject delayed snapshots that resurrect a tombstone; stop deleted/hidden cached voice offscreen | Merged after all five checks in `39211774870`; JVM + real Room instrumentation |
| #44 | Preserve raw page cursor through deleted rows; reject overlapping/stale appends | Merged after all five checks in `39211862514`; pagination/order regression tests |
| #45 | Prevent delayed incoming invitation from replacing outgoing/new-account/background state | Merged after all five checks in `39211945825`; auth-epoch/occupancy regressions |
| #46 | Loaded-photo Story timer, first-video replay, bounded oriented video export, off-main metadata and timeout | Merged after all five checks in `39213558422`; actual local AVC encode/export/decode instrumentation, timer/dimension tests |
| #47 | Downloaded-history sender/type/date search; manual-unread inbox filter/dot | Merged after all five checks in `39212615553`; account/deletion/filter/date regressions |
| #48 | Actual attachment upload bytes; separate Story export/upload/publish stages | Merged after all five checks in `39212924492`; real Okio payload/retry/progress tests; no false Sent at upload completion |
| #49 | Calling/video-Story privacy disclosures and consent versions; 200% text selected UI checks | Merged after all five checks in `39213148137`; light/dark/reduced-transparency semantics tests |
| #50 | Handle transient/duck audio interruptions; reject disposed-peer SDP immediately | Merged after all five checks in `39213450681`; focus-transition and native teardown instrumentation |
| #51 | Restrict lock-screen window access to validated full call UI; reset on end/error/minimize/logout | Exact-head CI required; untrusted intent extras cannot grant over-lock visibility |
| #53 | Propagate deletion/hide of uncached older Shared Media to playback/gallery; prevent an in-flight page restoring it | Exact-head CI required; account-scoped revocation stream, no old body cached, real Room/flow instrumentation and page reducer regression |

The earlier integration package versioned its candidate as **1.0.0-rc3 (10)**, prevents the compact chat-header status/username from wrapping beneath call controls, and updates stale README/calling/free-tier instructions. It preserves both visible call actions and the Liquid Glass palette. That historical companion checklist contained **69 unique acceptance cases**; the current checklist includes the subsequent MB acceptance extension.

Only verified PRs may be merged. If a listed change's new exact-head CI fails, it remains pending and is corrected before final candidate integration. The Story test initially exposed a JUnit signature error; its return type was corrected rather than suppressing or skipping the test. Final tracker evidence supersedes the pending wording above.

## Master Tracker coverage and acceptance status

**Implemented + automated evidence** describes verified source behavior and the applicable CI/SQL checks. **Requires final two-phone test** is an additional physical/provider acceptance gate. **Blocked** identifies an unavailable dependency. **Deferred** identifies a feature absent from this candidate, with a reason. No checkbox is considered physically passed merely because a method exists.

| Tracker scope | Status and evidence | Outstanding acceptance/dependency |
|---|---|---|
| Message synchronization, correct sender, stable accepted/failed state | Implemented; account-scoped Room, UUID idempotency, monotonic acceptance/cursors and no-op suppression; unit/Room/SQL regressions | Final M01–M08; real offline/reboot/OEM permutations |
| Deleted text/media, delete-for-me/history, mutation catch-up | Implemented; scrubbed tombstones, non-resurrection, hidden IDs/deletion watermark, local/global search exclusion; SQL + Room + Compose | Final M10–M15; already exported recipient copies cannot be erased |
| Notifications, foreground suppression, retries and stale account isolation | Implemented; authorized previews, durable dedupe receipts, account-unique WorkManager work, bounded registration/call retries; regression tests | Final N01–N07; real device FCM and provider-project match not certified |
| Keyboard, draft/focus and first-letter capitalization | Implemented; persistent revision-aware draft consumption, stable editor/focus, sentence capitalization; unit/Compose checks | Final M02–M03; successful-send Gboard/OEM behavior |
| Profile/avatar, call history, Stories/Posts repeated loading | Implemented; bounded/coalesced account/auth-scoped read cache, explicit mutation invalidation, stable images, failure recovery and relevant-only gallery refresh | Final A05/A08/A10/C16/S05/S11/S12 |
| Auth, receipts, block/online/last-seen privacy, erasure | Implemented selected paths; encrypted session store, serialized refresh, fail-closed RLS/RPC; live rollback suites | Final A01–A04/M16–M17/R01–R03; live OAuth/email/reset/erasure routes |
| 1:1 incoming/outgoing/ringing/connecting/active/end call UI | Implemented; permission-aware redial, immutable direction, one-call guard, native codec checks, stale invitation protection | Final C01–C06/C09–C10/C16; real peer audio/video required |
| Incoming-call FCM, cancellation, timeout, busy/block/logout/process lifecycle | Implemented bounded signaling, server invitation validation, safe notification actions, active-call service continuity | Final C09–C13/N05–N07; lock screen and OEM background restrictions |
| WebRTC codecs/sinks/preview/mute/camera and cleanup | Implemented explicit codec factories, sink lifecycle, bounded candidates/timeouts, closed-peer rejection | Final C04–C06/C13/C17; camera/microphone quality and devices |
| Audio route, Bluetooth/headsets, audio focus and reconnect | Implemented Android communication-device routing, explicit route selection, focus cleanup and bounded reconnection failure | Final C07–C08/C14–C15; route/device/handoff and cellular interruption |
| Authenticated no-charge TURN | **Blocked**: no tested authenticated relay with an enforced cost cap configured | Current build is STUN-only. Restricted NAT/carriers can fail; no billing or relay service enabled |
| Full Android Telecom/system dialer/headset integration | **Deferred**: current active-call FGS is not Telecom; device/OEM acceptance and a dedicated Telecom integration remain | No claim of system-dialer controls, process-death call restore or guaranteed wakeup |
| Text forwarding/multi-select/copy, reply/reaction/edit/star, long text/quote jump | Implemented; scoped forwarding to existing 1:1 chats, stable IDs, read-more and quote navigation; unit/Compose/SQL | Final M09–M12; batch delete/media forwarding not implemented |
| Enhanced history search | Implemented local query race isolation and per-conversation sender/type/date filters | Final M12; searches downloaded history, not an unbounded server-wide index |
| Image/camera/voice/private signed media, durable upload retries and progress | Implemented protected JPEG/AAC pipeline, fallback quotas, real byte progress, authorized views and cleanup | Final S01–S05/S12; not arbitrary file/video chat attachments |
| Voice record hold/lock/cancel, seek, focus, 1×/1.5×/2× and scroll continuity | Implemented; foreground capture, account/navigation cleanup and deletion/hide revocation even for uncached older Shared Media; AAC/Room/flow/speed tests | Final S03–S04/M14; waveform envelope not implemented |
| Photo/video Stories ≤30s, 720p MP4, carousel/viewer/progress/tap/hold/mute | Implemented; actual decode/export fixture and parser tests, loaded-photo timing, bounded oriented export | Final S06–S08; actual local phone encoders/player and long-video trim |
| Story 24-hour expiry/audience/privacy and upload/mutation/pagination | Implemented; server expiry/contact/block enforcement, local expiry/cursor, cache invalidation and upload stages; live rollback suite | Final S09–S10; scheduled cleanup after expiry is not immediate physical deletion |
| Story seen/unseen backend receipts | **Deferred by explicit prior user decision**: not approved for implementation | No fabricated seen state/counts |
| Posts create/private audience/feed/archive/restore/delete/pagination | Implemented; authenticated feed and signed-media authorization, source-based mutation refresh; live rollback suite | Final S10–S12; physical stable scrolling/thumb acceptance |
| Original Liquid Glass, icon-only navigation and tab/search/empty/error states | Preserved/refined; five-screen synthetic renders, explicit loading recovery and single row semantics | Final A05–A10; visual approval, Gboard/insets, TalkBack |
| Reduced transparency/motion, light/dark and 200% text | Implemented preferences and selected large-text light/dark semantic regressions | Final A06; not an exhaustive accessibility certification of every dialog |
| Timeline/Room/realtime/network performance | Implemented no-op write suppression, stable keys and 1,000-message viewport regression; bounded caches/paging | Physical frame-jank/battery/network baselines still require final device measurement; no speculative broad refactor |
| Build/release unit/lint, APIs 29/35/36, Edge, signatures/16KB and provenance | Automated candidate workflow runs all required jobs; final exact-source run/artifact recorded in #24 | Owner-signed release runtime and installed-version certificate continuity are separate gates |
| Supabase data/migrations | Existing data/history retained; no persistent schema changes in this continuation; live authorization fixtures rolled back and checked absent | Existing migrations preserved; no table reset, destructive migration or broad RLS policy added |
| Public FCM Android fields/private service-account match | Public fields presence reported by CI. Existing recent notify/call-notify HTTP 200 observed | Private service-account/sender/project matching and real two-device delivery unverified; no secret values read/exposed |
| Policy/Data Safety/account deletion/support | In-app disclosure and publication drafts updated for current candidate | **Blocked**: real owner support contact, hosted policy/external deletion route, operational/legal review and Play declarations |
| Signing/crash-ANR/Play rollout/backup and quotas | Debug candidate artifact/signature checks and no-backup policies implemented; operations documents preserved | **Blocked**: owner upload signing/Play access and adoption of monitoring/restore/rollout procedures; no Crashlytics/billing configured |
| One final integrated APK and one test matrix | Calling stays enabled in candidate workflow and is checked in the installed APK via reflection | Deliver only after all included changes' CI and final integrated run pass; use the companion one-round checklist |

## Conditional advanced features — explicit disposition

These items were conditional P2 work after P0 acceptance in the master plan. They are not silently described as delivered. The candidate prioritizes privacy, reliability and verifiable existing media/calling; adding a new subsystem is not represented as a completed one-line UI control.

| Item | Disposition/reason |
|---|---|
| Custom conversation appearance | Curated palettes plus bounded private Home/per-chat photo and MP4 wallpapers, dimming and account cleanup implemented in subsequent MB stages; device playback/performance remains unverified |
| Biometric chat lock/recents shielding | Opt-in Android FLAG_SECURE screenshot/recents shielding implemented; biometric per-chat authentication remains absent/deferred |
| Disappearing messages | Deferred P2; requires server retention/synchronization and explicit recipient-copy limits |
| PDF/documents/more file formats and safe preview | Deferred P2; current Edge allow-list is JPEG/AAC/story MP4. No generic upload bypass, unsafe MIME-only validation or unprotected external viewer was added |
| Multi-select batch share/delete/protected media forwarding | Text select/copy/forward implemented; batch delete and forwarding private media deferred because destination authorization/idempotency need their own end-to-end pipeline |
| Voice waveform | Deferred P2; speeds/seek/actual playback implemented, but no invented/random waveform is shown |
| Automatic link previews | Deferred P2; arbitrary URL fetching requires SSRF/redirect/IP defenses, user consent and bounded network budget |
| Stickers | Existing emoji/reaction picker implemented; personalized sticker upload/store deferred with additional media types |
| Per-chat notifications | Mute/unread plus account-scoped custom Android channels, OS sound settings and local message/sender redaction implemented in subsequent MB stages; OEM/FCM acceptance unverified |
| Export/restore/device migration | Deferred P2; private offline Room and pending outbox should not be exported through an unreviewed backup format; OS backup/transfer exclusions retained |
| Low-data automatic media download | On-demand bounded images/caching implemented; background auto-download policy/settings deferred |
| Smart contact/folder customization | Contact discovery/saved contacts and All/Favorite/Archive/Unread implemented; custom folders remain deferred |
| True multi-device session management | Core account/session isolation exists; a device-list/revoke UI is not implemented and remains deferred pending provider/session design |
| Bengali localization | Bengali message content supported; app-wide translated resources/plurals/accessibility copy deferred P2 |
| Predictive-back/haptics/startup/performance thresholds | Existing back navigation and selected haptics/render/anchor tests retained; formal predictive-back matrix and measured per-device thresholds deferred until device baseline |
| Visual regression/beta rollout | Synthetic screenshots and exact-APK emulator smoke implemented; screenshot-diff baselines and signed feature rollout remain deferred until an accepted visual/device baseline |
| Groups/Communities/call recording | Out of scope by user decision; not added |

## Live provider evidence and limits

On 19 October 2026, rollback-only `authorization.sql`, `message_actions.sql`, `calls_authorization.sql`, `moments_voice.sql` and `content_reporting.sql` all passed against the existing project. Follow-up queries found **0 fixture identities** and **0 public ordinary tables without RLS**. All five existing Edge Functions were ACTIVE. No real message bodies, tokens, credentials or user report contents were read.

Advisors retained the pre-existing leaked-password-protection warning, intentional service-only `private.call_dispatch` no-policy INFO, and unused-index INFOs. No speculative index removal or broader access policy was applied. Password protection remains an owner/platform setting gate; see https://supabase.com/docs/guides/auth/password-security#password-strength-and-leaked-password-protection.

Aggregate push evidence in the last-24-hour review: 52 notify HTTP 200, 17 older HTTP 503 (latest 18 October), and 12 call-notify HTTP 200 (latest 19 October). These are HTTP endpoint outcomes, not proof of device sound/display. Outbox snapshot showed 42 `sent` rows and 22 terminal `failed`/`LEASE_EXHAUSTED` rows, with no pending/processing rows. Old failures were **not replayed**, avoiding a fresh burst of old notifications. The final phone test must use fresh messages/calls. Service-account key material and FCM device tokens were never inspected or printed.

Selected security/authorization tests and provider advisors are not a comprehensive independent penetration test. Free-tier-compatible code and quota controls do not promise unlimited storage/egress, zero usage under every provider policy, every-network calling, or Android background-delivery guarantees. No billing/plan change was made.

## Final release evidence

The final report in #24 records the exact integrated source/version, five completed CI jobs, per-API instrumentation results, lint/unit counts, signature and native alignment, installed calling flag, artifact ID/download link, SHA-256 and material limitations. Artifact retention is shown by GitHub; a preserved installable copy accompanies the delivery where available. The unsigned release AAB is inspection output, not the installable test APK.

Use [ALL_IN_ONE_TWO_PHONE_TEST.md](ALL_IN_ONE_TWO_PHONE_TEST.md) for the single owner acceptance round. Keep #24 open while physical/provider/production gates remain; do not mark its entire original checklist complete based solely on this candidate.

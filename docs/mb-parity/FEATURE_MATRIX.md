# Liquid Chat — two-video and original APK parity inventory

## Owner-directed implementation mode — revised 23 October 2026

- Commit source changes **directly to `main`**, with no pull requests, as requested.
- **Testing and APK delivery are now explicitly authorized**. Run candidate Android unit tests, lint, emulator instrumentation, CI artifact/signature checks and Edge Function source tests; do not quietly suppress failing gates.
- Production signing, external provider credentials, physical two-phone checks and live database migrations remain separately gated. Never claim production approval without passing those.
- Keep all unverified acceptance checkboxes open. A source edit, a green compile or an artifact alone is not a verified feature or full parity release.
- Preserve existing users, data, messaging, calling, media privacy and working navigation. Never insert fake Groups/Communities/Channels or privacy-bypassing controls.
- Interim source-only commits may retain `[skip ci]`; run an actual full current-main CI after integration for the candidate APK.

### Direct-to-main source progress (unverified)

| Commit | Concrete change | Evidence status |
| --- | --- | --- |
| `a3b5ce7` | Search field on Liquid Chat Preferences page | SOURCE ONLY, untested |
| `ba9ee9b` | Working Settings "What's new?" route and Android share chooser for "Invite a friend" | SOURCE ONLY, untested |
| `54b6771` | Search keywords for theme, privacy, HD media, notifications, nav, font and settings categories | SOURCE ONLY, untested |
| `3e444d9` | Actual build version shown in reference Settings footer | SOURCE ONLY, untested |

**Overall scope:** 346 acceptance items across 12 tracks. The items below remain the source-of-truth checklist. Existing implementations are not newly certified. Major dependencies such as actual Communities/Groups/Channels, per-contact server privacy, GIF services, two-phone calling and exact screenshot parity require separate source work or external acceptance. No statement of full completion is made here.

Baseline 2026-10-22; current foundation head before planning: `02f0293f74a60d39602a88dd8f63d16a0c778704`.

**Purpose:** Every observed or user-required option is an independently checkable acceptance item. Sources: both 22 October 2026 owner screen recordings, `docs/MB_REFERENCE_SCREENRECORD_20261022.md`, and `docs/MBWHATSAPP_334_LAYOUT_COVERAGE.md`. Some extended requirements were not opened in recordings; **do not call those video-proven**.

**Status:** unchecked = **not certified**, NOT necessarily absent. Status choices: `SOURCE-PARTIAL`, `TODO`, `BLOCKED-BACKEND`, `EXCLUDED-PRIVACY`, `CI-GREEN`, `DEVICE-VERIFIED`. A feature passes only with real handler + saved value/server semantics + privacy + regression + visual comparison. Each item gets a stable ID, commit/PR reference, evidence and remaining gaps. Never erase or silently close rows.

Safety and scope: preserve Liquid Chat branding, existing Supabase user/data, FCM, free-tier, iOS Liquid Glass option, direct messaging and voice/video calls. Groups, Communities and Channels need genuine backend, not fake buttons. Never restore recipients' deleted content, bypass view-once/receipts security, add unsolicited message flooding, or impersonate WhatsApp E2EE. Record native safe replacement or exclusion explicitly.

**Total 346 independent acceptance items in 12 tracks.**

## Track 00: Build, architecture and design system (P0)

Owner branch: `parity/00-foundation`

- [ ] `P00-001` Kotlin FIR/KAPT compiler memory failure
- [ ] `P00-002` KSP/Room processor compatibility and build
- [ ] `P00-003` Split 250k-character ChatScreens.kt into smaller composables
- [ ] `P00-004` Preserve Material3 and Compose compatibility
- [ ] `P00-005` Stable shared DisplayOptions and theme contracts
- [ ] `P00-006` Supabase schema/data preservation contract
- [ ] `P00-007` Firebase FCM and calling enabled in all variants
- [ ] `P00-008` Debug/release signing identity and provenance
- [ ] `P00-009` No secrets in source, logs, reports or PRs
- [ ] `P00-010` Free-tier infra quotas and dependency inventory
- [ ] `P00-011` Original Liquid Glass theme remains selectable
- [ ] `P00-012` Cross-worktree shared component API boundaries

## Track 01: App navigation and Chats home (P1)

Owner branch: `parity/01-shell-home`

- [ ] `P01-001` Bottom tabs Updates Calls Communities Chats Profile
- [ ] `P01-002` Tab selected line / icon / text
- [ ] `P01-003` Per-account unread badge
- [ ] `P01-004` Chats header and overflow actions
- [ ] `P01-005` Camera and new-chat toolbar
- [ ] `P01-006` All/Unread/Favorites/Groups filters
- [ ] `P01-007` Custom lists create/add-to-list
- [ ] `P01-008` Unread count accuracy and filter persistence
- [ ] `P01-009` Compact avatar, name, snippet, timestamp
- [ ] `P01-010` Delivery/read ticks in chat row
- [ ] `P01-011` Photo/video/voice/document preview labels
- [ ] `P01-012` Unread pin mute archive favorite status
- [ ] `P01-013` Mark read and unread
- [ ] `P01-014` Chat long-press quick preview
- [ ] `P01-015` Home search and clear input
- [ ] `P01-016` Hide floating buttons
- [ ] `P01-017` Reorder tab positions
- [ ] `P01-018` Hide navigation titles
- [ ] `P01-019` Hide navigation icons
- [ ] `P01-020` Adjust nav icon size
- [ ] `P01-021` Adjust nav text size
- [ ] `P01-022` Navigation safe-area padding
- [ ] `P01-023` White/green Reference presentation
- [ ] `P01-024` Unread tab notification badge isolation
- [ ] `P01-025` Show/hide home list dividers
- [ ] `P01-026` Avatar size and no-photo fallback
- [ ] `P01-027` Hide archived-access shortcut
- [ ] `P01-028` Home wallpaper and dim
- [ ] `P01-029` Home story carousel and status preview
- [ ] `P01-030` Online indicator subject to privacy
- [ ] `P01-031` Last-seen indicator subject to privacy
- [ ] `P01-032` Chat row/font size/spacing

## Track 02: Settings, Themes, General and Home customization (P1)

Owner branch: `parity/02-settings-appearance`

- [ ] `P02-001` Main profile Settings dashboard
- [ ] `P02-002` Preferences groups and icon order
- [ ] `P02-003` Liquid Chat Preferences logo
- [ ] `P02-004` Privacy/Notifications entries
- [ ] `P02-005` VIP/Extra Features local safe equivalent
- [ ] `P02-006` Themes page and JSON import/export
- [ ] `P02-007` General configuration page
- [ ] `P02-008` Home Screen customization page
- [ ] `P02-009` Multimedia/HD media page
- [ ] `P02-010` Conversation Screen customization page
- [ ] `P02-011` Animations page
- [ ] `P02-012` Tips Help Updates Support links
- [ ] `P02-013` Light Dark System mode
- [ ] `P02-014` Saved themes and reset
- [ ] `P02-015` Primary accent and swatch/hex picker
- [ ] `P02-016` Secondary color
- [ ] `P02-017` Custom app text color
- [ ] `P02-018` App background color or photo
- [ ] `P02-019` Toolbar/header/footer color
- [ ] `P02-020` Global font scaling
- [ ] `P02-021` App UI density/DPI adjustment
- [ ] `P02-022` Status-bar layout compatibility
- [ ] `P02-023` Blur intensity and disable blur
- [ ] `P02-024` Reduce motion/transparency
- [ ] `P02-025` Random/alternative accent on launch
- [ ] `P02-026` Font family and file import
- [ ] `P02-027` Separate bold title font
- [ ] `P02-028` Launcher icon selector
- [ ] `P02-029` Android shortcut creation
- [ ] `P02-030` Cache size and safe clean-now
- [ ] `P02-031` Wallpaper selection and toggle
- [ ] `P02-032` Glass effect on buttons
- [ ] `P02-033` Bottom tab configuration screens
- [ ] `P02-034` Hide nav text or icons
- [ ] `P02-035` Chat avatar placeholder style
- [ ] `P02-036` Pin/unread badge style
- [ ] `P02-037` Long-press chat preview option
- [ ] `P02-038` Chat title/name/message type size
- [ ] `P02-039` Stories home visibility controls
- [ ] `P02-040` Color picker seekbar modal
- [ ] `P02-041` Touch glyph animation and color
- [ ] `P02-042` Large text / TalkBack contrast

## Track 03: Privacy and Notifications settings (P0)

Owner branch: `parity/03-settings-privacy`

- [ ] `P03-001` Show/hide online and last seen
- [ ] `P03-002` Hide/show read receipts
- [ ] `P03-003` Hide outgoing typing indicator
- [ ] `P03-004` Hide recording indicator
- [ ] `P03-005` Server-side block/unblock enforcement
- [ ] `P03-006` Per-contact privacy exceptions
- [ ] `P03-007` Read receipts after reply/react option
- [ ] `P03-008` Hide forwarded label without lying about origin
- [ ] `P03-009` Status view privacy controls
- [ ] `P03-010` Disable voice/video calls for chosen contacts
- [ ] `P03-011` View-once lifetime respected, never bypassed
- [ ] `P03-012` No restoring others' revoked messages/statuses
- [ ] `P03-013` Safe in-app HTTP/HTTPS browser
- [ ] `P03-014` Link handling and untrusted URL safeguards
- [ ] `P03-015` Disable all notifications per account
- [ ] `P03-016` Auto-hide messages after 5 seconds
- [ ] `P03-017` Quiet alerts during Android DND
- [ ] `P03-018` Quiet alerts in landscape/video
- [ ] `P03-019` Android permission and channel UI
- [ ] `P03-020` Classic/outline notification icon styles
- [ ] `P03-021` Per-contact sound/vibration channels
- [ ] `P03-022` Hidden preview and sender-name privacy
- [ ] `P03-023` FCM dedupe/account switch safety
- [ ] `P03-024` Floating bubble alerts with compliant permission
- [ ] `P03-025` Read notification aloud with opt-in privacy
- [ ] `P03-026` Online-contact notification with server authorization
- [ ] `P03-027` Status notification respecting private audience
- [ ] `P03-028` Notification sender filtering without name leak
- [ ] `P03-029` Custom notification toast sound/timeout
- [ ] `P03-030` No unconsented caller location enrichment
- [ ] `P03-031` Group privacy contingent on genuine group backend
- [ ] `P03-032` Chat lock / task-preview security
- [ ] `P03-033` Disappearing-message retention and propagation

## Track 04: Conversation bubbles, message actions and history (P0)

Owner branch: `parity/04-chat-actions`

- [ ] `P04-001` Conversation back/header/name/avatar
- [ ] `P04-002` Privacy-correct presence status
- [ ] `P04-003` Call/video-call actions in header
- [ ] `P04-004` Date separators
- [ ] `P04-005` Sender-aware compact message shape
- [ ] `P04-006` 12 recorded bubble presets and preview
- [ ] `P04-007` Left/right color and text ink customization
- [ ] `P04-008` Message and emoji font sizes
- [ ] `P04-009` Sent/read/delivered tick UI
- [ ] `P04-010` Media placeholders/loading accuracy
- [ ] `P04-011` Quoted reply bubble and jump action
- [ ] `P04-012` Quoted Status thumbnail display
- [ ] `P04-013` Jump-to-bottom floating control
- [ ] `P04-014` Long-press selected message highlight
- [ ] `P04-015` Seven quick reactions and custom slots
- [ ] `P04-016` React/unreact persistence
- [ ] `P04-017` Floating anchored reaction menu
- [ ] `P04-018` Reply/forward/share/copy/star
- [ ] `P04-019` Server-backed message pin
- [ ] `P04-020` Edit and edit revision history
- [ ] `P04-021` Deleted-message tombstone safeguards
- [ ] `P04-022` Delete for me/everyone
- [ ] `P04-023` Select multiple messages
- [ ] `P04-024` Bulk delete/star/forward
- [ ] `P04-025` Copy sender/time toggle
- [ ] `P04-026` AI reply with private/free opt-in
- [ ] `P04-027` Translate with actual provider or local model
- [ ] `P04-028` Android read-aloud TTS with consent
- [ ] `P04-029` Search sender/date/type
- [ ] `P04-030` Search unread/photos/videos/links
- [ ] `P04-031` Search result jump / highlight
- [ ] `P04-032` No cross-sender message confusion
- [ ] `P04-033` Offline queue retry and dedupe
- [ ] `P04-034` SENDING/FAILED final-state correctness
- [ ] `P04-035` Keyboard stays open after sending
- [ ] `P04-036` Draft/reply revision preservation
- [ ] `P04-037` Menu/sheet transitions and accessibility
- [ ] `P04-038` Wallpaper and dim
- [ ] `P04-039` Alternate reply popup interface
- [ ] `P04-040` Foreground read-receipt behavior

## Track 05: Composer, Emoji/GIF/Sticker picker and voice capture UI (P1)

Owner branch: `parity/05-composer-emoji`

- [ ] `P05-001` Composer rounded shape and send/mic toggle
- [ ] `P05-002` Emoji GIF Sticker tab switch
- [ ] `P05-003` Drag-handle rounded picker
- [ ] `P05-004` Emoji search
- [ ] `P05-005` Recent/suggested emojis
- [ ] `P05-006` Smiley people category
- [ ] `P05-007` Animals and nature category
- [ ] `P05-008` Food category
- [ ] `P05-009` Activities category
- [ ] `P05-010` Travel and places category
- [ ] `P05-011` Objects category
- [ ] `P05-012` Symbols category
- [ ] `P05-013` Flags category
- [ ] `P05-014` Emoji category navigation
- [ ] `P05-015` Emoji variant/skin tones
- [ ] `P05-016` GIF search and grid
- [ ] `P05-017` Trending GIF without secret exfiltration
- [ ] `P05-018` GIF preview and private sending
- [ ] `P05-019` Sticker pack tabs
- [ ] `P05-020` Recent/favorite stickers
- [ ] `P05-021` Create new sticker shortcut
- [ ] `P05-022` Sticker preview and send
- [ ] `P05-023` Import licensed sticker packs
- [ ] `P05-024` Animated sticker preview/send
- [ ] `P05-025` Confirm sticker send
- [ ] `P05-026` Custom sticker size
- [ ] `P05-027` Incognito keyboard flag
- [ ] `P05-028` Keyboard resize/focus/smooth IME
- [ ] `P05-029` Press-and-hold microphone
- [ ] `P05-030` Slide left to cancel
- [ ] `P05-031` Slide up to lock
- [ ] `P05-032` Pause/resume voice recording
- [ ] `P05-033` Recording timer and waveform
- [ ] `P05-034` Trash/back-to-discard recording
- [ ] `P05-035` Preview before voice send
- [ ] `P05-036` Send arrow and 120-second duration
- [ ] `P05-037` Permission-denied/error state
- [ ] `P05-038` Recorder cancels on call/logout/nav
- [ ] `P05-039` Voice playback speed and seeking
- [ ] `P05-040` Gestures do not block attachment taps

## Track 06: Media, stickers, documents and editing (P1)

Owner branch: `parity/06-media-editor`

- [ ] `P06-001` Gallery and camera picker
- [ ] `P06-002` 10-photo selection
- [ ] `P06-003` Album reorder/remove/caption
- [ ] `P06-004` Real photo HD conversion
- [ ] `P06-005` Video 480p/720p encoding cap
- [ ] `P06-006` Video upload and preview
- [ ] `P06-007` Crop presets 1:1/4:5/16:9
- [ ] `P06-008` Freeform touch crop and bounds
- [ ] `P06-009` Rotate and flip
- [ ] `P06-010` On-image text caption overlay
- [ ] `P06-011` Freehand doodle and undo redo
- [ ] `P06-012` Black-white/filter presets
- [ ] `P06-013` Editor apply/cancel cleanup
- [ ] `P06-014` 512x512 transparent sticker PNG
- [ ] `P06-015` Border-only white background removal
- [ ] `P06-016` Sticker caption text outline
- [ ] `P06-017` Animated stickers with licensed formats
- [ ] `P06-018` Saved stickers and import/export
- [ ] `P06-019` PDF/document attachment
- [ ] `P06-020` Multi-document selection
- [ ] `P06-021` Document scan / merge images to PDF
- [ ] `P06-022` Private media viewer/share/save
- [ ] `P06-023` Download progress/cancel
- [ ] `P06-024` Story photo/video editor
- [ ] `P06-025` Deleted/revoked media guard
- [ ] `P06-026` Signed private URLs and quota
- [ ] `P06-027` Offline upload/retry idempotency
- [ ] `P06-028` Temporary-file/cache deletion and isolation
- [ ] `P06-029` No public media keys
- [ ] `P06-030` Reference media popup consistency
- [ ] `P06-031` Stop avatar/media reload jitter

## Track 07: Voice/video calling and call history (P0)

Owner branch: `parity/07-calls-audio`

- [ ] `P07-001` Voice recording stability
- [ ] `P07-002` Audio focus vs active call conflicts
- [ ] `P07-003` Voice playback player lifecycle
- [ ] `P07-004` Outgoing ringing call screen
- [ ] `P07-005` Incoming answer and decline UI
- [ ] `P07-006` Connecting active ended call UI
- [ ] `P07-007` Speaker earpiece wired Bluetooth routes
- [ ] `P07-008` Mute microphone toggle
- [ ] `P07-009` Video on/off and camera switch
- [ ] `P07-010` Video minimize / Picture-in-Picture
- [ ] `P07-011` Call history refresh and local cache
- [ ] `P07-012` Missed/incoming/outgoing labels
- [ ] `P07-013` Redial call
- [ ] `P07-014` Call scheduling with real reminder backend
- [ ] `P07-015` Keypad for app identity contacts, not PSTN
- [ ] `P07-016` Favorites calls
- [ ] `P07-017` Android 10-16 runtime permissions
- [ ] `P07-018` Network loss/recovery behavior
- [ ] `P07-019` FCM invite TTL/decline/busy verification
- [ ] `P07-020` WebRTC offer/answer/candidates isolation
- [ ] `P07-021` Free-tier TURN limitation disclosure
- [ ] `P07-022` Two-phone audio/video route checklist
- [ ] `P07-023` Calls enabled in final build

## Track 08: Stories, Posts, Updates, Groups, Channels and Communities (P1)

Owner branch: `parity/08-moments-social`

- [ ] `P08-001` Updates screen and story carousel
- [ ] `P08-002` Add photo/video status
- [ ] `P08-003` 30-second 720p story editing
- [ ] `P08-004` Story upload progress/failure retry
- [ ] `P08-005` 24-hour expiry
- [ ] `P08-006` Viewed/unviewed privacy
- [ ] `P08-007` Tap next/back and hold pause
- [ ] `P08-008` Mute/unmute and start with sound
- [ ] `P08-009` Blur thumbnail/story transition
- [ ] `P08-010` Show/hide status reactions
- [ ] `P08-011` Disable auto jump
- [ ] `P08-012` Posts create/edit/delete
- [ ] `P08-013` Channel browse/follow/unfollow backend
- [ ] `P08-014` Create Channel backend
- [ ] `P08-015` Communities landing/create backend
- [ ] `P08-016` Community announcement and moderation
- [ ] `P08-017` Group chats roles/membership backend
- [ ] `P08-018` Groups tab/common groups
- [ ] `P08-019` No cloned WhatsApp service endpoints/content
- [ ] `P08-020` Group/channel RLS rate limits quota
- [ ] `P08-021` Existing one-to-one messaging and calls retained

## Track 09: Profiles, contacts, global search and accessibility (P1)

Owner branch: `parity/09-profile-contact-search`

- [ ] `P09-001` Profile photo/name/username/bio
- [ ] `P09-002` Profile edit/remove photo
- [ ] `P09-003` Profile toolbar quick actions
- [ ] `P09-004` Contact large photo/name/username
- [ ] `P09-005` Contact voice/video/search
- [ ] `P09-006` Contact notifications/media visibility
- [ ] `P09-007` Per-contact privacy/call disable
- [ ] `P09-008` Favorites / add to lists
- [ ] `P09-009` Save contact and block
- [ ] `P09-010` Report contact
- [ ] `P09-011` Clear chat confirmation
- [ ] `P09-012` Disappearing messages / chat lock
- [ ] `P09-013` Shared media and starred items
- [ ] `P09-014` Groups in common if backend available
- [ ] `P09-015` Global full-screen search / keyboard focus
- [ ] `P09-016` Global unread/photos/videos/links chips
- [ ] `P09-017` Search sender/date/context
- [ ] `P09-018` Jump to matching result
- [ ] `P09-019` Contact avatar cache / placeholders
- [ ] `P09-020` Account change local data hygiene
- [ ] `P09-021` TalkBack and 48dp targets
- [ ] `P09-022` 200-percent font and landscape
- [ ] `P09-023` User-selected UI language/localization
- [ ] `P09-024` No false E2EE claim

## Track 10: Supabase, FCM, security and sync (P0)

Owner branch: `parity/10-backend-security`

- [ ] `P10-001` Participant-only Supabase RLS
- [ ] `P10-002` Session restore and account isolation
- [ ] `P10-003` Auth login/logout/account deletion
- [ ] `P10-004` Reversible migrations preserving existing rows
- [ ] `P10-005` Messaging/block/receipts RPC validation
- [ ] `P10-006` WebRTC signaling credentials and expiry
- [ ] `P10-007` Report and moderation rights
- [ ] `P10-008` Private image/video/PDF media bucket
- [ ] `P10-009` Secure Firebase service account
- [ ] `P10-010` FCM token registration/cleanup per account
- [ ] `P10-011` Duplicate notification suppression
- [ ] `P10-012` Presence heartbeat privacy
- [ ] `P10-013` Two-way block and unblocking
- [ ] `P10-014` Deleted message never returns via cache
- [ ] `P10-015` Revoked media hidden in search/quotes
- [ ] `P10-016` Offline queue/retry idempotency
- [ ] `P10-017` Server-enforced privacy preferences
- [ ] `P10-018` Story TTL and view-state access
- [ ] `P10-019` Per-contact privacy schema/RLS
- [ ] `P10-020` Group and Channel schema feasibility
- [ ] `P10-021` Free-tier limits/storage/cost
- [ ] `P10-022` Release signing and key continuity
- [ ] `P10-023` No foreign WhatsApp endpoints
- [ ] `P10-024` External assets copyright/license audit

## Track 11: Integration QA, visual comparison and final APK (P0)

Owner branch: `parity/11-qa-release`

- [ ] `P11-001` Every video option has stable inventory ID
- [ ] `P11-002` Never silently remove unresolved option
- [ ] `P11-003` Each PR cites inventory IDs
- [ ] `P11-004` Review all source strings and resources
- [ ] `P11-005` Kotlin/Compose build using KSP
- [ ] `P11-006` Debug/release tests and lint
- [ ] `P11-007` API29/35/36 Android instrumentation
- [ ] `P11-008` Same-state screenshot baselines
- [ ] `P11-009` Reference visual alignment comparisons
- [ ] `P11-010` Theme/large-text/landscape QA
- [ ] `P11-011` RLS authorized and unauthorized checks
- [ ] `P11-012` Media/story/notification regressions
- [ ] `P11-013` Send/delete/block/offline acceptance
- [ ] `P11-014` Two real phones voice/video/FCM checks
- [ ] `P11-015` No repeated manual APK installs
- [ ] `P11-016` One final calling-enabled merged-main APK
- [ ] `P11-017` Signature/checksum/artifact provenance
- [ ] `P11-018` Supabase preservation and zero-cost guard
- [ ] `P11-019` No false 100-percent or production claim
- [ ] `P11-020` Explicit blocked/deferred exclusions
- [ ] `P11-021` Mandatory CI failures remain blocking
- [ ] `P11-022` Merge conflict and dependency evidence
- [ ] `P11-023` Version bump and release notes
- [ ] `P11-024` Final implementation and limitation report

## Closure rules

- Each row must remain tracked until implemented+verified or explicitly classified `BLOCKED-BACKEND`/`EXCLUDED-PRIVACY` with rationale and owner-visible limitation.
- An option shown in the UI without a working callback cannot be marked complete.
- Add new discoveries as a **new** stable ID within the corresponding section; do not renumber old IDs.
- User wants one final manual two-phone test, not repeated interim APK installations. Do not claim 100% parity without actual matched-state screenshots and device evidence.

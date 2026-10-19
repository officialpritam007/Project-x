# Twenty-screen UI audit - 19 October 2026

Source: owner-uploaded `PDF Gallery_20261019_142023.pdf`, twenty pages. Each embedded screenshot was extracted and reviewed at readable resolution. Baseline main: `fda67d90d5481ee43cc54e59b9a9392bf3b2825b` (PR63). This pass completes residual presentation issues; it does not claim that screenshots prove runtime bugs or physical call quality.

| Page | Screen / observed issue | Implementation and acceptance |
| --- | --- | --- |
| 1 | Active video: oversized tray, scattered controls and camera preview | PR56 already provides compact five-action strip and accessible audio route action; PR63 glass styling retained. Real camera, rotation, Bluetooth and carrier acceptance remain physical gates. |
| 2 | Audio output: plain list in a small dialog | Main already uses selectable device rows in a bottom sheet, current-route marker and device-specific icons. No audio-engine change in this pass. |
| 3 | Account: small avatar, empty vertical gaps and scattered actions | PR63 responsive 72dp avatar, profile fields, full-width edit/photo controls. This pass reserves floating-navigation and system-bar space in Settings subpages. |
| 4 | Chat Settings: touching option chips and cramped controls | Separate chips by 8dp; keep horizontal scrolling, font scaling and disabled animation presets under reduced motion. |
| 5 | Privacy: explanatory card, generic grouping | Preserve participant/privacy explanation and actual persisted switches. Bottom content stays clear of floating navigation. No E2EE claim. |
| 6 | Notifications: previews toggle only, misleading scope | PR60 already adds actual Android permission/channel state and lifecycle refresh; preserve that, plus safe bottom spacing. |
| 7 | Storage: dense technical paragraphs, unsafe cache expectations | PR58 already separates storage overview, pending messages, limits and confirmed local image-cache clearing. No outbox deletion or quota changes. |
| 8 | Power Saving repeats appearance controls | PR54 already gives dedicated reduced-motion, media-download and Android-battery controls. Preserve regression assertions. |
| 9 | Settings home: long dense flat list and inaccurate subtitles | PR63 grouped account hero/preferences/more, icon tiles, readable subtitles. Planned capabilities remain accurately labelled. |
| 10 | Language dialog: default lavender surface | PR63 frosted dialog retained. English-only availability is explicit; no fake language switch. |
| 11 | Inbox: oversized sparse story cards, flat rows, redundant controls | PR63 glass inbox and existing story carousel retained; existing filter chips remain the single filter UI. Overflow receives matching rounded light/dark styling. Camera shortcut still opens contact selection. |
| 12 | Conversation: glass consistency, voice controls and small status | PR63 directional bubbles/header retained with existing content sizing and playback controls. Physical font/IME/audio acceptance remains in the consolidated checklist. No fabricated waveform. |
| 13 | Conversation overflow: very long popup, Block and Unblock together | Existing owner-scoped exclusive block state retained. Replace long popup with scrollable icon action sheet; group navigation, chat preferences and safety actions. Delete remains a separate confirmed action. |
| 14 | Message actions: long flat list and no danger hierarchy | PR59/63 already group actions, reactions and safety, with scrollable content. Keep exact message callbacks/deletion guards. |
| 15 | Emoji: clipped fourth category, unstyled dialog | PR57 already provides two-row categories, search and temporary recents. This pass applies the shared Liquid dialog used elsewhere, retaining all callbacks. |
| 16 | Attach: tiny text actions surrounded by blank space | Replace with full-width Gallery and Camera icon rows in a scrollable rounded sheet. Keep existing picker, camera ownership and failure cleanup paths. |
| 17 | Recorder: keyboard remains visible under the recording popup | PR55 already clears focus/hides IME only after capture starts, supports Back-to-discard and cancels on background. Preserve gestures and drafts; physical Gboard/mic acceptance remains. |
| 18 | Contact details: small modal, cramped tabs, narrow media list and empty text | New tall rounded contact sheet with hero, username/save action, horizontally scrollable tabs, adaptive square photo grid and full-width Voice/Starred cards. Hero/results share one lazy scroll area for landscape/200% text. Accessible close, explanatory empty states, visible retry and paging. Preserve signed-media viewer/player and pagination/deletion/auth guards. Existing relevant-only refresh/deletion logic is unchanged; tabs get separate viewports. |
| 19 | Inbox overflow duplicates All/Favorite/Archive/Unread chips | PR54 already removes duplication. Keep Refresh conversations; apply consistent rounded light/dark menu styling. |
| 20 | Chat long-press: plain list, weak hierarchy, delete indistinct | Full-width labelled icon rows in a scrollable sheet, current-state labels, divider and error-colour delete. Existing confirmation remains mandatory. |

## Verification

- Local: 33 host regression tests passed; `git diff --check` passed.
- Local Android execution unavailable: no Android SDK or cached Gradle distribution; wrapper bootstrap returned `Network is unreachable`. GitHub candidate CI is the Android build/lint/JVM/emulator authority for this patch.
- Added Compose coverage for contact save/close/tab/empty states at 200% font, dark photo grid with 36 fixtures and retry, and a long action sheet with a reachable destructive callback. Tests export actual composable screenshots to the existing CI screenshot directory.
- Owner explicitly authorized publication after the earlier approval-review block. The connected GitHub integration published PR64 after confirming the expected repository and reviewing the UI-only diff; the CLI had no push credentials. The current candidate is rc5 (12).
- Require all existing five candidate jobs (build, Edge, API29, API35, API36), with no disabled tests, before merge. Require another exact merged-main run before distributing a new APK.
- Calling-enabled candidate configuration remains unchanged. No Supabase schema, production data, RLS, paid service, dependency or credential changes.

## Remaining acceptance

One final two-phone pass covers real media, Gboard/gestures, route devices, notifications and system insets. The source screenshots cannot establish hardware capture/playback quality or network reliability. Existing conditional features (documents, waveform, true Story view receipts, custom sounds, device management) retain their prior statuses; this UI request does not silently implement backend features or fabricate states.

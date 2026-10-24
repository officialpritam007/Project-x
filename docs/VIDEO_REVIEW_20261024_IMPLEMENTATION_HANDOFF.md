# 24 October 2026 — Screen-recording implementation handoff

## Verification gate

**Source implementation only.** The owner requested that no build, lint,
unit test, instrumentation test, CI verification or acceptance check be
started until a separate explicit instruction. All items below are unverified
at runtime. No APK / release / production status is claimed.

## Implemented source areas (pending verification)

| Area | Source | Intended behavior |
| --- | --- | --- |
| Popup/sheet legibility | `ui/GlassComponents.kt`, `ui/SharedMedia.kt` | Active theme surface, near-opaque dialog/sheet background, drag handle and larger close targets |
| Protected still images | `ui/ProtectedImage.kt`, `ui/SharedImagePreview.kt` | Visible loading state without losing reserved media aspect ratio; retry preserved |
| Contact/profile identity | `ui/ChatListRows.kt`, `ui/ProfileSharedPanels.kt` | Initials behind loading avatars; theme-aware fallback color; no forced blank image |
| Story private feeds | `ui/MomentModels.kt`, `ui/Moments.kt` | Own/contacts filtering at view and playback queue while retaining source pagination |
| Private video thumbnails | `ui/ProtectedVideoThumbnail.kt`, `ui/MomentsStoryStrip.kt`, `ui/Moments.kt`, `ui/ChatVideoAttachment.kt`, `app/build.gradle.kts` | Coil video frame extraction using ephemeral signed media URL, memory-only cache scoped to account/session, fallback and play affordance |
| Video playback loading | `ui/StoryVideoPlayer.kt`, `ui/MomentStoryViewer.kt` | Poster during URL fetch/initial buffering, spinner/retry, existing player cleanup |
| Chat media geometry | `ui/ChatVideoAttachment.kt`, `ui/ConversationParts.kt` | Stable preview/playback height, do not jump to latest while reading long newest media |
| Navigation | `ui/GlassNavigationBar.kt`, `ui/MbNavigationVisibility.kt`, `ui/ProfileHome.kt` | Adaptive bottom bar and overflow tab heights, profile bottom padding grows with navigation |
| Settings | `ui/SettingsAppearanceControls.kt` | Navigation icon/label sliders preview immediately, persist value at gesture end |
| UI polish/accessibility | `ui/CompactMessageActionMenu.kt`, `ui/ConversationScreen.kt`, `ui/ProfileHome.kt` | 48dp reactions, last-seen wrap, copy feedback, legible story play overlays |

## Regression tests prepared but NOT executed

- `app/src/test/java/com/pritam/liquidchat/ui/GlassPopupPaletteTest.kt`
- `app/src/test/java/com/pritam/liquidchat/ui/MomentAudienceTest.kt`
- `app/src/test/java/com/pritam/liquidchat/ui/AdaptiveNavigationHeightTest.kt`
- `app/src/test/java/com/pritam/liquidchat/ui/ChatAutoFollowTest.kt`
- Existing `app/src/androidTest/java/com/pritam/liquidchat/ui/ProtectedImageCacheTest.kt`
- Existing `app/src/androidTest/java/com/pritam/liquidchat/ui/ContactMediaUiTest.kt`
- Existing `app/src/androidTest/java/com/pritam/liquidchat/ui/MbNavigationBarUiTest.kt`
- Existing `app/src/androidTest/java/com/pritam/liquidchat/ui/CompactMessageActionMenuTest.kt`

## ONE acceptance pass — ONLY after explicit owner instruction

1. Establish exact source commit SHA, compiler/JDK/SDK availability and workflow scope.
2. Run debug unit tests, Android lint, debug assembly; treat any build failure as a blocker.
3. Run targeted Android UI tests on supported emulator, including the above pre-existing suites; do not assume all tests are available merely from filenames.
4. Test private signed video frame extraction on API 29 and API 35, including corrupt/expired/revoked media, account switch and process recreation. Ensure no permanent public image URL or disk thumbnail survives revocation.
5. On a real device, test slow 3G/offline/reconnect, media cache, download retry, first-frame buffering, play/pause/close, network data cost, background/foreground and call interruption.
6. Rapidly open, drag, scroll and dismiss message-actions/contact-media sheets. Verify visual contrast in reference/light/dark/custom/large font modes, without leaking data between accounts.
7. Test bottom tab order/visibility, icon/label sizes 20–50dp / 8–20sp, process restart, compact width, IME and safe-area.
8. Test message history with tall images/videos: load older, new incoming while reading, typing, jump-to-latest, reply focus, screen change and restoration.
9. Validate message actions, Contacts/Calls/Profile empty states, story audience filtering, multi-story navigation and expiration using two accounts.
10. Keep provider/backend acceptance and signed release gates separate. Do not mark features verified or distribute an APK unless corresponding evidence exists.

## Remaining acceptance-only dependencies

- Build/test/device result, crash/performance log, playback codec/device support.
- Backend integration and two-account authorization/deletion behavior.
- Real network profiling for private video thumbnails: decoding may require
  enough MP4 data to reach the first frame; measure data use and latency.
- Final installable APK and signing provenance.

**Do not run this verification plan automatically.**

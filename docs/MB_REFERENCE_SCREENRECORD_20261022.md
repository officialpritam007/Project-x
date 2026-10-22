# Liquid Chat — owner-provided MBWhatsApp screen recording audit

Reference: 2026-10-22, 162.8-second portrait screen recording (1080 × 2392). This file contains private profile/message imagery and must NOT be committed; this document lists UI affordances only.

## Recorded navigation and differences

| Recorded time | What is actually visible | Liquid Chat action/status |
| --- | --- | --- |
| 00:00–00:11 | Profile/Settings landing: profile photo and name, grouped lists, persistent five-icon bottom bar | Profile and bottom navigation exist; exact profile layout still needs runtime comparison |
| 00:12–00:25 | Reference preferences landing with logo, grouped Privacy/Notify, VIP, Themes/General/Home/Multimedia/Conversation/Animations, support | Refactor reference Settings landing in `MbPreferencesLanding`; use Liquid Chat branding, real routes; no foreign VIP paywall |
| 00:27–00:33 | General configuration: text size, screen DPI, blur, theme colors, background/wallpaper, local fonts | Existing appearance/theme settings provide many counterparts; exact order/preview and settings visual styling still incomplete |
| 00:33–00:45 | Home Screen: hide floating buttons, nav order, hide titles/icons, icon/text size, liquid glass switches, chat dividers | Existing `DisplayOptions` options are wired; precise per-control placement/gestures must be verified |
| 00:51–00:53 | Multimedia: HD quality images/videos switches | Add dedicated reference media page wired to actual `photoQuality` and `videoQuality`; don't invent a server setting |
| 00:54–01:15 | Conversation Screen: bubble style chooser, size, sticker options, bubble colors, link color, double tap reaction, custom pop-up reactions, send confirmation | Existing conversation options and reaction logic; exact modal art, options and timing not certified |
| 01:18–01:30 | Privacy: hide forwarded tags/status, anti-delete, view once, contacts/group privacy, links | Do not port recipient-bypassing anti-delete/view-once bypass. Existing real presence/receipts/block & server privacy are retained. Group privacy requires unsupported server group model |
| 01:30–01:39 | Notify: disable notifications, notification toast, quiet mode, custom alerts | Existing notification channels/icons/push preferences; several settings are not yet matched or verified |
| 01:39–02:15 | Chats: filters, compact rows; message menu quick reactions and reply/forward/copy/delete/pin/star; media tile states; Calls history | Implement as native conversation/media/calling controls only; visual matching depends on real same-state screenshots |
| 02:15–02:30 | Community + status/channel screens | Existing Liquid Chat scope differs; do not invent community/channel backend or copy WhatsApp's content |
| 02:30–02:42 | Contact info privacy/actions, real call/video actions | Existing private 1:1 contact details and voice/video calls kept; verify end-to-end with two devices |

## Quality gate

The video is evidence for reference behavior only, not proof of feature parity. The owner's original APK is not the new app. No exact pixel similarity, build success, reliability claim, or "100% working" label is valid until identical-state reference/new screenshots and actual Android CI/2-device tests are available.

The last checked `main` Android builds #487, #489, and #490 failed in Kotlin FIR/KAPT stubs with JVM heap exhaustion. Merely changing the settings menu cannot fix this. Existing verified build #486 predates these new changes.

## Build pipeline root-cause response — KSP migration

The 2026-10-22 build logs for Android runs #487/#489/#490 show a `java.lang.OutOfMemoryError` during `:app:kaptGenerateStubsDebugKotlin` even with 5 GiB. Room 2.7.1 is the only `kapt` processor. The implementation now migrates this dependency to Kotlin Symbol Processing (`com.google.devtools.ksp` 2.1.20-2.0.1), which is compatible with Kotlin 2.1.20 and avoids generating Java stubs for the entire Compose source tree. This **does not establish a successful build**; actual GitHub Actions verification remains required. The previous successful #486 APK predates these changes.

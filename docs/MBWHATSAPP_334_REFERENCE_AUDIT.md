# MBWhatsApp 3.3.4 -> Liquid Chat: APK reference and implementation register

Reference inspected: user-provided `MBWhatsApp_3.3.4_by_stefanoYG.apk` (119 MB). Read only, no installation or execution. The APK is **compiled WhatsApp-derived Android software**, not a Kotlin/Jetpack Compose source tree. Preserve original MBWhatsApp screen hierarchy and interaction wording where implementable, but rebuild Liquid Chat backend-dependent behavior natively rather than embedding or replacing its Supabase/Auth/WebRTC stack. A matching layout without functioning state is **not** accepted.

## Reference evidence (static APK audit)

- Assets: `assets/mbmods/mbchangelog.txt`, including the current 3.3.4 fixes and historical option names. Historical changelog mentions establish intended features, **not** proof they work in this binary.
- Binary Android XML parsed as a real hierarchy (not only filenames): `mb_preferences.xml` has 46 start nodes (FrameLayout root, hero icon, scrollable grouped settings rows and a bottom `MBNavigationTab`); `mb_general_settings.xml` has 60 nodes (grouped `MBListBar`, `MBSwitch`, `MBSeekBar`, `MBColorPicker` controls); `mb_convo_settings.xml` has 97 nodes (collapsible controls, per-chat color pickers, switches/sliders). Screen structures can be ported to Compose without inserting a WhatsApp package.
- Actual binary XML layouts: `res/layout-v1/mb_preferences.xml`, `mb_general_settings.xml`, `mb_home_settings.xml`, `mb_convo_settings.xml`, `mb_privacy_settings.xml`, `mb_notify_settings.xml`, `mb_themes_settings.xml`, `mb_bottom_iosv2.xml`, `mb_home_toolbar.xml`, `mb_colorpicker_layout.xml`, `mb_seekbar_layout.xml`, `mb_animations.xml`, `mb_pop_view_layout.xml`.
- Distinct mod assets: `assets/mbmods/fonts/Font_iOS*.ttf`, `assets/mbmods/emoji/`, multiple `mb_noti_*.png`, and custom Android resources. Do not blindly copy application package metadata, signing, obfuscated WhatsApp code or proprietary endpoints.
- Version 3.3.4 specifically mentions sticker export white-border option and earlier status / once-view download fixes. These cannot be assumed relevant until the equivalent Liquid Chat feature exists.

## Exact-feature parity matrix (not all complete)

| Reference area | APK evidence | Current Liquid Chat status / implementation gate |
| --- | --- | --- |
| MB Preferences + General Settings | `mb_preferences`, `mb_general_settings`, changelog | **Partial**. Appearance and accessibility exist. New live persistent Liquid Glass blur intensity control added on this branch. Reference screen-by-screen geometry and complete options **not yet ported**. |
| Home Screen + toolbar + tab order/icons | `mb_home_settings`, `mb_home_toolbar`, `mb_bottom_iosv2`; changelog hide/reorder tabs, title/icon sizes | **Partial**: Liquid Chat has inbox filters, story carousel and navigation; pixel/layout parity, tab customization and home wallpaper/video **not verified**. Keep calls/profile destinations reachable. |
| Conversation screen | `mb_convo_settings`, changelog bubble style, fonts, emoji size, link colors, chat preview, headers | **Partial**: modern message actions, input, voice, photos and per-chat wallpaper already exist. Font/color/size picker and exact bubble/layout gestures remain. |
| Privacy, Ghost mode, hide ticks | `mb_privacy_settings`, changelog | **Partial**: real presence/read-receipt server settings exist. Combined Ghost Mode now toggles server-backed presence/read receipts; per-contact exceptions and alternative tick suppression **not implemented**; never show a working-looking toggle without matching server guarantees. |
| Notifications | `mb_notify_settings`, changelog icon/toast/filters | **Partial**: FCM, permission/channel status and existing deduplication are present. Custom notification icons/sounds and contact-specific alerts remain. |
| Themes and presets | `mb_themes_settings`, color pickers, own font assets | **Partial**: six persisted primary accent palettes work across Material3; accurate import/export, font/theme packs and screenshot parity still require verification. |
| Scheduled messages / editing diff | v2.6.1 changelog | **Partial**: Room v8 + persistent WorkManager text scheduling, cancel list, due-time idempotent outbox transfer, account/logout/delete protections. Actual device scheduling precision remains unverified; edited-body history and cross-device schedules remain unimplemented. |
| Read-more, message preview and mark read | changelog v2.6, v2.8 | **Partial**: read-more and mark-read exist; long-press preview layout not verified. |
| Story/status editor, playback/download | changelog, status reference resources | **Partial**: 24h stories and video story tooling exist; WhatsApp-specific downloads / once-view bypass must not be copied as a backend promise. |
| Sticker maker, custom emoji, reactions | sticker layouts, assets, changelog | **Partial**: emoji picker/reactions present, full sticker pipeline not complete. |
| Voice/video calling | base app + Liquid Chat existing calling | **Partial**: must retain calling-enabled screens and WebRTC; physical two-phone media test and TURN are still open. |
| Groups, Meta AI, WhatsApp channels, account linking | historical WhatsApp-mod changelog | **Different platform scope**: Liquid Chat uses its own accounts/data and currently targets one-to-one conversations. Cannot claim proprietary WhatsApp capabilities just by copying UI. |

## Release gates and implementation policy

1. Treat the APK as a UI and functionality reference. Map every reference screen/action to Compose screen, actual state owner, database/API and device test.
2. No existing Supabase data deletion, credential copying, E2EE claim or invisible bypass of authorization.
3. Preserve voice and video calling in the final candidate.
4. Implement in reviewable branches, then run debug/release JVM, lint, API29/API35/API36 instrumented tests, Supabase/Edge contracts and exact merged-main checks.
5. Mark every feature **implemented**, **blocked**, **deferred** or **unverified**. Static APK resources and changelog are not runtime acceptance.
6. Ship a single combined test APK only after all feasible changes pass CI. The owner tests one consolidated two-phone checklist.

## Branch 1 implementation

- `DisplayOptions.blurIntensity`: 0-100 range; 55 default preserves existing 22 dp blur.
- `AppViewModel.setDisplay`: clamps and persists `blurIntensity` to device preferences, survives restart.
- `GlassSurface`: uses preference directly for Haze blur strength across live glass surfaces.
- `SettingsScreen`: working slider under Chat Settings; disabled when reduced transparency is active.
- This **does not** finish exact MB Preferences visuals, full option parity or a pixel-perfect port.

## Branch 1 continuation — functional Conversation Screen customization

- Settings home exposes **MB Preferences** with **General Settings** and **Conversation Screen** subpages. Both navigate to real settings, not placeholder menus.
- Conversation Screen changes message bubble geometry (Tail/Rounded/Compact), own/contact message color palette (Original/Green/Lavender/Rose/Night), and message text size (14–22sp), with an in-settings live preview.
- Selected values persist through Android SharedPreferences and are validated/clamped on read and write. Existing installs default to their previous Tail/Original/16sp appearance; no Supabase data, call buttons or message delivery code changes.
- The live message composable consumes these settings; unit regressions check legacy defaults and mapping. **Reference pixel parity is not yet proven.** These are working counterpart controls, not an assertion that the list exhausts the MBWhatsApp binary.
- Remaining high-priority gaps: reference screen screenshots/raster/layout matching, per-chat themes & wallpaper assets, theme import/export, home toolbar and bottom-tab customization, scheduled outbox, edited history, notification policies, and backend-enforced privacy semantics. All need distinct verification; an APK changelog alone is not a working implementation.


## Branch 1 continuation — functional Home Screen customization

- The MB Preferences hierarchy now includes **General Settings**, **Home Screen**, and **Conversation Screen**. Settings Home links directly to it.
- Home Screen options: tab labels show/hide; navigation icon size 18–30dp; label size 9–15sp; branded/home title size 20–30sp. Live GlassNavigation and GlassHeader read the same persisted display state.
- Defaults retain the previous icon-only 24dp bar, 25sp title, and current route order; five tabs and calling destination remain accessible. No navigation or Supabase behavior was removed.
- All settings are presentation controls only and cannot be treated as the full home-screen parity target. MBWhatsApp's screenshot-level toolbar/tab assets, ordering, and animated states are still unverified.

## Branch 1 continuation — real privacy and more home appearance controls

- Server-backed **Ghost Mode** available both in Privacy Settings and Chats overflow. It disables **show_presence** and **read_receipts** together using the existing authenticated Supabase `save_settings` function. Toggling off restores both to true. The switch only appears enabled when full settings have been retrieved; account change clears the in-memory privacy snapshot to prevent cross-account state leakage. Notification preview preferences remain unchanged. Old read receipts cannot be withdrawn.
- Independent home conversation-name 13–20sp and preview 12–18sp sizes are applied in real inbox rows, live and persisted.
- Main accent color presets Cyan/Blue/Green/Purple/Rose/Amber change the Material3 primary palette and remain local to the device. Original Cyan color scheme is the backward-compatible default.
- Home header can show the authenticated user's profile name instead of the branded title. Archive filter can be hidden without deleting archived messages and restored through MB Preferences.
- Unit tests cover Ghost Mode flags and legacy display defaults. This still requires exact-commit build, emulator and physical-acceptance validation.
- **Known incomplete reference work:** exact resource/pixel replication, theme/font import/export, launcher icons, scheduled messages, document scanning, stickers, status downloads, notification-specific policies and native system-level call quality checks. No unavailable feature has been labelled production-ready.

## Branch 1 continuation — scheduled text messages (candidate only)

- New Room v8 `scheduled_texts` table and `Migration7To8`, additive with no Supabase schema mutation.
- Android WorkManager keeps a scheduled text separate from the pending text outbox until due. When due, an account-scoped transaction moves the same UUID into the existing idempotent message queue. Standard network retry and delivery status then apply.
- Conversation menu supports **Schedule message**, native date/time picker, scheduled text list and cancellation. Allowed time: one minute to 180 days ahead; text up to 4000 characters. Draft consumption is version-guarded and leaves newer drafts untouched.
- WorkManager schedule registration is restored when an account becomes foreground. Sign-out and account erasure clear local schedules; deleting a conversation cancels its future texts. The worker checks the authenticated account before dispatch and never uses another account's session.
- Database instrumentation covers non-dispatch-before-due, user isolation and safe deletion. Room v1→v8 migration fixture preserves older pending messages and drafts.
- **Limitations:** schedules are stored *locally on this device*, not synchronized to other devices or the Supabase server. Android power/battery policies can delay exact dispatch. Clearing app data, uninstalling or signing out removes local schedules. No scheduled media, recurring/group messages, or server-time SLA. Real reboot/Doze/device acceptance remains required; this is not yet a production scheduling guarantee.
- Calling remains enabled by existing workflow configuration. Candidate version 1.0.0-rc6-mb-preview (13); exact-head CI plus exact merged-main checks are required before release.

## Stage 3 — locally exportable and importable theme JSON

- MB Preferences > Themes now uses Android's document picker to create a `LiquidChat-Theme.json` file and import one. Roundtrip carries all supported Liquid Chat display settings; no account, session, message, file/media link or secret data is exported.
- Imported files are limited to 32 KiB, versioned, type-checked and clamped. Malformed or foreign formats are rejected without partially persisting state. A successful import uses the existing synchronized `setDisplay` persistence path and applies live.
- This **does not support MBWhatsApp's proprietary theme files**, arbitrary fonts, launcher icon packs, or screenshot-perfect themes. The menu explicitly explains the distinction. The change does not touch Supabase/FCM/WebRTC or add a paid dependency.
- UI import/export implementation and instrumentation codec tests require CI and emulator checks before merge.

## Stage 3 continuation — MB navigation parity and conversation preview

- MB Preferences now links to actual existing server-backed **Privacy** and Android-backed **Notifications** screens, in addition to General/Home/Conversation/Themes. Returning from nested sections leads back to MB Preferences.
- Long-pressing a home conversation opens a read-only preview of up to three cached, authorized, non-deleted messages with a working **Open conversation** action. Media is represented by descriptive labels, not unauthorized image reads. The existing pin/archive/favorite/mute/delete actions remain.
- The reference-specific visual modal geometry and unsupported notification or theme controls still require further work; never claim full screenshot matching based on these stateful counterparts.

## Stage 4 — per-conversation notification channels (candidate)

- Every conversation menu exposes a working **Custom notifications** dialog.
- Opt-in creates a dedicated `NotificationChannel` per account + conversation. All normal FCM message alerts for that conversation then route to the selected Android channel, with an Android settings intent for the device-controlled sound, vibration and alert level. The original global `messages` channel remains the default when the user opts out.
- Channel IDs are non-identifying SHA-256-derived values of account and conversation UUIDs. Settings are account-scoped, never read from FCM input, and previous-account channels/labels are removed on local account erasure.
- This uses no paid service and does not change Supabase, message delivery, notification-deduplication, muted chat privacy, or incoming voice/video calling channels.
- **Android limitation:** on Android 8+ installed channel sound/importance/vibration are controlled by the OS and user; apps cannot silently change existing channel sound or bypass blocked permissions.
- Instrumentation tests cover default routing, account isolation, dedicated channel creation and cleanup; exact-head CI and two-phone notification acceptance remain release gates.

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
| Privacy, Ghost mode, hide ticks | `mb_privacy_settings`, changelog | **Partial**: real presence/read-receipt server settings exist. Ghost mode, per-contact exceptions and suppression semantics **not implemented**; never show a working-looking toggle without matching server guarantees. |
| Notifications | `mb_notify_settings`, changelog icon/toast/filters | **Partial**: FCM, permission/channel status and existing deduplication are present. Custom notification icons/sounds and contact-specific alerts remain. |
| Themes and presets | `mb_themes_settings`, color pickers, own font assets | **Not yet**: accurate import/export and font/theme selection require storage, persistence and full screen verification. |
| Scheduled messages / editing diff | v2.6.1 changelog | **Not yet**: requires durable scheduling, exact-once outbox, timezone/reboot/account-switch policies; edited-body histories require explicit database/access rules. |
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


# Complete MBWhatsApp 3.3.4 layout coverage register

Static inventory from the user-provided APK: **55 distinct `res/layout*/mb_*.xml` files**. These include reusable UI components, not necessarily 55 standalone screens. No original APK source or third-party account data has been imported into Liquid Chat. **No row below has been proven pixel-identical or device-verified**; an implemented native counterpart is not evidence of behavioral parity.

Status legend: **Partial** = some native counterpart exists but incomplete reference UI/behavior; **Missing** = recognizable feature absent; **Unmapped** = APK layout needs binary hierarchy inspection before assigning behavior. The version 3.3.4 changelog is reference evidence, not proof of how code works.

| Reference layout | Status | Liquid Chat route/counterpart | Gap before claiming exact parity |
|---|---|---|---|
| `mb_activity_image_crop.xml` | Partial | Photo editor with 1:1, 4:5 and 16:9 centered crop, ±90° rotate, two-axis flip, B&W and five-step Undo; PDF scanner | Reference freehand crop gestures, exact toolbar, full edit history/Redo, pixel comparison and real-device proof missing |
| `mb_alert_sticker.xml` | Partial | Sticker creator + saved shelf | Animated packs, exact dialog and segmentation missing |
| `mb_animations.xml` | Partial | DisplayOptions animation settings | Touch and snow overlays not verified |
| `mb_bomb_dialog.xml` | Unmapped | No confirmed safe counterpart | Decode actual dialog interaction before claiming feature |
| `mb_bottom_iosv2.xml` | Partial | GlassNavigation | Exact size, icons and animation not verified |
| `mb_bottom_iosv2_ot.xml` | Partial | GlassNavigation variants | Alternate reference layout not matched |
| `mb_box_item.xml` | Partial | GlassCard | Exact spacing and hierarchy unverified |
| `mb_colorpicker_layout.xml` | Partial | Custom hex and photo-accent settings | Exact color-picker palette unverified |
| `mb_convo_settings.xml` | Partial | Conversation Screen Settings | Custom controls implemented; screenshot parity unverified |
| `mb_custom_cm.xml` | Unmapped | No confirmed native counterpart | Inspect original hierarchy and control semantics |
| `mb_custom_color_picker_dialog.xml` | Partial | Liquid accent hex input | Native popup palette and eyedropper missing |
| `mb_custom_webview.xml` | Partial | MessageInAppBrowser | Website behavior and navigation tests pending |
| `mb_dialog_alert_ios.xml` | Partial | LiquidAlertDialog | Exact blur, transitions and button order unverified |
| `mb_dialog_color_picker.xml` | Partial | Theme/color editor | Full reference dialog not matched |
| `mb_dialog_launcher.xml` | Partial | Classic/Night Glass Activity aliases + Appearance selector | Full reference icon pack and launcher-specific visual verification remain |
| `mb_dialog_media.xml` | Partial | Attachment sheet, PDF/stickers, single photo and up-to-10 photo picker with private sequential outbox | GIF/video attachment and exact reference grouped album presentation still missing |
| `mb_download_dialog.xml` | Partial | Media and story downloads | Reference export confirmation layout not matched |
| `mb_edittext_box.xml` | Partial | Compose conversation composer | Reference animations and accessories unverified |
| `mb_edittext_layout.xml` | Partial | Compose text field/selection | Reference geometry not matched |
| `mb_error_activity.xml` | Partial | Existing errors/retry dialogs | Dedicated reference-style error screen absent |
| `mb_general_settings.xml` | Partial | General Settings | All source sliders/switches not enumerated yet |
| `mb_home_settings.xml` | Partial | Home Screen Settings | Full reference tab and wallpaper options unverified |
| `mb_home_tab.xml` | Partial | Chats home tabs + custom folders | Exact tab layout/animation unverified |
| `mb_home_toolbar.xml` | Partial | GlassHeader | Pixel and icon parity unverified |
| `mb_image_picker.xml` | Partial | Android picker with 10-photo selection, per-photo removal, first-photo caption, atomic private outbox, Undo-enabled single-photo editor | Batch crop/edit controls, original gallery selector geometry and native grouped album parity missing |
| `mb_info.xml` | Partial | ContactMediaDialog | Reference account/details UI unverified |
| `mb_info_profile.xml` | Partial | Profile and contact information | Layout and gestures unverified |
| `mb_info_row_text.xml` | Partial | Contact/profile information rows | Reference typography unverified |
| `mb_item_expandable.xml` | Partial | Settings sections and cards | Animated expand/collapse not matched |
| `mb_item_settings.xml` | Partial | MbPreferencesLanding | All control destinations not mapped |
| `mb_layout_menu_item.xml` | Partial | GlassActionRow | Menu item art and spacing not verified |
| `mb_list_layout.xml` | Partial | Compose preference lists | Full reference item density unverified |
| `mb_list_layout_dialog.xml` | Partial | LiquidActionSheet | Reference scrolling/scrim unverified |
| `mb_list_layout_item.xml` | Partial | Settings list entries | Exact row semantics unverified |
| `mb_list_tips_item.xml` | Partial | MbTipsAndHelp | All tips and content not yet ported |
| `mb_mes_reg.xml` | Unmapped | No confirmed native counterpart | Identify exact underlying screen before implementation |
| `mb_new_dialog_bottom.xml` | Partial | LiquidActionSheet | Reference animation/geometry unverified |
| `mb_notify_settings.xml` | Partial | Notification Settings + DND/landscape quiet mode | Contact-specific status notification options missing |
| `mb_pop_view_layout.xml` | Partial | Long-press conversation preview | Pixel/layout/gesture parity unverified |
| `mb_preferences.xml` | Partial | MbPreferencesLanding | Complete nested navigation/parity unverified |
| `mb_privacy_settings.xml` | Partial | Server-backed Ghost Mode + privacy settings | Per-contact exceptions not implemented |
| `mb_prof_d.xml` | Partial | Liquid Chat profile card | Exact arrangement and edit screen unverified |
| `mb_search_view.xml` | Partial | GlassSearch + message search filters | Full reference global search UI missing |
| `mb_seekbar_dialog.xml` | Partial | Material3 sliders | Reference custom dialog unverified |
| `mb_seekbar_layout.xml` | Partial | Display sliders | Exact gradient/animation missing |
| `mb_service_ui.xml` | Unmapped | No confirmed safe counterpart | Investigate service UI without copying WhatsApp protocol |
| `mb_settings.xml` | Partial | SettingsScreen | Full feature list and reference layout not matched |
| `mb_switch_layout.xml` | Partial | Material3 switches | Reference track/thumb behavior unverified |
| `mb_themes_settings.xml` | Partial | Theme JSON + library + photo color extraction | Fonts/assets and exact presets still missing |
| `mb_tip_aitools.xml` | Partial | Offline draft Text tools: clean spacing, capitalization and case changes with preview/Apply | No remote AI rewrite, translation, hallucination-prone suggestions or external AI service configured |
| `mb_tip_logout.xml` | Partial | Liquid Chat sign-out confirmation | Exact layout/animation unverified |
| `mb_tips_help.xml` | Partial | MbTipsAndHelp | Original page content not fully ported |
| `mb_toast_no.xml` | Partial | App notices + FCM notifications | Reference toast and suppression semantics unverified |
| `mb_update.xml` | Partial | Liquid Chat App Updates panel / update information | Signed release distribution/verification and exact reference layout remain unverified |
| `mb_vip_settings.xml` | Unmapped | No verified equivalent paid entitlement | Do not import another service's subscription or licensing logic |

## 21 October 2026 — reference APK follow-up (committed source only)

- **Batch gallery images:** real Android multi-image picker, bounded to 10 selected images, previews with remove-one controls and optional first-photo caption, account-scoped JPEG processing, and a single Room transaction that queues all selected independent messages or none. WorkManager reuses the existing private media upload/delivery/retry flow. This is not a WhatsApp-protocol grouped album.
- **Photo edit continuation:** fully local center-cropped 1:1 / 4:5 / 16:9 variants, ±90° rotation, horizontal/vertical flip, grayscale and five-step Undo, with bounded temporary revision cleanup on cancel, selecting new photo or successful queue. The native gestures and exact reference toolbar are still unverified.
- **Caption/profile copy:** one-tap copy for authorized Story/Post descriptions (including full-screen viewers), plus copy of the signed-in account's own username and bio. Android clipboard is written only on user action; no third-party service is called.
- **Offline Text tools:** in-chat draft transformations (capitalization, whitespace cleanup, uppercase/lowercase) include before-apply preview, explicit user action and stale-draft protection. These do not contact an AI model, transmit content or claim reference AI parity.
- **Still missing/blocked:** GIF/video attachment transport, pixel-perfect APK dialog art and animated transitions, fully verified private media SQL/Edge deployment, advanced contact-specific status alerts, third-party AI/VIP integrations and WhatsApp-specific groups/channels. User explicitly defers build, lint, CI, SQL/Edge deployments and device tests until later.

## Backend and release dependencies

- Existing messaging and calling remain Liquid Chat services (Supabase/FCM/WebRTC), **not WhatsApp protocol clients**. Reference-specific features such as linked WhatsApp accounts, proprietary groups/channels and commercial AI/VIP services require separate functionality; duplicating a UI control alone would be deceptive.
- **Still in Git only, not applied to the running database:** `supabase/migrations/20261021183500_private_sticker_messages.sql`, `supabase/migrations/20261021195500_private_pdf_documents.sql`, and `supabase/migrations/20261021202500_message_edit_history.sql`. Sticker/PDF migrations also need the associated `supabase/functions/media` deployment. Existing database/user rows must be preserved. New sticker/PDF send cannot work before this paired deployment.
- Chat Folders and saved sticker shelf are **device-only account-scoped**; they do not sync across phones. PDF scanning supports single captured camera pages or up to 10 gallery pages, with no automatic document edge detection. Real sticker background removal is near-white masking, not ML segmentation. Gallery batch-photo sending now queues up to 10 private independent image messages and does **not** claim true WhatsApp grouped-album protocol parity.
- To claim any row complete: inspect binary XML and event behavior, implement matching Compose UI, wire state/backend, and compare reference screenshots at 360/393/412 dp, both themes, relevant font scales and gesture states.
- Owner-requested current workflow deferred builds, SQL/Edge checks, emulator and physical verification. Do **not** assert 100% parity, production readiness, a deployed migration or a final APK from this register.

Tracker: https://github.com/officialpritam007/Project-x/issues/66

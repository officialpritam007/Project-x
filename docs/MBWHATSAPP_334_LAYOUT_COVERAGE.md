# Complete MBWhatsApp 3.3.4 layout coverage register

Static inventory from the user-provided APK: **55 distinct `res/layout*/mb_*.xml` files**. These include reusable UI components, not necessarily 55 standalone screens. No original APK source or third-party account data has been imported into Liquid Chat. **No row below has been proven pixel-identical or device-verified**; an implemented native counterpart is not evidence of behavioral parity.

Status legend: **Partial** = some native counterpart exists but incomplete reference UI/behavior; **Missing** = recognizable feature absent; **Unmapped** = APK layout needs binary hierarchy inspection before assigning behavior. The version 3.3.4 changelog is reference evidence, not proof of how code works.

| Reference layout | Status | Liquid Chat route/counterpart | Gap before claiming exact parity |
|---|---|---|---|
| `mb_activity_image_crop.xml` | Partial | Photo editor with 1:1, 4:5 and 16:9 centered crop, ±90° rotate, two-axis flip, B&W and five-step Undo/Redo; PDF scanner | Reference freehand crop gestures, exact toolbar, unlimited history, pixel comparison and real-device proof missing |
| `mb_alert_sticker.xml` | Partial | Sticker creator + saved shelf | Animated packs, exact dialog and segmentation missing |
| `mb_animations.xml` | Partial | Device-local passive touch glyph with emoji/color setting, initially off, plus current motion controls | Other custom overlays and reference animation curves not established |
| `mb_bomb_dialog.xml` | Blocked | APK has two text inputs, a switch and a 1–20 count field | Bulk flooding/repeated-send behavior not ported; no unsolicited message-bombing tool |
| `mb_bottom_iosv2.xml` | Partial | Reference-mode GlassNavigation: 56dp five-slot footer, 30dp reference icons and eight owner-supplied selected/unselected icon files, 11sp labels, 90dp selection line, account-scoped unread badge | Correct reference destination mapping, font/pixel states, alternate navigation and emulator verification remain unproven |
| `mb_bottom_iosv2_ot.xml` | Partial | GlassNavigation variants | Alternate reference layout not matched |
| `mb_box_item.xml` | Partial | GlassCard | Exact spacing and hierarchy unverified |
| `mb_colorpicker_layout.xml` | Partial | Original 45dp color-picker settings row with 15sp/13sp labels and a real 30dp swatch; used for app ink/background/chrome with persistent RGB color picker and Reset | More exact embedded picker art/transparency color-wheel gestures unverified |
| `mb_convo_settings.xml` | Partial | Conversation Screen Settings with five animated, accessible expandable groups; existing privacy/media controls preserved | Other reference sections, precise icons, nested layout and visual acceptance still unverified |
| `mb_custom_cm.xml` | Unmapped | ScrollView, RowBackground, several labels and divider views identified in binary XML | String resource meanings and source button behavior remain unresolved |
| `mb_custom_color_picker_dialog.xml` | Partial | Native HSV sliders, hex input, 18 swatches, automatic dominant accent and bounded on-device photo preview with actual tapped-pixel RGB eyedropper | Exact reference color-wheel artwork, magnifier gesture, touch precision and device verification remain unproven |
| `mb_custom_webview.xml` | Partial | MessageInAppBrowser | Website behavior and navigation tests pending |
| `mb_dialog_alert_ios.xml` | Partial | Reference-mode 22dp rounded dialogs with fade/spring entry and reduced-motion support | Exact reference backdrop blur, button order and exit transition unverified |
| `mb_dialog_color_picker.xml` | Partial | 18-color swatch palette, three HSV sliders, hex input and local image sampling | Exact reference palette art and manual eyedropper gesture not yet matched |
| `mb_dialog_launcher.xml` | Partial | Classic/Night Glass Activity aliases + Appearance selector | Full reference icon pack and launcher-specific visual verification remain |
| `mb_dialog_media.xml` | Partial | **Corrected binary XML mapping:** its layout is the Photo HD / Video HD / view-once-check settings group, not an attachment chooser. Liquid Chat has genuine Photo HD and 480p/720p Video HD toggles wired to its picker/transcoder. | The original view-once-check bypass is not copied for recipient privacy; native media delivery and visual matching still require verification |
| `mb_download_dialog.xml` | Partial | Authenticated private photo/PDF/MP4 save uses SAF and bounded transfer, with native progress for some transfer modes | Source dialog's 15sp message, 14sp percentage and 15sp cancel progress layout not consistently implemented |
| `mb_edittext_box.xml` | Partial | Compose conversation composer | Reference animations and accessories unverified |
| `mb_edittext_layout.xml` | Partial | Native 45dp editable settings row with 24dp icon, 15sp main/13sp secondary label; touch-character option saves via 18sp/14sp input dialog | More instances in source settings and precise Android view-font metrics unverified |
| `mb_error_activity.xml` | Partial | Recovery screen supports Retry, Sign out and user-initiated 48dp Send report through Android Sharesheet, without logs/tokens/messages | Reference error display's 16sp text and complete report UI not yet pixel matched |
| `mb_general_settings.xml` | Partial | General Settings | All source sliders/switches not enumerated yet |
| `mb_home_settings.xml` | Partial | Home Screen Settings | Full reference tab and wallpaper options unverified |
| `mb_home_tab.xml` | Partial | Chats home tabs/custom folders, reference footer-adjacent 56dp New Chat action opening Contacts (no duplicate floating FAB) | Reference tab arrangement, alternate statuses, animations and screenshot match still unverified |
| `mb_home_toolbar.xml` | Partial | Reference-mode 47dp header, default 17sp title, 28dp Liquid Chat logo and existing toolbar actions | Exact action icon sizes, colors, positioning and pixel parity remain unverified |
| `mb_image_picker.xml` | Partial | Android picker, 10-photo selection, per-photo removal, reorder, individual crop/rotate/flip/B&W edit, first-photo caption, atomic private outbox; separate Undo/Redo photo editor | Original gallery selector geometry, group-send presentation and device proof missing |
| `mb_info.xml` | Partial | ContactMediaDialog | Reference account/details UI unverified |
| `mb_info_profile.xml` | Partial | Contact detail and shared-media hero now uses the reference 128dp authenticated contact avatar, 60dp top spacing, 23sp contact name and 16sp username in Reference style, while preserving working Save contact and signed-media views | Reference action tiles, complete details hierarchy and actual display proof still missing |
| `mb_info_row_text.xml` | Partial | Contact/profile information rows | Reference typography unverified |
| `mb_item_expandable.xml` | Partial | MbExpandableSettingsCard with accessible header and animate/fade expand-collapse | Exact reference tint, padding and section hierarchy not visually verified |
| `mb_item_settings.xml` | Partial | MB-style 48dp rows with 24dp leading/trailing icons, 15dp start and 10dp end insets; genuine Liquid Chat settings destinations | Some reference icons, control density and nested source destinations not matched |
| `mb_layout_menu_item.xml` | Partial | GlassActionRow | Menu item art and spacing not verified |
| `mb_list_layout.xml` | Partial | Compose preference lists | Full reference item density unverified |
| `mb_list_layout_dialog.xml` | Partial | LiquidActionSheet | Reference scrolling/scrim unverified |
| `mb_list_layout_item.xml` | Partial | Settings list entries | Exact row semantics unverified |
| `mb_list_tips_item.xml` | Partial | MbTipsAndHelp | All tips and content not yet ported |
| `mb_mes_reg.xml` | Blocked | APK embeds MBWhatsApp registration/info screen | Supabase Auth and Liquid Chat branding replace the proprietary mod registration flow |
| `mb_new_dialog_bottom.xml` | Partial | Reference bottom sheet: 22dp top corners, 16dp outer padding, centered 17sp title, 12dp header gap, working close and existing action callbacks | Actual animation timing, artwork and device screenshot parity unverified |
| `mb_notify_settings.xml` | Partial | Notification Settings + DND/landscape quiet mode | Contact-specific status notification options missing |
| `mb_pop_view_layout.xml` | Partial | Long-press popup with 3 account-scoped sender-aware incoming/outgoing bubble previews, timestamps and private-media placeholders | Exact reference gesture radius and visual geometry unverified |
| `mb_preferences.xml` | Partial | Centered 110dp Liquid Chat logo, 20dp margins, 14sp section labels, inset dividers, connected General/Home/Conversation/Themes/Privacy/Notifications routes | Full binary layout/animation parity and some original menu groupings not verified |
| `mb_privacy_settings.xml` | Partial | Server-backed Ghost Mode + privacy settings | Per-contact exceptions not implemented |
| `mb_prof_d.xml` | Partial | Liquid Chat profile card | Exact arrangement and edit screen unverified |
| `mb_search_view.xml` | Partial | Real reference 35dp search row with 12dp inset, 24dp icon and 16sp hint implemented for Chats/Contacts, including clear and focus | Full reference global-search results and animated transitions unverified |
| `mb_seekbar_dialog.xml` | Partial | 17sp reference title, Preview label, 18sp value and Save/Cancel slider dialog wired to real message font/emoji size and bubble opacity | Exact reference slider tint, thumb, gesture curves and screenshot match unverified |
| `mb_seekbar_layout.xml` | Partial | Reference conversation sliders now appear in 45dp selectable rows that open a staged value dialog instead of immediately mutating the setting | Original per-row artwork, panel spacing and full dialog parity unverified |
| `mb_service_ui.xml` | Unmapped | Service heading, description and three labeled action controls identified | Source service behavior is not known; no misleading mock buttons or foreign protocol integration |
| `mb_settings.xml` | Partial | SettingsScreen | Full feature list and reference layout not matched |
| `mb_switch_layout.xml` | Partial | 45dp accessible native switch row with 24dp leading icon and 15sp/12sp labels; active Photo HD, Video HD and touch-animation controls | Native reference switch-thumb animation and pixel comparison unverified |
| `mb_themes_settings.xml` | Partial | Theme JSON/library, user-confirmed saved-theme preview, local accent photo extraction, HSV/swatches | Proprietary reference theme files, external fonts/assets, exact package previews unimplemented |
| `mb_tip_aitools.xml` | Partial | Offline draft Text tools: clean spacing, capitalization and case changes with preview/Apply | No remote AI rewrite, translation, hallucination-prone suggestions or external AI service configured |
| `mb_tip_logout.xml` | Partial | Authenticated Liquid Chat sign-out warning and unsent-message confirmation remain real | Reference 30sp title and explanatory 20sp full-page layout still missing |
| `mb_tips_help.xml` | Partial | MbTipsAndHelp | Original page content not fully ported |
| `mb_toast_no.xml` | Partial | App notices + FCM notifications | Reference toast and suppression semantics unverified |
| `mb_update.xml` | Partial | Liquid Chat App Updates panel / update information | Signed release distribution/verification and exact reference layout remain unverified |
| `mb_vip_settings.xml` | Partial | Device-local iOS-style contact info switch controlling actual Liquid Chat contact/Shared Media hero layout | Foreign VIP paywall not imported; exact reference contact visual still needs screenshot parity |

## 21 October 2026 — reference APK follow-up (committed source only)

- **Batch gallery images:** real Android multi-image picker, bounded to 10 selected images, previews with remove-one controls and optional first-photo caption, account-scoped JPEG processing, and a single Room transaction that queues all selected independent messages or none. WorkManager reuses the existing private media upload/delivery/retry flow. This is not a WhatsApp-protocol grouped album.
- **Photo edit continuation:** fully local center-cropped 1:1 / 4:5 / 16:9 variants, ±90° rotation, horizontal/vertical flip, grayscale and five-step Undo/Redo, with bounded temporary revision cleanup on cancel, selecting new photo or successful queue. The native gestures and exact reference toolbar are still unverified.
- **Caption/profile copy:** one-tap copy for authorized Story/Post descriptions (including full-screen viewers), plus copy of the signed-in account's own username and bio. Android clipboard is written only on user action; no third-party service is called.
- **Offline Text tools:** in-chat draft transformations (capitalization, whitespace cleanup, uppercase/lowercase) include before-apply preview, explicit user action and stale-draft protection. These do not contact an AI model, transmit content or claim reference AI parity.
- **Still missing/blocked:** GIF animation and longer videos (30s clips are source-implemented but not deployed), pixel-perfect APK dialog art and animated transitions, fully verified private media SQL/Edge deployment, advanced contact-specific status alerts, third-party AI/VIP integrations and WhatsApp-specific groups/channels. User explicitly defers build, lint, CI, SQL/Edge deployments and device tests until later.

## 2026-10-21 continuation — PDFs and account-safe search

- PDF picker additionally supports selecting up to **five different files**. Each is copied into the signed-in account's private directory with bounded 8 MiB validation; the preview allows removing a document; `ChatRepository.sendDocuments` inserts all chosen independent PDF messages in a single Room transaction, then schedules one existing outbox drain. No server album grouping or deployed backend support is claimed.
- Global downloaded-history search now intersects results with active account, its currently known conversation IDs, non-deleted and sent-only state. Plain-text type filter no longer includes unsent/unqueued attachments that have not yet been assigned media IDs.
- These are source-only direct-main changes with `[skip ci]`; no CI, tests, migrations, Edge deployments or APK verification have been executed per user instruction.

## 21 October 2026 — direct reference-icon/layout port (source only)

- The owner-supplied APK's **eight original selected/unselected navigation PNG icons** for Chats, Calls, People/Contacts and Settings were extracted, bounded to 96×96 transparent raster canvases and committed as `app/src/main/assets/liquid_reference/ref_*.png`. They are used only in `DisplayOptions.navBarStyle == "Reference"`, via Coil's bundled-asset decoder; all resource paths are Liquid-branded. The app's Profile tab deliberately keeps its existing profile icon instead of pretending to be a WhatsApp-specific Status/Communities route.
- Reference navbar measures 56dp, default 30dp icons, five weight-balanced tab regions, 11sp captions, and a selection line. New Chat has a 56dp dedicated footer-adjacent action on the home tab wired to the real Contacts screen; its duplicate floating FAB is suppressed in Reference style. No username or avatar data was copied from the APK.
- Reference home toolbar uses 47dp, default 17sp title and a 28dp branded Liquid Chat icon. MB Preferences is reshaped with a 110dp centered **Liquid Chat** logo, 20dp outer margins, 14sp section headings and 48dp settings rows with 24dp icons.
- Original assets are supplied solely as visual reference; the old Figma/Liquid theme remains available. This source-only port has **not** been compiled, linted, screen-captured or physically tested at the owner's request. Actual MB menu destination mapping, animations, complex layouts and pixel-perfect behavior are still incomplete. Do not claim full parity.

## Backend and release dependencies

- Existing messaging and calling remain Liquid Chat services (Supabase/FCM/WebRTC), **not WhatsApp protocol clients**. Reference-specific features such as linked WhatsApp accounts, proprietary groups/channels and commercial AI/VIP services require separate functionality; duplicating a UI control alone would be deceptive.
- **Still in Git only, not applied to the running database:** `supabase/migrations/20261021183500_private_sticker_messages.sql`, `supabase/migrations/20261021195500_private_pdf_documents.sql`, `supabase/migrations/20261021202500_message_edit_history.sql`, and `supabase/migrations/20261021230500_private_chat_videos.sql`. Sticker/PDF migrations also need the associated `supabase/functions/media` deployment. Existing database/user rows must be preserved. New sticker/PDF send cannot work before this paired deployment.
- Chat Folders and saved sticker shelf are **device-only account-scoped**; they do not sync across phones. PDF scanning supports single captured camera pages or up to 10 gallery pages, with no automatic document edge detection. Real sticker background removal is near-white masking, not ML segmentation. Gallery batch-photo sending now queues up to 10 private independent image messages and does **not** claim true WhatsApp grouped-album protocol parity.
- To claim any row complete: inspect binary XML and event behavior, implement matching Compose UI, wire state/backend, and compare reference screenshots at 360/393/412 dp, both themes, relevant font scales and gesture states.
- Owner-requested current workflow deferred builds, SQL/Edge checks, emulator and physical verification. Do **not** assert 100% parity, production readiness, a deployed migration or a final APK from this register.

Tracker: https://github.com/officialpritam007/Project-x/issues/66

## 21 October 2026 — private video source implementation (not tested)

- Private 30-second MP4 chat videos reuse on-device StoryVideo 720p H.264/AAC trim/transcode, copy bounded output into account-owned private media files, and require an authenticated conversation member.
- The additive chat_video SQL migration extends media checks, prepared uploads, participant/block/rate-limited send RPCs, service-only finalize, signed media metadata and garbage reclamation. The media Edge Function inspects actual MP4 before finalization and uses private Supabase Storage rather than public Cloudinary links.
- The Android outbox retries each video as a normal message. Users can choose a clip start, add caption, send, explicitly play received videos with short-lived URLs, find them in Shared Videos/search/starred views, or export caption-only text. A user-initiated Save MP4 control reauthorizes the visible message before downloading at most 8 MB to an Android SAF-selected location. Playback stops on app backgrounding, authorization changes, and active calls.
- Gallery batches support drag-free left/right reordering, single-image edits before queuing and a first-photo caption. There is no claim of a native server album object.
- **Release blocker:** no applied migration, current Edge deployment, Gradle build, emulator, signed-URL read/save, multiple-device call test, or screenshot comparison has been performed. The owner explicitly deferred testing and deployment. Never mark video messages operational until the migration/Edge release pair is verified against the production database; do not replace or delete existing user data.

## 2026-10-22 — reference conversation, popup and theme source port

- **Conversation:** Tapping a visible emoji reaction chip now sends an authenticated toggle for the current account after rechecking the live message is not deleted/changed. Swiping a sent message to reply draws a reveal indicator and springs back, honoring reduced-motion preferences. The backend message-action semantics are retained.
- **Reference dialogs & menus:** Shared `GlassActionRow` switches to 48dp target height and 24dp icons in Reference mode, and LiquidActionSheet uses tighter spacing and 22dp top corners. LiquidAlertDialog animates alpha/scale on presentation and avoids animation under reduced motion. These are layout/motion counterparts, not screenshot proofs.
- **Theme editor:** 18 preselected RGB swatches, independent hue/saturation/brightness sliders and hex fields, plus a user-chosen photo's dominant color extracted locally. No color commits until Apply. Saved themes display sanitized mock incoming/outgoing conversation previews before explicit application, without leaking real chat data.
- **Expandable settings:** Header, Media Quality, Copy, Wallpaper and Message Bubble groups use a reusable animated expandable component with descriptive screen-reader state and preserved actual settings callbacks.
- **Long-press chat preview:** Sender-aligned metadata-only text bubbles, time labels, and strict active-account/conversation/sent/deletion gates; only the last three downloaded messages are exposed. No signed attachment URLs or third-party reference account data are rendered.
- **Source-level maintenance:** Fixed selection underline's invalid `ColumnScope.align(Alignment.BottomCenter)` usage by retaining underline in `BoxScope` and using a bounded relative width.
- **Owner's release gate remains unchanged:** These commits are `[skip ci]` source changes. Do not assert MB 3.3.4 pixel equivalence or call/sticker/PDF/video end-to-end operation. CI, builds, unit tests, emulator tests, Supabase migrations, Edge Function deployment, screenshot comparisons and two-phone testing have not been run and remain explicitly deferred.

## 2026-10-22 — decoded APK controls, choice dialogs and touch visuals

- **Actual compiled layouts examined:** `mb_dialog_alert_ios` uses a title, text and divided choice rows; `mb_animations` has an initially disabled touch animation switch, an emoji default ❤️ and a separate color chooser; `mb_item_expandable` contains title, secondary label and arrow. These now have functioning native Compose counterparts.
- **Choice popups:** Bubble style, default bubble palette, call/video/header icon colors and media upload quality now use a row-and-divider selection dialog matching the reference structure, wired to the existing persisted DisplayOptions.
- **Contact information:** Reference iOS-style contact information is a functional local preference in Conversation Settings, affecting avatar scale/card arrangement in Contact Details and Shared Media. No external VIP licensing/paywall is copied.
- **Touch effects:** Nonconsuming root pointer observer, optional local glyph/color, disabled by default. At most eight fading particles for ~700ms, suppressed under Reduce Motion; no remote service or input interception.
- **Limits:** `mb_bomb_dialog` appears related to repeated message flooding (two inputs, 1–20 hint, switch), deliberately not implemented. The branded registration and proprietary service panels are not Liquid Chat equivalents. `mb_custom_cm` still needs semantic source string mapping. No native APK pixel fidelity has been verified.
- **Release gate:** All commits are source-only `[skip ci]`. No build, CI, lint, live SQL/Edge deployment, screenshots, emulator, phone verification or APK generated; the owner decides when final testing starts.

## 22 October 2026 — corrected compiled APK layout mapping (source-only)

- Parsed the binary XML in the supplied APK directly. `mb_dialog_media.xml` is the **Photo HD / Video HD / view-once-check settings group**, not the attachment sheet; `mb_edittext_layout.xml` is a **45dp settings input row**, not the conversation composer. The reference `mb_search_view.xml` is 35dp high with 24dp search icon and 12dp start margin. `mb_new_dialog_bottom.xml` has 16dp outer padding, a centered 17sp title and 12dp gap.
- Search/Reference bottom sheets now use those measurements and true clear/close actions. Reference conversation message actions reuse their existing server-checked callbacks but appear in a bottom sheet, with icon-bearing rows and destructive action styling. A hidden/revoked message dismisses its action menu on the active session.
- Genuine Video HD toggle added with persistent 480p Standard / 720p HD H.264/AAC export. Story exports still default to 720p; authenticated private chat video remains capped at 30 seconds and 8 MB. Do not reproduce a third-party view-once bypass that contradicts media privacy.
- Owner explicitly deferred builds, lint, CI, Supabase migrations/Edge deployments and device verification. Do not claim parity or production readiness yet.

## 22 October 2026 — reference input/control layouts

- `mb_colorpicker_layout.xml`: original 45dp color row, 15sp/13sp labels, 30dp preview swatch rebuilt and wired to genuine device-local app text/background/chrome colors (native themes unchanged unless chosen).
- `mb_switch_layout.xml`: original 45dp control row, leading 24dp icon and 15sp/12sp labels rebuilt with a single TalkBack-accessible switch action; Photo HD, Video HD and touch animation use real saved settings.
- `mb_edittext_layout.xml` and `mb_edittext_box.xml`: editable 45dp touch-character row and 18sp input title, 14sp hint and Save/Cancel actions; changes commit only on Save.
- `mb_seekbar_dialog.xml`: 17sp title, Preview, 18sp value and staged Save/Cancel used for message font size (10–30 sp), emoji picker (18–32 sp) and bubble opacity (45–100%). No settings change occurs on Cancel.
- These are **unbuilt source commits**. Owner asked to defer CI/lint/build tests until all implementation work is over. No device screenshots or verified APK are available yet.

## 22 October 2026 — reference contact details and safe recovery controls

- Parsed the owner's binary `mb_info_profile.xml`: 60dp top spacing before a 128dp avatar, 23sp name, 16sp details and 14sp secondary detail. Reference-mode Liquid Chat contact shared-media now renders the original spacing and avatar/name sizing, retaining actual private signed-media queries and real Save Contact callback.
- Parsed `mb_error_activity.xml`: an error surface with 20dp padding and a 48dp Send report control. Liquid Chat recovery provides user-initiated Android share of only a generic profile-load issue, app version and Android API; no raw server error, credentials, account IDs, phone numbers or message content leave the app.
- Remaining native UI/behavior gaps remain open. All changes **source-only and unverified**: no Gradle, lint, CI, emulator, database deployment or APK tested per owner's explicit workflow.

## 22 October 2026 — continuation: true photo-eyedropper and font-state fix

- Native MB-style color dialog can open a photo, show an ephemeral software-bitmap preview bounded to 640px, and sample the **exact opaque RGB pixel tapped**, correctly accounting for image letterboxing under `ContentScale.Fit`. Taps on margins/transparent pixels are rejected. Original photo is neither uploaded nor saved to theme exports; the sampled HEX value still needs explicit **Apply**. The existing automatic dominant-accent sampler, HSV sliders and swatches remain.
- Fixed the real imported-font selection regression: `AppViewModel.setDisplay` previously accepted `Imported` when loading preferences but silently sanitized a freshly selected imported body font back to `System` when writing preferences. It now accepts `Imported`; the renderer keeps its safe fallback if the selected font file is no longer present.
- New source-only commits: `7cc698b`, `6b008ea`, `ff51d80`, `0e28361`. No build, lint, CI, emulator, migration, deployment, screenshot, or two-phone test was run. **No pixel-equivalence or functionality certification is claimed.**


## 2026-10-22 — conversation chrome/search/composer stage (source-only)

- Reused the compiled APK's **35dp search / 24dp icon** reference structure through existing `GlassSearch` in conversation search instead of Material's tall outlined text field. Sender, type, date, next/previous-result navigation, query account scope and explicit close remain wired to their original callbacks.
- Reference mode now uses a **compact, edge-aligned** conversation header and composer visual structure. These are **reconstructions**, not a claim that WhatsApp's actual chat XML or Dex was imported. The APK's `mb_home_toolbar` measures 47dp/17sp but this layout is a *home toolbar*, not proof of the complete private chat header's geometry. Liquid alternative styling is preserved. No message input, draft, voice capture, attachment, authorization or calling callbacks were changed.
- The original `mb_edittext_box` is a **settings input dialog**, not evidence for pixel-identical chat composer dimensions; do not conflate these in parity reports.
- Prior stage changed Reference-mode Chats, Calls and Contacts to flat list presentation and made Preferences primary, retaining all actual routes.
- **Verification**: only source commits. Per owner instruction, no Gradle build, CI, lint, emulator screenshot/pixel comparison, backend deployment, APK install or two-phone acceptance. Visual parity and runtime behavior are unverified.

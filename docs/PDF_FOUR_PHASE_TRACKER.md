# Project X — complete PDF four-phase execution tracker

**Source:** owner-supplied PDF Gallery (22 October 2026), 12 numbered sections; plus [346-item engineering acceptance matrix](mb-parity/FEATURE_MATRIX.md).

**Status:** baseline source audit in progress. Unchecked = not accepted/verified; existing implementation may already exist. Never check a box based on UI labels alone. Do not claim exact visual parity, working backend, passing builds or completed releases without evidence.

**Owner's execution rules:** commit source updates directly to **`main`**; no pull requests; do not run tests, CI, APK builds or deploy backend. Include **`[skip ci]`** on code commits. Never silently delete work, data, encrypted assets or the author's screen captures. No unauthorized privacy bypasses, impersonated services, or fake group/channel/paid features.

**4 phases / 12 PDF sections**; each specific PDF item has a unique `PDF-Sxx-nnn` ID. Original engineering items retain their `Pxx-nnn` IDs at the bottom; these are a separate complementary set and are intentionally not conflated. Both inventories must be reviewed during implementation.

## Phase 1 — Overall UI, Navigation, Settings & General

### PDF Section 1 — Overall UI and Navigation

- [ ] `PDF-S01-001` Light/white canvas, bold readable text, green accent, fine grey borders
- [ ] `PDF-S01-002` Rounded grouped cards, compact bubbles and pill composer
- [ ] `PDF-S01-003` Consistent typography, icon weight, spacing and accessible touch targets
- [ ] `PDF-S01-004` Updates, Calls, Communities, Chats, Profile destination parity (genuine backed routes only)
- [ ] `PDF-S01-005` Selected navigation styling and unread badges
- [ ] `PDF-S01-006` Global search shortcut and actual search destination
- [ ] `PDF-S01-007` Rounded toolbar buttons, avatars, badges
- [ ] `PDF-S01-008` Reorder navigation destinations without changing route IDs
- [ ] `PDF-S01-009` Context menus and bottom sheets with lifecycle-safe animations
- [ ] `PDF-S01-010` Light, Dark, System and Custom theme modes
- [ ] `PDF-S01-011` Smooth keyboard, scrolling and page transitions
- [ ] `PDF-S01-012` Liquid Glass toggle, blur controls, reduced-transparency fallback
- [ ] `PDF-S01-013` Liquid Chat brand assets; no impersonation of other products

### PDF Section 2 — Main Settings

- [ ] `PDF-S02-001` Centered owner profile photograph and display name
- [ ] `PDF-S02-002` What's new route and release notes
- [ ] `PDF-S02-003` Settings header and Appearance/Add/Edit actions
- [ ] `PDF-S02-004` Liquid Chat Preferences navigation
- [ ] `PDF-S02-005` Tips and Help route
- [ ] `PDF-S02-006` Service Control: specify genuine scope; no fake controls
- [ ] `PDF-S02-007` App information and version details
- [ ] `PDF-S02-008` Add Lists route and functional account-scoped folders
- [ ] `PDF-S02-009` Starred Messages destination from Settings
- [ ] `PDF-S02-010` User-selected app language (not merely a fake label)
- [ ] `PDF-S02-011` Account screen
- [ ] `PDF-S02-012` Privacy and security screen
- [ ] `PDF-S02-013` Chats settings screen
- [ ] `PDF-S02-014` Appearance screen
- [ ] `PDF-S02-015` Notifications screen
- [ ] `PDF-S02-016` Storage and Data Usage screen
- [ ] `PDF-S02-017` Invite a Friend using native sharing
- [ ] `PDF-S02-018` Correct installed version footer

### PDF Section 3 — Preferences Landing

- [ ] `PDF-S03-001` Centered Liquid Chat application logo
- [ ] `PDF-S03-002` Privacy and Security category
- [ ] `PDF-S03-003` Notify Preferences category
- [ ] `PDF-S03-004` Genuine free Extra Features in place of third-party VIP entitlements
- [ ] `PDF-S03-005` Themes category
- [ ] `PDF-S03-006` General Configuration category
- [ ] `PDF-S03-007` Home Screen category
- [ ] `PDF-S03-008` Multimedia category
- [ ] `PDF-S03-009` Conversation Screen category
- [ ] `PDF-S03-010` Animations category
- [ ] `PDF-S03-011` Developer Information
- [ ] `PDF-S03-012` Make a Donation only with verified owner funding destination
- [ ] `PDF-S03-013` Support, help and updates destinations
- [ ] `PDF-S03-014` Searchable preferences with control keywords

### PDF Section 4 — General Configuration

- [ ] `PDF-S04-001` Universal font size
- [ ] `PDF-S04-002` UI scale and device-safe in-app density adjustment
- [ ] `PDF-S04-003` Performance mode disabling costly blur or animation
- [ ] `PDF-S04-004` Blur intensity, color and contrast
- [ ] `PDF-S04-005` Correct Android status/system bar inset handling
- [ ] `PDF-S04-006` Selectable notification icon
- [ ] `PDF-S04-007` Automatic cache cleanup with retention safety
- [ ] `PDF-S04-008` Real safe Clean Now operation, never delete the outbox
- [ ] `PDF-S04-009` Main/global accent color
- [ ] `PDF-S04-010` Alternative accent / random accent setting
- [ ] `PDF-S04-011` Secondary color
- [ ] `PDF-S04-012` Header and Footer color
- [ ] `PDF-S04-013` App background customization
- [ ] `PDF-S04-014` Show Wallpaper toggle
- [ ] `PDF-S04-015` Gallery wallpaper selection with persisted access
- [ ] `PDF-S04-016` Custom Font enable/disable
- [ ] `PDF-S04-017` Import and choose local font
- [ ] `PDF-S04-018` Independent bold-title font
- [ ] `PDF-S04-019` Android custom app shortcut creation
- [ ] `PDF-S04-020` Launcher icon selector with working aliases
- [ ] `PDF-S04-021` Restore accessible defaults

### Existing engineering acceptance IDs assigned to Phase 1

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

## Phase 2 — Home, Chats, Messaging & Calls

### PDF Section 5 — Home Screen Customization

- [ ] `PDF-S05-001` Profile name as Home title
- [ ] `PDF-S05-002` Enable/disable home chat filter chips
- [ ] `PDF-S05-003` Hide archived shortcut without deleting archive
- [ ] `PDF-S05-004` Hide home floating buttons
- [ ] `PDF-S05-005` Bottom tab preview and rearrangement
- [ ] `PDF-S05-006` Hide tab labels
- [ ] `PDF-S05-007` Hide tab icons
- [ ] `PDF-S05-008` Per-tab visibility with overflow recovery
- [ ] `PDF-S05-009` Navigation icon dimensions
- [ ] `PDF-S05-010` Navigation label dimensions
- [ ] `PDF-S05-011` Bottom tab spacing and system safe area
- [ ] `PDF-S05-012` Toggle glass-effect/navigation style
- [ ] `PDF-S05-013` Blur intensity on compatible Android versions
- [ ] `PDF-S05-014` Global Search shortcut beside navigation
- [ ] `PDF-S05-015` Liquid Glass on buttons
- [ ] `PDF-S05-016` Separate Chats and Groups only with real group backend
- [ ] `PDF-S05-017` Online indicator respects owner/contact privacy
- [ ] `PDF-S05-018` No-photo contact avatar style
- [ ] `PDF-S05-019` Support more than three pinned conversations
- [ ] `PDF-S05-020` Last-seen respects server visibility
- [ ] `PDF-S05-021` Home chat row dividers
- [ ] `PDF-S05-022` Long-press chat preview
- [ ] `PDF-S05-023` Chat row names/snippet font sizing
- [ ] `PDF-S05-024` Story thumbnail blur
- [ ] `PDF-S05-025` Toggle story reaction section
- [ ] `PDF-S05-026` Disable automatic story advance
- [ ] `PDF-S05-027` Hide Channels until real backend exists
- [ ] `PDF-S05-028` Optional story start with sound
- [ ] `PDF-S05-029` Story card styling and spacing

### PDF Section 10 — Chats Home Screen

- [ ] `PDF-S10-001` Chats heading and toolbar
- [ ] `PDF-S10-002` Overflow actions
- [ ] `PDF-S10-003` Camera shortcut
- [ ] `PDF-S10-004` New chat button
- [ ] `PDF-S10-005` All filter
- [ ] `PDF-S10-006` Unread count filter
- [ ] `PDF-S10-007` Favorites filter
- [ ] `PDF-S10-008` Groups filter (requires genuine groups)
- [ ] `PDF-S10-009` Custom list creation and membership
- [ ] `PDF-S10-010` Circular avatars
- [ ] `PDF-S10-011` Name, snippet and timestamp
- [ ] `PDF-S10-012` Sent, delivered and read ticks
- [ ] `PDF-S10-013` Media-type labels for image/video/PDF
- [ ] `PDF-S10-014` Unread badges
- [ ] `PDF-S10-015` Swipe actions with deliberate gestures
- [ ] `PDF-S10-016` Long-press quick actions
- [ ] `PDF-S10-017` Pin chats
- [ ] `PDF-S10-018` Mute chats
- [ ] `PDF-S10-019` Archive chats
- [ ] `PDF-S10-020` Favorite chats
- [ ] `PDF-S10-021` Mark read/unread
- [ ] `PDF-S10-022` Search entry
- [ ] `PDF-S10-023` Bottom navigation glass/background

### PDF Section 11 — Actual Conversation Screen

- [ ] `PDF-S11-001` Back action, optional unread badge and hierarchy
- [ ] `PDF-S11-002` Contact name and avatar
- [ ] `PDF-S11-003` Voice call action
- [ ] `PDF-S11-004` Video call action
- [ ] `PDF-S11-005` Per-chat wallpaper and pattern
- [ ] `PDF-S11-006` Incoming bubble appearance
- [ ] `PDF-S11-007` Outgoing bubble appearance
- [ ] `PDF-S11-008` Date separators
- [ ] `PDF-S11-009` Message timestamps
- [ ] `PDF-S11-010` Sent/delivered/read ticks
- [ ] `PDF-S11-011` Quoted story thumbnail
- [ ] `PDF-S11-012` Quoted reply and jump to original
- [ ] `PDF-S11-013` Standalone large emoji
- [ ] `PDF-S11-014` Reaction chips and selection
- [ ] `PDF-S11-015` Safe media placeholders and progress
- [ ] `PDF-S11-016` Jump to bottom
- [ ] `PDF-S11-017` Keyboard/IME insets
- [ ] `PDF-S11-018` Attachment chooser
- [ ] `PDF-S11-019` Pill text composer
- [ ] `PDF-S11-020` Emoji and sticker launcher
- [ ] `PDF-S11-021` Camera shortcut
- [ ] `PDF-S11-022` Microphone/Send state switch
- [ ] `PDF-S11-023` No unexpected scroll jump on keyboard open/close
- [ ] `PDF-S11-024` Message menu, reply, forward, copy, star, pin and delete

### Existing engineering acceptance IDs assigned to Phase 2

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

## Phase 3 — Multimedia, Conversation Styles, Emoji & Stickers

### PDF Section 6 — Multimedia Settings

- [ ] `PDF-S06-001` HD images toggle controls real private encoder
- [ ] `PDF-S06-002` HD video toggle controls real 720p / 480p encoder
- [ ] `PDF-S06-003` Reference grouping, iconography, switch states and accessibility
- [ ] `PDF-S06-004` Document upload compression, limits and receiver quality

### PDF Section 7 — Conversation Appearance and Interactions

- [ ] `PDF-S07-001` 3-column bubble preset picker
- [ ] `PDF-S07-002` iOS-compatible bubble presentation (own visuals)
- [ ] `PDF-S07-003` Bubble preset 1
- [ ] `PDF-S07-004` Bubble preset 2
- [ ] `PDF-S07-005` Bubble preset 3
- [ ] `PDF-S07-006` Bubble preset 4
- [ ] `PDF-S07-007` Bubble preset 5
- [ ] `PDF-S07-008` Bubble preset 6
- [ ] `PDF-S07-009` Bubble preset 7
- [ ] `PDF-S07-010` Bubble preset 8
- [ ] `PDF-S07-011` Bubble preset 9
- [ ] `PDF-S07-012` Bubble preset 10
- [ ] `PDF-S07-013` Bubble preset 11
- [ ] `PDF-S07-014` Message font size
- [ ] `PDF-S07-015` Emoji size
- [ ] `PDF-S07-016` Larger sticker display
- [ ] `PDF-S07-017` Custom sent sticker size
- [ ] `PDF-S07-018` Outgoing/right bubble colors and ink
- [ ] `PDF-S07-019` Incoming/left bubble colors and ink
- [ ] `PDF-S07-020` Link color override
- [ ] `PDF-S07-021` Conversation action icon color
- [ ] `PDF-S07-022` Double-tap reaction enable/disable
- [ ] `PDF-S07-023` Double-tap default emoji
- [ ] `PDF-S07-024` Custom popup reaction slot 1
- [ ] `PDF-S07-025` Custom popup reaction slot 2
- [ ] `PDF-S07-026` Custom popup reaction slot 3
- [ ] `PDF-S07-027` Custom popup reaction slot 4
- [ ] `PDF-S07-028` Custom popup reaction slot 5
- [ ] `PDF-S07-029` Custom popup reaction slot 6
- [ ] `PDF-S07-030` Confirmation before saved sticker sending
- [ ] `PDF-S07-031` Alternative reply popup
- [ ] `PDF-S07-032` One-tap message options enable/disable
- [ ] `PDF-S07-033` Quick reactions and message popup
- [ ] `PDF-S07-034` Native emoji styling with copyright-safe glyphs
- [ ] `PDF-S07-035` Animated emojis toggle
- [ ] `PDF-S07-036` Admin marker only if real group roles
- [ ] `PDF-S07-037` Quick reply suggestion visibility
- [ ] `PDF-S07-038` Incognito keyboard request
- [ ] `PDF-S07-039` Copy sender and timestamp option
- [ ] `PDF-S07-040` Incoming message sound
- [ ] `PDF-S07-041` Outgoing message sound
- [ ] `PDF-S07-042` Border-only white sticker background removal
- [ ] `PDF-S07-043` Edited message indicator
- [ ] `PDF-S07-044` Edited message timestamp

### PDF Section 12 — Emoji, GIF and Sticker Picker

- [ ] `PDF-S12-001` Rounded expandable emoji panel
- [ ] `PDF-S12-002` Panel drag handle
- [ ] `PDF-S12-003` Emoji search
- [ ] `PDF-S12-004` Emoji tab
- [ ] `PDF-S12-005` GIF tab
- [ ] `PDF-S12-006` Sticker tab
- [ ] `PDF-S12-007` Recent emoji
- [ ] `PDF-S12-008` Suggested hi/haha/love/sad/wow reactions
- [ ] `PDF-S12-009` Smileys and People
- [ ] `PDF-S12-010` Animals and Nature
- [ ] `PDF-S12-011` Food
- [ ] `PDF-S12-012` Activities
- [ ] `PDF-S12-013` Travel and Places
- [ ] `PDF-S12-014` Objects
- [ ] `PDF-S12-015` Symbols
- [ ] `PDF-S12-016` Flags
- [ ] `PDF-S12-017` Category icon navigation
- [ ] `PDF-S12-018` Lazy performant scrollable grid
- [ ] `PDF-S12-019` Sticker creation entry
- [ ] `PDF-S12-020` Saved packs shelf
- [ ] `PDF-S12-021` Sticker grid
- [ ] `PDF-S12-022` Recent stickers
- [ ] `PDF-S12-023` Favorite stickers
- [ ] `PDF-S12-024` Licensed/consented GIF and sticker sources

### Existing engineering acceptance IDs assigned to Phase 3

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

## Phase 4 — Privacy, Notifications, Backend & Release Acceptance

### PDF Section 8 — Privacy and Security

- [ ] `PDF-S08-001` Server-enforced online/last-seen visibility
- [ ] `PDF-S08-002` Forwarded provenance displayed honestly
- [ ] `PDF-S08-003` Status view/receipt privacy only with backend authorization
- [ ] `PDF-S08-004` Call/video-call control per selected contact
- [ ] `PDF-S08-005` Private in-app browser with safe external link handling
- [ ] `PDF-S08-006` TikTok HTTPS URLs treated as untrusted external content
- [ ] `PDF-S08-007` Hide read receipt/blue ticks when server allows
- [ ] `PDF-S08-008` Read receipt after reply/react option
- [ ] `PDF-S08-009` Delivery tick must reflect actual server delivery
- [ ] `PDF-S08-010` Voice played/read marker privacy
- [ ] `PDF-S08-011` Typing indicator privacy
- [ ] `PDF-S08-012` Recording indicator privacy
- [ ] `PDF-S08-013` Per-contact privacy exception persistence and RLS
- [ ] `PDF-S08-014` Group privacy only after group membership/backend is real
- [ ] `PDF-S08-015` Never recover somebody else's deleted message
- [ ] `PDF-S08-016` Never recover somebody else's deleted status
- [ ] `PDF-S08-017` Never bypass view-once expiration or consent
- [ ] `PDF-S08-018` Screen capture and task-preview privacy

### PDF Section 9 — Notify Preferences

- [ ] `PDF-S09-001` Disable notifications per signed-in account
- [ ] `PDF-S09-002` Auto-dismiss only message alerts after five seconds
- [ ] `PDF-S09-003` Floating bubble alerts with user permission
- [ ] `PDF-S09-004` Filter displayed sender name without leaking it
- [ ] `PDF-S09-005` Read alerts aloud only after informed opt-in
- [ ] `PDF-S09-006` Silence message alerts during Do Not Disturb
- [ ] `PDF-S09-007` Silence alerts during landscape/video playback
- [ ] `PDF-S09-008` Do not enrich caller country/city without informed permission and real source
- [ ] `PDF-S09-009` Contact online alerts require server-authorized presence
- [ ] `PDF-S09-010` Story sharing alerts respect private story audience
- [ ] `PDF-S09-011` Android 10–16 channels, FCM deduplication and permission state

### Existing engineering acceptance IDs assigned to Phase 4

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

## Tracking and commit evidence

- PDF check items: **243** across twelve PDF sections. Original engineering checks: **346** in twelve tracks (all assigned once to a phase here).
- Phase 1 source commits (all **unverified**): `7321f45` (main Settings landing), `7d50466` (searchable dashboard), `34ac301` (verified public developer links and honest donations status), `b26033c` (Developer Information in Preferences), `4154392` (real Settings route wiring).
- Older source commits: `a3b5ce7`, `ba9ee9b`, `54b6771`, `3e444d9`, `05220d7` (preference search, invitation/version links).
- **None of the 346 original items or the PDF checkboxes are automatically certified by creating this document.** Advance individual statuses with evidence and capture unresolved external/backend dependencies.
- Phase 2 includes backend-only Groups/Channels/Communities requirements, not placeholder views. Phase 4 includes verification gates, which remain blocked while automated tests/builds are disallowed.


## Incremental source implementation — 22 October 2026 continuation

The following **direct-`main` source commits** correspond to existing checklist IDs. Their status is `SOURCE-CHANGED / UNVERIFIED`; no automated tests, Android builds, two-phone acceptance or Supabase deployments have been run. Leave acceptance checkboxes open until the owner authorizes evidence gathering.

| Phase | PDF/engineering requirements touched | Commit(s) | Concrete source change and remaining limitation |
| --- | --- | --- | --- |
| 1 | `PDF-S02-009`, `P09-013` | `2c9e505`, `7d4f4ba` | Settings opens the already existing account-scoped downloaded-starred-message sheet; uncached history remains unlisted |
| 1 | `PDF-S02-006`, `PDF-S02-015` | `b542b29`, `03e7688`, `3dfafc4` | Service Status screen shows current session/connection, explicit authorized refresh and Android notification permission controls; not a server restart toggle |
| 2 | `P04-030`, `PDF-S11-024` | `e9f806c`, `991feef` | Conversation search filters already downloaded hyperlinks; regex repaired to stop on whitespace; no remote global-search claim |
| 2 | `PDF-S10-009`, `P01-007` | `35a5eee`, `7767472`, `8fcc296` | Account-local folder rename preserving membership and a chat picker search; no server groups claimed |
| 2/4 | `PDF-S10-022`, `P01-015`, `P10-002` | `21e2e42` | Inbox filters the active account and optionally searches existing visible last-message previews; privacy-hiding setting excludes previews |
| 3 | `PDF-S12-001`, `PDF-S12-002`, `PDF-S12-008`–`PDF-S12-019`, `P05-006`–`P05-014` | `d00734d`, `4daffb8` | Native emoji bottom sheet, eight visible local Unicode categories and quick reactions; not a third-party GIF service or full OS emoji catalog |
| 3 | `P05-032`, `PDF-S11-022` | `3490a86`, `6f9db57` | Android MediaRecorder pause/resume while voice recording is locked, timer excludes paused time; device audio acceptance remains open |
| 4 | `P03-013`, `P03-014` | `f1191ab` | In-app browser refuses loopback/private IP/IPv6 literal and local hostname navigation; DNS rebinding and malicious public sites still need ordinary platform safeguards |

**Unfinished scope:** authenticated Groups/Communities/Channels, backend privacy exceptions, real two-device calling and FCM acceptance, licensed GIF sources, advanced media editors, release signing, screenshot parity and the entire prescribed verification phase are **not complete**. No empty menu item or source checkbox represents such functionality as done.


## Additional direct-main source work — 22 October 2026

All entries below are **source changes only**, never acceptance certification. No tests, builds, emulator runs, physical-phone checks or backend deployments were performed for these commits.

| Phase | Acceptance IDs | Commit | Implemented source behavior / limitation |
| --- | --- | --- | --- |
| 2 | `PDF-S10-009`, `P01-007` | `f952c4b` | Confirmation before deleting a device-local chat folder; existing chats and messages are untouched |
| 2 | `PDF-S02-009`, `P09-013` | `d3ee429` | Search cached, account-isolated starred messages by text and conversation name; uncached items remain out of scope |
| 3 | `PDF-S12-003`, `PDF-S12-019`, `P05-014` | `4c2b4e1` | Virtualized emoji rows and always-reachable category selector; not a licensed GIF service |
| 3 | `P05-015` | `7b4dba1` | Explicit skin-tone preference applied to supported hand/person emojis only; other glyphs retained as-is |
| 1 | `PDF-S04-003`, `P02-023`, `P02-024` | `565ff1b` | Battery-friendly visuals action persists reduced motion, opaque surfaces and zero blur; existing choices can restore effects |
| 4 | `PDF-S08-005`, `P03-013`, `P03-014` | `c502372` | Confirmation before loading an unencrypted HTTP chat link; ordinary HTTPS links open through existing isolated browser |

These changes do **not** complete the PDF's 243 distinct requirements or the complementary 346-item engineering matrix. Group/channel backends, many media features, device/provider validation, and release verification remain outstanding.


## Additional consent-based accessibility work — 22 October 2026

These are **unverified source edits**, not checked acceptance criteria:

| Phase | Requirement IDs | Commit | Scope and remaining limitations |
| --- | --- | --- | --- |
| 2 | `PDF-S11-024`, `P04-028` | `4a58e80`, `7e5263a` | Message long-press offers optional text-to-speech after an explicit engine/privacy confirmation; capped at 2,000 characters, user can stop reading |
| 4 | `P03-032`, `P04-028` | `8e65575` | Stops ongoing manual message speech when Android app leaves foreground or the conversation/account is disposed; device behavior not validated |

The speech feature invokes the installed Android TTS engine; it does **not** claim offline/private processing by the vendor, and is not used for unsolicited spoken push alerts.

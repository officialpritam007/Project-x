# Liquid Chat — consolidated screen-level reference UI review (2026-10-21)

This register is for the user's requested **single integrated Android app**, branded only **Liquid Chat**, modeled on the supplied reference APK. The user's final manual run is **one all-in-one two-phone acceptance**, not one install per PR. Automated CI and disposable emulators are independent pre-merge gates.

| Screen | UI / interactive controls in this consolidated UI pass | Data source | Validation |
|---|---|---|---|
| Chats / Home | Existing five-tab iOS-style navigation, story carousel, actual query/filter chips, section title and visible conversation count, working new-chat, long-press and conversation shortcuts | Filtered signed-in Room chats, existing Supabase messaging | Screenshot + Compose navigation/search test |
| Contacts | Liquid Glass header with real Find and sort actions, live search, saved-contact count, invite, actual save/remove and chat entry | Authenticated contacts cache + existing server find/save RPC | Compose screen test and full Android suite |
| Calls | Glass history, real All/Missed/Voice/Video filters, loaded-page count, redial with mic/camera guards, detail sheet | Signed-in, participant-scoped call-history RPC cache | JVM filter tests, screenshot and two real devices |
| Stories | Existing real 24h private-story horizontal carousel and editor/viewer, enhanced audience-aware heading | Existing secured moments RPC and authenticated protected media | Existing screenshot / moment tests and physical video playback |
| Conversation | Existing avatar/presence, voice/video call buttons, media, voice, reliable composer, replies, reactions, search and appearance; shared Glass header clamps long titles to a single line at larger system font | Existing scoped Supabase, Room, WebRTC, FCM | Existing conversation and message tests / two real devices |
| Profile | Current photo, edit/info/settings actions and private Posts/Archive within a consistent rounded glass hero. No invented online badge | Authenticated profile and moments RPC | Compose snapshot and existing profile tests |
| Settings | Existing real account, privacy, notification, theme, storage and power-saving panels. Omit three previously *nonfunctional* stubs (unimplemented device linking, translated language, server-backed chat folders) rather than simulate real features. | Actual persisted device and server settings | Compose regression: unavailable entries absent |

**Non-goals / unresolved source-vs-original parity:** pixel-perfect icons, all original third-party fonts and binary illustrations, proprietary WhatsApp server behavior, complete stickers/document editing and distribution, phone-specific WebRTC/NAT/TURN reliability, complete OEM notification behavior. Do not silently pretend unsupported controls work. These remain open in #66 / #24 / #91. Existing Supabase user data, Firebase Cloud Messaging, caller UI/RTC, free-tier limits, app ID `com.pritam.liquidchat` and branding are preserved.

**Release rule:** source-head 5/5 mandatory CI → verified PR merge → exact merged-main 5/5 → one combined development APK link and comprehensive two-device validation sheet. No repeated manual installations.


## Direct-main implementation pass — testing explicitly withheld by owner

All commits in this pass use `[skip ci]`. The owner explicitly requested **no CI, lint, build, emulator tests, screenshot verification or phone tests** until they separately approve verification. Consequently all items below are **committed, unverified**, not production-certified.

- **Theme custom color**: opaque `#RRGGBB` local picker, validation and theme file round-trip, adaptable Material primary/onPrimary rendering, optional random accent per new app session.
- **Independent conversation colors**: custom outgoing/incoming bubble `#RRGGBB` pickers, four text/quote/time independent `Custom` ink color pickers with actual message and preview usage, bounded bubble opacity, safe import/export and local persistence.
- **Home panel**: independently hide or restore filter chips and Stories carousel. Hidden Stories remain reachable in the Home overflow menu (view/add).
- **Notifications**: real selectable Classic/Outline white-vector status-bar icon used for message, incoming-call and active-call alerts; device-local only.
- **Animations**: existing Normal/Balanced/Extra and reduced-motion preferences now also control tab-selection and typing-dot animation timing.
- **Preferences navigation**: real Account, Data & Storage, Power Saving pages added to the reference-style Preferences landing; Back returns to the proper parent.
- **No backend mutation**: Existing Supabase messages, Cloudinary media, FCM token routing, WebRTC calling, account privacy, delivery state, Room migrations and registered application package remain unchanged.

**Unimplemented / non-equivalent**: original third-party custom fonts/emoji packs, full proprietary sticker builder/export and document editing/transfer, full original WhatsApp group/channel/Meta service flows, device-wide stealth/anti-revoke/receipt bypass, all screenshot-precise pixels/icons/animations, carrier/TURN two-phone media validation, and irreversible read receipts semantics. Do not expose fictional working controls. Resolve only with real compatible product implementations and the owner's later explicit verification permission.

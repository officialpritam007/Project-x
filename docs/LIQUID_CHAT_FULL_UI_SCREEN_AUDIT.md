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

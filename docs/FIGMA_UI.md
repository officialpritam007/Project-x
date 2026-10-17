# Liquid Glass UI integration

Source: https://www.figma.com/design/VRHamP7Sz4YmHCROClffbW?node-id=0-1

The five Figma frames map to native Compose: Chats (1:2), Contacts (1:36), Settings (1:68), Profile (1:123), Conversation (1:162). The shared palette uses the five-stop ice-blue wallpaper, 44% white reflective surfaces, white edges, rounded 24–33 dp containers and cyan selection pills. The conversation wallpaper is #A2CFA3 with #E1FAD8 outgoing bubbles. Native status bars, keyboard insets, scrolling, font scaling and real account data replace the prototype's fixed 390×844 coordinates, sample messages and fake status bar.

Nine original vector exports are bundled under `app/src/main/assets/figma`, rendered by Coil SVG at the original 18 dp search / 24 dp navigation sizes. File names preserve the source layer IDs. Each active tab uses its corresponding cyan icon; inactive tabs use the neutral original. There are no expiring Figma URLs in app code. The design's backgrounds contain smooth color gradients; reflective layers emulate that appearance without applying blur to foreground text. A live backdrop refraction/blur effect is not implemented. Reduced transparency uses an opaque surface; reduced motion and the existing animation preference remain available.

Existing Supabase authentication, Room flows, realtime subscriptions, private media signing and Cloudinary uploads are retained. No schema, backend deployment, billing or hosting-plan changes were needed. Typing remains in the bubble area; the header keeps online/last-seen. Existing delivery acknowledgments, stable message keys, history paging, retry, blocking, reporting, archive/favorite and logout/account erasure actions remain.

## Interaction adaptations
- Four icon-only, accessible tabs: Chats / Contacts / Settings / Profile.
- Chat filters moved to the header menu to preserve the Figma layout. New chat opens Contacts. The camera shortcut also opens Contacts so an image is sent to an explicitly chosen recipient.
- Contact search filters saved contacts; Find Friends opens exact-username lookup with the existing start/save actions. Invite Friends opens Android's share sheet. Sort uses saved data, without inventing last-seen values.
- Profile actions edit the existing profile, set/remove an avatar or open Settings.
- Settings groups lead to existing account, appearance and privacy controls. Device management and other unavailable controls explain their current capabilities.
- Stories and Posts have explicit unavailable states. Add a post is disabled. Voice recording is not implemented; the composer exposes the working send and image attachment actions. No placeholder action claims to publish content.

## Verification
Local `git diff --check` passes. Local Android execution is unavailable because this environment has no Android SDK and the Gradle distribution cannot be downloaded. GitHub Actions runs compilation, JVM tests, Android lint and APK packaging, followed by the existing Room emulator tests. Physical-device visual comparison, font scaling/keyboard checks, and real signed-in Supabase/Cloudinary journeys remain required; no pixel-perfect runtime claim is made.

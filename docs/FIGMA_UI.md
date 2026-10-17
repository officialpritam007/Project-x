# Liquid Glass UI integration

Source: https://www.figma.com/design/VRHamP7Sz4YmHCROClffbW?node-id=0-1

The five Figma frames map to native Compose: Chats (1:2), Contacts (1:36), Settings (1:68), Profile (1:123), Conversation (1:162). The shared palette uses the five-stop ice-blue wallpaper, 44% white reflective surfaces, white edges, rounded 24–33 dp containers and cyan selection pills. The conversation wallpaper is #A2CFA3 with #E1FAD8 outgoing bubbles. Native status bars, keyboard insets, scrolling, font scaling and real account data replace the prototype's fixed 390×844 coordinates, sample messages and fake status bar.

Nine original vector exports are bundled under `app/src/main/assets/figma`, rendered by Coil SVG at the original 18 dp search / 24 dp navigation sizes. File names preserve the source layer IDs. Each active tab uses its corresponding cyan icon; inactive tabs use the neutral original. There are no expiring Figma URLs in app code. The design's backgrounds contain smooth color gradients; reflective layers emulate that appearance without applying blur to foreground text. Haze 1.5.4 now captures actual backdrop layers and applies a 30 dp live blur on Android 12+; Android 10/11 use its readable scrim fallback. Foreground text is not blurred. Optical lens refraction is not simulated. Reduced transparency uses an opaque surface; reduced motion and the existing animation preference remain available.

Existing Supabase authentication, Room flows, realtime subscriptions, private media signing and Cloudinary compatibility is retained; private Supabase Storage is the fallback while Cloudinary server credentials are missing. An additive migration extends authenticated media to AAC voice messages and private moments. Billing and hosting plans remain unchanged. Typing remains in the bubble area; the header keeps online/last-seen. Existing delivery acknowledgments, stable message keys, history paging, retry, blocking, reporting, archive/favorite and logout/account erasure actions remain.

## Interaction adaptations
- Four icon-only, accessible tabs: Chats / Contacts / Settings / Profile.
- Chat filters moved to the header menu to preserve the Figma layout. New chat opens Contacts. The camera shortcut also opens Contacts so an image is sent to an explicitly chosen recipient.
- Contact search filters saved contacts; Find Friends opens exact-username lookup with the existing start/save actions. Invite Friends opens Android's share sheet. Sort uses saved data, without inventing last-seen values.
- Profile actions edit the existing profile, set/remove an avatar or open Settings.
- Settings groups lead to existing account, appearance and privacy controls. Device management and other unavailable controls explain their current capabilities.
- Stories support image/caption publishing and 24-hour expiry. Posts support image/caption publishing, archive/restore and deletion. Audiences are the creator’s saved contacts with both-way blocks enforced. The microphone records AAC up to 120 seconds with slide-to-cancel, slide-up hands-free lock and playback seeking. The existing durable outbox uploads/retries voice files with stable message IDs. Capture/playback stop when the app backgrounds.

## Verification
Local `git diff --check` passes. Local Android execution is unavailable because this environment has no Android SDK and the Gradle distribution cannot be downloaded. GitHub Actions runs compilation, JVM tests, Android lint and APK packaging, followed by the existing Room emulator tests. Physical-device visual comparison, font scaling/keyboard checks, and real signed-in Supabase/Cloudinary journeys remain required; no pixel-perfect runtime claim is made.

# Liquid Chat — Codex archive transfer and truthful implementation status
_Date: 21 October 2026. Source-only direct-`main` work; user requested verification only after implementation._

## Source evidence and merge boundary

Reference workspace was uploaded as `2026-10-21.zip` and contained a genuine Liquid Chat
Git repository under `work/Project-x`. Its independent local Codex commit
`e0438b4478088376b340e2fd7a29eb7a4560f13c` is based on
`49f6ec0dceb9601258c6aae3932577bf7527ad8a` and touches 53 files.

That **local Git commit object itself has not been pushed to GitHub**. Its feature changes
were adapted to the later remote `main` and committed **source-by-source** using the
connected GitHub integration. This matters because remote `main` already contained
significant permission/session fixes at `a2416afa721a4606f8c7f8de195427ecf8a83279`
that would be lost by force-pushing the old local branch. Do not cherry-pick those 53 files
blindly; compare actual feature code on the current head.

## Adapted source changes (not verified)

| Area | Actual Liquid Chat integration |
|---|---|
| Updates | User-facing version and real Project-x release notes / report issue links |
| Keyboard privacy | Android `IME_FLAG_NO_PERSONALIZED_LEARNING` only on active conversation input; keyboard implementation may ignore the hint |
| Saved stickers | Real device-local size preference 96–256 dp and optional send confirmation; authenticated existing sticker outbox; still requires server deployment |
| Conversation | Double-tap existing `message_action` reaction, bounded emoji choice, optional editor timestamps, quick reply suggestion visibility |
| Story playback | Device-local pause-autoadvance, replay on the viewer and optional initial sound honoring silent/DND |
| Home | Long-press preview and floating button visibility, without losing Contacts/New chat routes |
| Navigation | Five tab visibility toggles, hidden routes retained in a More menu even if all five are hidden |
| Theme | HSV color picker for accent, bubble and ink; per-device app/background/chrome colors; size adjustments with bounded density |
| Fonts | Device-private ≤8 MiB TTF/OTF import with directory validation, independent app/title fonts and original fallback; third-party fonts not bundled |
| Launcher | Original Blue/Night Glass plus new Mint/Midnight Liquid Chat-branded aliases; no change to main activity for OAuth or calls |
| Recovery | Full-screen account-safe profile loading/retry/sign out |
| Conversation sounds | Optional default-off foreground text/audio cues honoring DND, silent mode, audio playback, calling and current account |
| Visual themes | Round-trip compatible appearance-only settings with no exported auth/personal message state |

The uploaded archive's test source has been partially ported into independent `app/src/test`
source files for the later owner-approved verification phase; those tests have **not** been executed.

### Invariants

- Retain application id `com.pritam.liquidchat`, Supabase backend/users/data, Firebase FCM,
  existing protected Cloudinary/private Supabase storage, Room cache, WebRTC audio/video and
  existing Liquid Glass UI.
- Do not add reference APK branding, third-party secrets or proprietary messaging services.
- Only the future user-authorized final verification may compile, test, lint, build,
  run emulator suites, deploy backend code/migrations or produce a final APK.
- A feature is **not live** just because an Android control and matching SQL file exist.

## Gating deficiencies, unimplemented parity

1. GitHub SQL migrations `20261021183500_private_sticker_messages.sql`,
   `20261021195500_private_pdf_documents.sql` and
   `20261021202500_message_edit_history.sql` **were not present on production** at
   last read-only Supabase inspection. The active production `media` Edge Function
   did **not** include new sticker or PDF finalization handlers. Sticker, PDF and
   secure edit-history features are **not end-to-end production verified**.
2. A source-level port is not a 53-file Git merge; original Codex instrumentation
   and some unit regression cases still need review/adaptation.
3. Pixel-by-pixel parity has not been verified against all 55 reference APK
   `mb_*.xml` layouts; refer to `MBWHATSAPP_334_LAYOUT_COVERAGE.md` for
   each partial/missing layout and non-equivalence.
4. WhatsApp proprietary identity, delivery networks, anti-revoke behavior,
   groups, channels and Meta-only services are not interchangeable with
   Liquid Chat's one-to-one Supabase backend. Do not present unsupported actions.
5. Modern GIF/video/file send/receive coverage beyond existing photo, voice, sticker
   and PDF code remains incomplete, as does exhaustive two-phone FCM delivery,
   free-tier TURN/WebRTC calling, push/performance and accessibility acceptance.
6. The current direct-main delta has intentionally **never been built, linted,
   unit-tested, screenshot-tested, emulator-tested or installed on two phones**.
   Keep this status until the user explicitly authorizes final testing.

## Owner-controlled final acceptance sequence (not executed)

When the owner requests the final check after implementation:
- Review static Kotlin and SQL migrations; check schema access privileges, byte validation,
  regressions and Edge code; back up production or verify on disposable staging first.
- Run the complete project-supported Android CI/build/JVM/Compose/instrumentation suite
  on the **exact integrated main** and repair failures without losing any feature.
- Apply approved additive migrations and Edge Function in dependency order with
  accurate deployment verification; do not overwrite live data.
- Generate **one calling-enabled All-in-One APK** from verified exact main.
- Run the single complete two-phone test checklist for message state, media,
  notifications, privacy, Stories, profiles, voice and video calling.
- Publish real GitHub Actions artifact link only after verifying the existing artifact.

**Current status: feature source port in progress; NOT release-ready.**

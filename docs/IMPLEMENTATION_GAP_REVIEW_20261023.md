# Implementation gap review — 23 October 2026

Baseline: `4db1e73bcd8b359590c1c3d42c5114315f7fb07c`, clean clone of main.
Read README, PRD, both inventories, Issue 24 and its comments, and issues 114–125.
Latest README authorization supersedes older PR-only/no-testing notes.

## Inventory integrity and evidence

The primary tracker has 243 unique PDF IDs and 346 unique engineering IDs.
The engineering set exactly matches FEATURE_MATRIX.md. Both sets remain intact;
no requirement has been accepted in this session. Source inspection is not runtime
verification. Historical green builds do not verify this checkout.

## Existing / Needs Audit

- Phase 1: LiquidApp routes and GlassNavigation, DisplayOptions/theme, persisted
  appearance, reference Settings dashboard, searchable preferences, fonts, launcher
  aliases/shortcuts, cache-only maintenance, help/share/version are present.
  Current routes are Chats/Calls/Contacts/Settings/Profile, not the requested
  Updates/Calls/Communities/Chats/Profile. Language currently displays English-only.
- Phase 2: AppViewModel -> ChatRepository -> account-keyed Room chats/messages,
  durable outbox, Backend session generation checks, drafts, search, reactions,
  message actions, voice, private attachments and WebRTC source exist.
  End-to-end delivery and lifecycle behavior have not been verified here.
- Phase 3: Images/ChatVideo, protected viewer, PDF/sticker library, emoji picker,
  bubble shapes and media quality paths exist. A licensed GIF/animated sticker
  service and full pack/editor acceptance remain open; no mock substitute added.
- Phase 4: Backend authentication/session checks, participant/call RLS migrations,
  private media bucket with direct client access denied, authorized media RPC,
  trusted Edge handlers, FCM account/dedup/preview controls exist. These were
  source-inspected only, not certified against the deployed backend.

## Fixes and integration gaps addressed

- 266 KB ChatScreens.kt held all major screens. Extracted seven focused source
  files without renaming public composables or changing navigation callbacks.
  ConversationScreen remains large; further decomposition and real compiler
  measurements are required before P00-001/P00-003 can be accepted.
- Twelve bubble presets were rejected by save/reload/theme import despite being
  offered in the picker. A shared supported set now includes all 15 choices.
- Custom accent was accepted on save but rejected on process restoration.
- Show Wallpaper had no persisted display control. Rendering now consumes it.
- Appearance reset unintentionally restored video quality; now preserved.
- DisplayPreferences separates persistence from the ViewModel so regression tests
  can use isolated preferences without backend or account initialization.

## Build / architecture blockers

JDK 17 is installed; the standard local Android SDK directory is absent.
The initial Gradle attempt could not write its default cache; a workspace cache
resolved that, but wrapper download failed DNS for services.gradle.org. Thus no
Android compile, JVM, lint or instrumentation result is claimed.
Host suite: 41 attempted; after moving temp files into workspace, 36 passed and
five shell smoke tests errored because bash is absent. No test was disabled.
GitHub Linux candidate workflow remains the intended full verification gate.
Known Compose teardown issue 91 remains open. No root-cause claim from extraction.

## Missing implementation and external gates

Real groups/communities/channels require schema, membership roles, RLS, quotas,
moderation and app integration. Per-contact server privacy and genuine story
receipts require explicit backend coverage. Live migrations/deployments were not
performed. App language localization and remaining settings/rendering controls
need implementation. Full screenshot parity requires same-state reference images.
Physical FCM/OEM, two-phone media/calling and authenticated TURN acceptance require
provider/device access; production signing, Play/policy/support and operations
remain owner gates. Existing user data and service configuration were untouched.

## Resume order

1. Compile this exact integrated change and fix actual compiler failures; run all
   candidate mandatory checks, retaining failures and provenance.
2. Finish decomposing ConversationScreen state/dialogs while retaining callbacks.
3. Continue Phase 1 gaps, then remaining Phase 2/3/4 IDs; record per-ID evidence.
4. Only promote one final APK after exact-source checks and provenance; do not
   represent this incremental repair as phase completion or production readiness.

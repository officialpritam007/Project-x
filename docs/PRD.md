# Liquid Chat — Living Product Requirements

**Repository:** [Project-x](https://github.com/officialpritam007/Project-x)  
**Document owner:** project maintainer  
**Updated:** 2026-10-23  
**State:** working product scope; not a release-acceptance certificate

## Purpose and sources of truth

Liquid Chat is an Android-first, private person-to-person messaging app with a Liquid Glass-inspired, accessible UI. The canonical detailed product baseline is [Liquid_Chat_PRD-1.md](../Liquid_Chat_PRD-1.md). This living summary captures later, current scope without deleting that earlier specification.

Read this together with the [owner PDF requirements and four-phase tracker](PDF_FOUR_PHASE_TRACKER.md), [346-item engineering matrix](mb-parity/FEATURE_MATRIX.md), [latest implementation gap review](IMPLEMENTATION_GAP_REVIEW_20261023.md), and [master issue #24](https://github.com/officialpritam007/Project-x/issues/24). Later explicit owner requirements supersede older exclusions when they conflict. **Do not infer that a documented requirement has been implemented, tested, or released.**

## Product outcome

Users should reliably sign in, find a contact, send and receive private messages across unstable networks, understand truthful message states, access authorized media, and customize a responsive, readable Android experience. The product is one integrated Liquid Chat app—not multiple builds for separate features.

## Users and core journeys

1. **New user:** authenticate, accept the versioned policy, create a profile, find a username, and open a one-to-one chat.
2. **Returning user:** restore the account safely, show cached conversations promptly, and synchronize without duplicates or visible jumps.
3. **Offline sender:** retain drafts/outbox through interruption, retry idempotently, and show queued/failed/sent states honestly.
4. **Recipient:** receive only authorized content and appropriate FCM notifications; delivered/read indicators require recipient evidence, not merely push submission.
5. **Media user:** send or view images, voice, documents/stickers where supported, and private Stories/Posts with access controls.
6. **Caller:** use experimental one-to-one voice/video calls with correct permissions, foreground behavior, lifecycle cleanup, and an explicit real-device acceptance gate.
7. **Accessibility/privacy user:** adjust appearance/transparency/motion; block, report, mute and remove account data under documented policies.

## Current scope and priority

| Priority | Requirements | Acceptance boundary |
|---|---|---|
| P0 | Authentication/session safety; profiles and contacts; one-to-one text; local outbox; truthful receipts; authorization/RLS; safe logout/account isolation | Exact-head CI plus real two-account and offline tests |
| P0 | Private image/media access; notification routing/privacy; blocks, reports and deletion controls | Backend authorization and physical-device/provider validation |
| P0 | Working navigation, search, Settings, accessibility and reliable UI state | No inert/fake controls; screen/IME/font-scale tests |
| P1 / experimental | Voice messages, Stories/Posts, custom appearance, documents/stickers, one-to-one WebRTC voice/video calls | Feature-specific code, server support, tests and two-phone acceptance |
| Planned, **not shipped by assumption** | Requested Updates/Communities/Groups/Channels parity, advanced packs/GIF/editor flows, localization, precise reference fidelity | Separate backend models, permissions, UI and verification before status changes |

Earlier PRD v1.1 excluded calls, but newer code/owner tracking explicitly includes experimental calling. **Do not advertise group messaging or Communities as operational:** they require real membership schemas, RLS and full implementation. Never claim end-to-end encryption; there is no verified E2EE design in this project.

## Functional acceptance requirements

- **Identity:** tokens are protected on device, OAuth/recovery redirects are validated, and logout/account switching cannot expose the previous account's Room cache, queued work, media or previews.
- **Messaging:** stable client IDs, idempotent retry, immutable server ordering, paging, monotonic states and correct message ownership; edited/deleted/tombstoned content must not reappear.
- **Privacy:** enforce participant/blocked-user checks server-side for content, media, receipts and calls; never treat hiding a button as authorization.
- **Media:** preserve protected Supabase Storage fallback; optional authenticated Cloudinary must not degrade privacy or require a paid plan.
- **Notifications:** FCM is a transport hint, not proof of delivery; respect mute, preview and foreground-current-chat settings.
- **Design:** retain Liquid Chat branding and Liquid Glass option; readable contrast, scalable text, 48 dp touch areas where appropriate, accessibility semantics, and reduced-transparency/motion paths.
- **Calls:** no production call-reliability promise without real two-phone testing across lifecycle/audio/network conditions.

## Release definition of done

All required source/head build, JVM, lint, Edge and emulator checks must pass for the **exact candidate commit**. Publish only a traceable artifact with version, commit and signature provenance. Privileged provider configuration, live authorization checks, two physical phones, production signing, Play eligibility, legal/support/deletion hosting and operations are separate gates. A debug APK or a source checkbox is **not** production approval.

## Success indicators (targets, not measured claims)

- Zero unaccounted-for message loss, duplicate send, unauthorized read, or cross-account cache leak in acceptance suites.
- Clear, recoverable user-facing failures; no fabricated delivery/read/online state.
- Navigable, accessible critical journeys at large font scales and in offline/slow-network conditions.
- Evidence-linked closure of each applicable PDF and engineering requirement.

**Change control:** record decisions in [MEMORY.md](MEMORY.md), engineering constraints in [RULES.md](RULES.md), and acceptance/evidence in [TRACKER.md](TRACKER.md).

# Liquid Chat — Delivery & Acceptance Tracker

**Snapshot:** 2026-10-23  
**Overall status:** implementation and integration in progress; **not production-approved**

## Canonical requirement registers

This page is a human-readable **dashboard**, not a replacement for these full item-level checklists:

| Register | Count | Meaning |
|---|---:|---|
| [Four-phase owner PDF tracker](PDF_FOUR_PHASE_TRACKER.md) | 243 IDs | \`PDF-Sxx-nnn\` across 12 numbered sections; unchecked is **unaccepted** |
| [Engineering feature matrix](mb-parity/FEATURE_MATRIX.md) | 346 IDs | \`Pxx-nnn\`, independent of the PDF list |
| [Master issue #24](https://github.com/officialpritam007/Project-x/issues/24) | ongoing | Integrated scope, CI evidence and final calling-enabled APK |
| [2026-10-23 implementation gap review](IMPLEMENTATION_GAP_REVIEW_20261023.md) | snapshot | Source evidence, remaining work and limitations |
| [QA matrix](QA_MATRIX.md), [bug ledger](BUG_LEDGER.md) | ongoing | Detailed test runs, bugs, blockers |

**Never convert “source exists” into an accepted checkbox.** In particular, historical green runs do not verify new commits. The two numbered inventories are distinct and must not be merged or discarded.

## Phase dashboard

| Phase / stream | Evidence now visible in repository | Status / next proof |
|---|---|---|
| Phase 1: UI, nav, Settings, general | Compose routes, appearance preferences, theme and Settings source; 2026-10-23 gap review identifies language/nav parity gaps | **Source present / needs audit**; exact-head build, screenshot/behavior tests and remaining PDF IDs |
| Phase 2: messaging, calls and lifecycle | Room, outbox, Backend, message actions, voice and WebRTC source/tests | **Needs end-to-end verification**; delivery/read, account switching, two-device calling and stability |
| Phase 3: media and visual parity | Image/video/PDF/sticker/editor/Stories paths and selected tests | **Partial / needs verification**; real private-media/backend deployment, licensed assets and pixel parity |
| Phase 4: backend, security, notifications, release | RLS/migrations, functions, FCM integration source and workflows | **Source present; provider/release gates open**; deployed migrations, FCM service config, external tests |
| Final candidate | \`app/build.gradle.kts\` declares \`1.0.0-rc12-liquid-preview\` (19) on snapshot main | **Debug candidate only**; no current exact-head production-readiness claim |

## Highest-priority open work

| Priority | Task / acceptance | Current position | Evidence to attach |
|---|---|---|---|
| P0 | Build/test **exact integrated main**: unit, lint, assemble, API 29/35/36 emulator, Edge checks | **Unverified for current snapshot**; local compile blocked in gap review | Commit SHA, five workflow/job outcomes, failing logs and signed/unsigned artifact provenance |
| P0 | Preserve Room outbox, account isolation and true receipts through offline/restart/two accounts | **Needs real integration acceptance** | Device/account cases, no loss/duplication/privacy leak |
| P0 | Resolve [Compose teardown issue #91](https://github.com/officialpritam007/Project-x/issues/91) durably | **Open**; intermittent rerun success does not prove root cause | Reproduction, root cause, repeat green shards with tests intact |
| P0 | Real FCM/SMTP/OAuth/private media and provider-side operations | **Blocked / owner-provider verification** | Provider config confirmation without secrets, live two-device results |
| P0 | Production signing, Play/privacy/deletion/support/moderation gates | **Blocked / owner-provider verification** | Signed release provenance, hosted URLs, owner operational acceptance |
| P1 | Requested PDF navigation, language, Settings and genuine Community/Group behavior | **Partial / missing**, not a cosmetic-only fix | Feature-specific schema, RLS, UI, persistence, tests |
| P1 | Final screenshot and accessibility parity | **Needs verification** | Same-state screenshots, device/resolution, 200% font and TalkBack checks |
| P1 | Sticker/PDF/GIF/editor features and private media correctness | **Partial / backend and acceptance gap** | Runtime tests, migration deployment and private access checks |
| P1 | Physical voice/video calls, audio focus, network/TURN and OEM lifecycle | **Experimental; unverified** | Single consolidated two-phone results on final exact APK |

## How to update a requirement

1. Reference its **exact** \`PDF-Sxx-nnn\` or \`Pxx-nnn\` ID and scope in the original register.
2. Record code/source location, responsible issue, commit SHA and **exact-head** check/run.
3. Distinguish **Source present**, **Automated verified**, **Live provider verified**, **Physical-device verified** and **Accepted**. Missing proof means **Open**, even when source code looks complete.
4. On failure, link logs and a follow-up issue; don't erase the failure or mark the item done.
5. Mark checkbox accepted **only** when its acceptance criteria and all dependencies are independently evidenced.

## Suggested per-item record

| ID | Owner | Status | Commit / CI | Device / provider proof | Blocker / next step |
|---|---|---|---|---|---|
| _Exact requirement ID_ | _Maintainer_ | _Open / Verified / Accepted_ | _SHA + run URL_ | _Case and result or N/A_ | _Concrete next action_ |

## Final release gate checklist

- [ ] Current integrated source compiles, lints and passes all required JVM/emulator/Edge checks.
- [ ] Same-source debug APK version/signature/hash/artifact provenance documented.
- [ ] All release-critical PDF and engineering IDs are evidence-accepted, with no security/privacy blocker.
- [ ] FCM, OAuth, email recovery, protected media and backend migrations are live-verified.
- [ ] Exactly one consolidated two-phone/real-network acceptance completed for the final candidate.
- [ ] Production signing, Play submission/policy/support/deletion and moderation operations approved by owner.

Reference [PRD](PRD.md), [ARCHITECTURE](ARCHITECTURE.md), [DESIGN](DESIGN.md), [MEMORY](MEMORY.md), [RULES](RULES.md) for intent and guardrails; use detailed existing trackers for actual requirement closure.

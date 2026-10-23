# Liquid Chat — Project Memory & Handoff

**Updated:** 2026-10-23  
**Purpose:** durable **repository context** for maintainers and coding agents. This is not a record of private conversations or personal user data.

## Project identity

- Repository: [officialpritam007/Project-x](https://github.com/officialpritam007/Project-x), default branch \`main\`.
- Product: **Liquid Chat**, native Kotlin/Compose Android app, package \`com.pritam.liquidchat\`, minimum API 29.
- Current Gradle snapshot: \`1.0.0-rc12-liquid-preview\` / versionCode \`19\`; debug candidate, **not production-certified**. Verify before future edits.
- Infrastructure: existing Supabase Free project, private Storage/Edge/RLS/RPC; FCM push; optional authenticated Cloudinary; local account-scoped Room and WorkManager; experimental one-to-one WebRTC calls.

## Durable decisions

| Decision | Rationale / guardrail |
|---|---|
| Preserve application ID, existing Supabase identities/data, and source provenance | No unapproved migration, app identity reset, or destructive replacement |
| Maintain the original Liquid Chat brand and selectable Liquid Glass appearance | Reference layouts inspire behavior; do not misrepresent another product |
| Calling is experimental/current scope; groups/Communities are not automatically supported | Newer owner tracking takes precedence over the old PRD calling exclusion; membership/RLS/backend are required for real groups |
| FCM transports push only | Not a delivered/read acknowledgment |
| Protected Supabase Storage stays available; Cloudinary optional | No public attachment fallback and no paid upgrade without authorization |
| Do not claim E2EE | This repository has no verified E2EE implementation |
| Truthful verification over percentage completion | Source present, CI passed, device-verified and production-approved are different evidence levels |
| Preserve and cross-reference existing trackers | 243 PDF IDs and 346 engineering IDs form distinct inventories; never compress or mark them accepted without proof |
| Repository instructions currently authorize direct-\`main\` changes and testing | Newer README/PDF tracker override older PR-only or no-testing notes; recheck owner instructions before future work |

## Snapshot of recent work (not new acceptance)

- Source contains substantial messaging, appearance, Calls, protected media and Settings work, plus tests and CI workflows.
- [2026-10-23 gap review](IMPLEMENTATION_GAP_REVIEW_20261023.md) identifies implemented source and open integrations; local Android compile was blocked by missing SDK/Gradle download, so current code was not certified by that review.
- [PDF four-phase tracker](PDF_FOUR_PHASE_TRACKER.md) preserves 243 requirement IDs; [engineering feature matrix](mb-parity/FEATURE_MATRIX.md) preserves 346. The Phase 1 UI source is **not** equivalent to acceptance of all Phase 1 checkboxes.
- [Master issue #24](https://github.com/officialpritam007/Project-x/issues/24) tracks the one final calling-enabled test APK and two-phone acceptance. [Issue #91](https://github.com/officialpritam007/Project-x/issues/91) retains an unresolved intermittent Compose teardown failure.
- Provider/owner gates include real FCM/SMTP/OAuth, physical two-phone calls/media/notifications, production signing, Play submission, privacy/deletion/support hosting and operations sign-off.

## How to resume safely

1. Pull and inspect **current** \`main\`, README, open issue #24, [gap review](IMPLEMENTATION_GAP_REVIEW_20261023.md), [tracker](TRACKER.md) and exact open issues; this page becomes stale as soon as code changes.
2. Identify a concrete requirement ID and its acceptance criteria before implementing anything.
3. Inspect source, applied migration inventory, backend privileges and tests. Distinguish source-present from runtime-proven and live-deployed.
4. Make small non-destructive changes; use additive SQL and scoped authorization; keep existing user messages, media and unsent outbox safe.
5. Run exact-head required CI/JVM/lint/Edge/emulator checks and physical/provider validation where possible. Record full failure context instead of hiding flaky tests.
6. Update [TRACKER.md](TRACKER.md), [QA_MATRIX.md](QA_MATRIX.md) and affected release/bug documents with commit, CI run, test result, owner and blockers.

## Memory update format

For every substantive decision, append a dated record with **context → decision → impacted files/requirements → evidence → unresolved risk → next action**. Never put passwords, access tokens, service accounts, private chat text, unredacted user data, phone numbers or sensitive account artifacts here.

### 2026-10-23 — documentation set

Context: project has a detailed original PRD and many scoped audits, but no unified six-document entry set. Decision: maintain [PRD](PRD.md), [ARCHITECTURE](ARCHITECTURE.md), [DESIGN](DESIGN.md), [MEMORY](MEMORY.md), [RULES](RULES.md) and [TRACKER](TRACKER.md) as concise navigation and change-control docs; preserve original inventories. Evidence: source and document review; **documentation-only**, no build or runtime validation claimed. Next: link future exact-head evidence into the tracker.

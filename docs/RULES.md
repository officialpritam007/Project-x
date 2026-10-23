# Liquid Chat — Engineering & Agent Rules

**Updated:** 2026-10-23 | **Applies to:** contributors, maintainers, agents and release workflows

These rules summarize project-specific constraints from [README](../README.md), [original PRD](../Liquid_Chat_PRD-1.md), [PDF tracker](PDF_FOUR_PHASE_TRACKER.md), [master plan](INPUT_MASTER_WORK_PLAN.md) and security/release audits. Re-read current owner instructions before every substantive action; later explicit owner direction wins over older process notes.

## 1. Product truth

- Build **Liquid Chat**, Android package \`com.pritam.liquidchat\`; preserve real features and identity.
- Treat one-to-one messaging, private media and account controls as critical. Calls are currently experimental and require device acceptance. Do not invent Groups, Communities, Channels, GIF services, E2EE or third-party proprietary functionality.
- No fake UI actions, manufactured test results, undocumented silent fallbacks, fabricated delivery/read indicators or production-ready claims.

## 2. Security, privacy and data

- Participant checks, blocks, mutation rights, media eligibility and any calling rights belong on trusted server/RLS/RPC/Edge paths, not only in Compose.
- Never commit, print or transmit service-role credentials, FCM service-account JSON, Cloudinary secrets, keystores, refresh tokens or private user conversations.
- Maintain per-account boundaries across Room, cache, workers, FCM, UI, session refresh and logout. Preserve unrelated accounts' data.
- No destructive live migration, mass deletion, production data wipe, credential rotation or paid infrastructure change without explicit owner authorization.
- Existing deployed migrations are immutable. Add compatible migrations with rollback strategy and isolated, rollback-only authorization tests.
- Protect private attachments with authorized short-lived media access; do not switch to public links to make a feature appear to work.
- Free-tier compatibility remains a constraint, not an unlimited capacity guarantee.

## 3. Code and UX quality

- Keep UI state lifecycle-aware; stable IDs, typed/account-scoped state, single-source decisions and background I/O. Do not do network work during recomposition.
- Preserve durable drafts/outbox/idempotent retries, truthful monotonic receipts, tombstone revocation and scroll/IME stability.
- Follow shared design tokens and real accessibility behavior. Respect reduced motion/transparency, TalkBack, font scaling and permission states.
- Prefer smaller components, focused tests and additive changes over replacing entire subsystems. Do not remove existing features, data or tests to silence failures.
- Do not break Android 10+ or change application ID/signing continuity without an approved migration plan.

## 4. Change workflow

1. Inspect current \`main\`, README, relevant feature code, open issues and [TRACKER.md](TRACKER.md). Current owner instructions in README/PDF tracker permit **direct \`main\`** changes and automated testing; older documents may still prescribe PRs or withhold tests.
2. Select concrete \`PDF-Sxx-nnn\` / \`Pxx-nnn\` requirements; document acceptance boundaries before edits. The two inventories remain separate.
3. Make small focused changes with regression tests. Preserve privacy, stored data and actual code behavior.
4. For executable changes, run the applicable exact-head Android build, JVM, lint, emulator and Edge/SQL checks. Check GitHub Actions when local SDK is unavailable. Do not claim green CI from an older commit.
5. Treat provider delivery, real two-phone interactions and production release credentials as separate external gates. Never describe a compiled candidate as signed production-ready.
6. Record exact commit, CI run link, results, known failures, artifacts, issue references and any unresolved blocker in [TRACKER.md](TRACKER.md), [QA_MATRIX.md](QA_MATRIX.md) and affected audits.
7. Never skip tests, suppress a flaky failure or mark an unmet requirement complete merely to improve the completion percentage.

**Docs-only exception:** documentation edits need review for accurate links and statuses, but do not imply build execution or change implementation acceptance.

## 5. Release invariants

- Keep experimental calls and new media behind accurate user disclosures and permissions.
- Build an installable candidate from **exact verified source**; inspect artifact hash, signature, version, package and provenance.
- Production publication additionally requires owner-controlled signing, provider configuration, two real devices/accounts, Play/policy/deletion/support readiness and moderation/operations acceptance.
- Block release when a critical authorization/privacy issue, failed gate, unresolved migration, missing artifact provenance or unknown signing identity exists.

## 6. Evidence language

Use exactly differentiated labels: **Planned**, **Source present / needs audit**, **Automated verified (commit + CI)**, **Live provider verified**, **Physical-device verified**, **Blocked (name dependency)**, or **Accepted (linked criteria fulfilled)**. A checkbox stays open until its specific evidence exists. When unsure, mark **Needs verification**, not **Done**.

## 7. Document precedence and updates

Original [PRD](../Liquid_Chat_PRD-1.md) provides detailed baseline; [PRD.md](PRD.md) provides current-scope interpretation; [ARCHITECTURE.md](ARCHITECTURE.md) describes the code map; [DESIGN.md](DESIGN.md) defines UI review; [MEMORY.md](MEMORY.md) records decisions; [TRACKER.md](TRACKER.md) points to the canonical exhaustive inventories. Source and tested behavior—not any prose file alone—determine implementation status.

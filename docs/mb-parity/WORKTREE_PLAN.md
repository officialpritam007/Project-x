# Liquid Chat parity — Git worktree operating plan

**Repo:** https://github.com/officialpritam007/Project-x
**Master scope issue:** https://github.com/officialpritam007/Project-x/issues/66
**Release issue:** https://github.com/officialpritam007/Project-x/issues/24
**Known emulator teardown blocker:** https://github.com/officialpritam007/Project-x/issues/91
**Feature matrix:** [FEATURE_MATRIX.md](FEATURE_MATRIX.md) — 346 stable IDs, 12 tracks.
**Manifest:** `scripts/worktrees/tracks.json`.

## How actual worktrees work
GitHub hosts **branches**, not local `git worktree` checkouts. Twelve `parity/NN-...` branches are created from the exact same **planning-main commit**, never from unrelated stale commits. On Windows run `powershell -ExecutionPolicy Bypass -File scripts/worktrees/setup.ps1 -All` at the repository root; on macOS/Linux run `bash scripts/worktrees/setup.sh all`. Both place sibling directories under `../Project-x-worktrees/NN-slug`. For one branch use `-Track 04-chat-actions` or `bash scripts/worktrees/setup.sh 04-chat-actions`.

No automatic cherry-pick or merge occurs. Every worktree has a dedicated branch. If code is added to another branch, do not overwrite it; bring changes in during controlled integration after checking dependencies. GitHub mobile or Work users can work directly on those remote branches without a local checkout.

## Track ownership and dependency DAG

| ID | Branch | Scope | Integration prerequisite |
|---|---|---|---|
| 00 | `parity/00-foundation` | KSP/Compose memory, modularization, CI prereqs, stable shared component contracts | None |
| 01 | `parity/01-shell-home` | Tabs, badges, chat list/filter, search toolbar, home wallpaper and controls | 00 for common UI; coordination for backend |
| 02 | `parity/02-settings-appearance` | MB Preferences, color/fonts/DPI, wallpaper, animations, launcher icons | 00 for common UI; coordination for backend |
| 03 | `parity/03-settings-privacy` | Per-contact options, real preferences, FCM quiet/disable, safe privacy | 00 for common UI; coordination for backend |
| 04 | `parity/04-chat-actions` | Reply/delete/forward/star/pin, reactions, search and delivery states | 00 for common UI; coordination for backend |
| 05 | `parity/05-composer-emoji` | IME, emoji grids, GIF, pack picker, voice pause/resume/lock | 00 for common UI; coordination for backend |
| 06 | `parity/06-media-editor` | Photo/video/PDF processing, sticker cutout and packs, safe upload | 00 for common UI; coordination for backend |
| 07 | `parity/07-calls-audio` | WebRTC signaling, route, active calling, call logs and visual parity | 00 for common UI; coordination for backend |
| 08 | `parity/08-moments-social` | Story UI and privacy, group/channel backend scope and access control | 00 for common UI; coordination for backend |
| 09 | `parity/09-profile-contact-search` | Profile cards, contact controls, global/media search, translation and a11y | 00 for common UI; coordination for backend |
| 10 | `parity/10-backend-security` | RLS, account isolation, deletion, presence, media privacy, API migration | 00 + coordination with 03/07/08 |
| 11 | `parity/11-qa-release` | Source inventory, screenshot baselines, CI and one two-phone verification | All ready tracks |

**Critical sequence:** 00 (fix build/FIR KSP + extract oversized Composables) → other feature tracks in isolated worktrees → 10 backend contracts reviewed before security-affecting UI lands → 11 integrated QA and calling-enabled release.

**Avoid overlap:** only track 00 edits root Gradle and shared design system; 01 owns `LiquidApp.kt` and home navigation; 02/03 own settings modules; 04 owns conversation message actions; 05 composer/emoji/voice capture UI; 06 photo/video/sticker creation; 07 call services; 08 Stories/Posts; 09 contact/profile/global search; 10 Supabase migrations/RPC/FCM and privacy; 11 workflow/test/docs. `ChatScreens.kt` is currently monolithic (~254KB): 00 must extract per-track facades first. Until that extraction is merged, feature tracks should create separate composables/files and small, coordination-reviewed integration hooks rather than all editing that monolith independently.

## Required track handoff
1. Pick inventory IDs; add them to issue and branch PR description.
2. Identify original screenshot/frame/behavior and existing native handler.
3. Implement real state, backend/RLS, keyboard/permissions/error/accessibility.
4. Record changed files, data migrations, privacy/security impact, acceptance tests.
5. Check no current feature was removed; preserve default behavior and user data.
6. Any code-only commit status is `SOURCE-PARTIAL`. Do not mark `CI-GREEN` without exact-head CI.
7. Use short PRs targeting `main`; integration is reviewed in dependency order. No force push, unverified merge or mandatory-check suppression.
8. Do not request repeated owner testing. Final owner phone round occurs only after full integration, exact-merged-main CI, APK provenance and completed checklist.

## Change and scope policy

All UI labels must say Liquid Chat. Keep existing FCM and enabled one-to-one WebRTC calling. Preserve existing Supabase tables/data; every destructive migration needs rollback and authorization review. Free-tier caps apply. No misleading VIP purchase, foreign developer service links, copied messaging accounts, false E2EE labels, anti-delete bypass, view-once bypass, unconsented notification TTS, unsolicited spam or paid-only dependency.

Groups, Channels and Communities are in the matrix and track 08, but need real schema/RLS, storage, moderation, cost and UX scoping before marking completed. If infeasible, record `BLOCKED-BACKEND` rather than pretending a clickable mock is a delivered feature.

## One final release gate

The final gate is exact source SHAs, compile success, debug/release tests, lint, Android API29/35/36 emulator checks, Supabase/Edge/RLS validations, screenshot pairs and **one** two-phone acceptance pass. Sign/align/hash/cert-verify the exact calling-enabled APK from merged main. If a gate fails, fix and rerun it, never distribute a falsely verified final APK. Historical #486 works as an older installable candidate, not a replacement for this parity program.

## Worktree health and maintenance

- Each branch is pinned to the same baseline and has an independent working directory. Prefer scoped module ownership.
- `git worktree list` lists physical checkouts; the manifest lists intended tracks and GitHub branches.
- After safely merging a branch: `git worktree remove <path>` (only with no uncommitted changes), then `git worktree prune`.
- Never remove the main checkout or delete an active feature branch with unmerged commits.

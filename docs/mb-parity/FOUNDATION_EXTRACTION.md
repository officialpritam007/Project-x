# Track 00 — Build and Compose extraction sequence

**Owner worktree:** `parity/00-foundation` · [#114](https://github.com/officialpritam007/Project-x/issues/114) · dependent worktrees 01–10. Baseline `95c49a241f8d365f830dd6578997668e58360dd2`.

**This document is an executable handoff plan, not proof that a build was fixed.** The owner wants one final manual APK/phone acceptance after implementation. No compile, emulator, lint or Android test is performed by creating this plan.

## Confirmed source inventory (baseline source only)

- `app/src/main/java/com/pritam/liquidchat/ui/ChatScreens.kt`: 254,557 source characters, 4,013 lines.
- Top-level composable `ConversationScreen`: ~146,126 characters until next top-level function.
- Top-level composable `SettingsScreen`: ~71,912 characters.
- Top-level composable `ChatsScreen`: ~20,946 characters.
- `build.gradle.kts` / `app/build.gradle.kts` have Room's KAPT → KSP migration on main; compatibility and compile success are not proven.
- Android runs #487, #489 and #490 failed in Kotlin FIR/KAPT stubs with JVM out-of-memory; changing heap alone did not resolve it. The last known successful distributable calling-enabled APK is **older** build #486.

## Refactor slices and file ownership

| Slice | Source extraction | New module candidate | Coordinate with |
|---|---|---|---|
| F0 | KSP and AGP/Kotlin configuration; verify no remaining kapt processors, preserve 17 target and debug calls flag | `build.gradle.kts`, `app/build.gradle.kts`, `gradle.properties` | 11 |
| F1 | Extract pure conversation message presentation only (bubble, date separators, reaction bar), keep data logic intact | `ui/conversation/MessagePresentation.kt` | 04 |
| F2 | Extract message action sheets, dialogs and selection toolbar; keep delete/forward/RLS in existing repository | `ui/conversation/MessageActionSheets.kt` | 04, 10 |
| F3 | Extract composer/emoji/sticker/photo entry-point layout while preserving voice recorder lifecycle and IME focus | `ui/conversation/ComposerSurface.kt` | 05, 06 |
| F4 | Split Settings route selection from General/Home/Conversation/Privacy/Notification content; do not duplicate preference state | `ui/settings/SettingsRoutes.kt`, `SettingsPanels.kt` | 02, 03 |
| F5 | Extract Chats home list and filters without changing Flow/session state owner | `ui/home/ChatList.kt` | 01 |
| F6 | Restore source ownership map and stabilize independent local UI APIs; remove temporary glue only when downstream merged | `docs/mb-parity/...` | all |

## Compiler-safe rules

1. Avoid a second copy of `AppViewModel`/repository state, concurrent Firebase listeners or parallel `collectAsStateWithLifecycle` subscriptions for the same mutable flow.
2. Move leaf composables with explicit immutable arguments and callbacks; do **not** move a deeply nested `@Composable` lambda without first identifying all capture dependencies.
3. Preserve private helper accessibility: Kotlin `private` is **file-scoped**; after moving a helper, either move its callers with it or expose a narrow `internal` bridge. Avoid global wildcard imports that hide conflicting names.
4. Message ordering, room IDs, delete guards, quote privacy, offline outbox and FCM foreground suppression must remain identical.
5. Keep default Reference white/green and original Liquid Glass mode, accessibility focus, stickers, media and both call buttons unchanged unless an explicitly tracked `Pxx` item requires UI change.
6. No generated stubs or output paths should be committed; do not turn off unit/lint/emulator jobs to mask OOM. Statically inspect compiler errors at final gate.
7. Only foundation worktree edits `ChatScreens.kt` during initial extraction. Other branches may implement new independent composables, but integration hooks should wait until these slices are committed/merged to avoid simultaneous modifications.
8. Every slice commit references `P00-001` through `P00-012`, appropriate downstream IDs and an exact changed-file list. **Completion still needs successful exact-head and merged-main CI**, not just a source commit.

## Dependency-unblocking milestones

- [ ] F0: KSP migration compiles on hosted runner without prior FIR/KAPT heap failure.
- [ ] F1: Message bubble UI extracted without altered sender/quote/receipt state.
- [ ] F2: Message action/dialog UI extracted with no private message leaks or dead buttons.
- [ ] F3: Composer shell extracted while retaining voice/call keyboard correctness.
- [ ] F4: Settings page shell separated; 14 reference routes still lead to real options.
- [ ] F5: Chats home extracted without duplicate collectors or row jitter.
- [ ] F6: Track ownership reviewed, branches rebased safely, integration checklist updated.
- [ ] All mandatory regression suites, visual parity checks, API29/35/36 and real phone testing performed **only at approved test gate**.

## Exact acceptance priority

1. **P0:** Build and mandatory CI not red; Supabase/FCM/calls preserved.
2. **P0:** Every original feature remains reachable; no fake switch or cache resurrection.
3. **P1:** Parallel track ownership works without overwriting changes.
4. **P1:** Same-state pixel comparison and performance tuning before final calling-enabled release.

Current status: **PLAN READY, IMPLEMENTATION NOT VERIFIED.**

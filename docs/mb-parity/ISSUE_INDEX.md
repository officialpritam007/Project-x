# Liquid Chat — issue and branch tracking index

Branch baseline: `95c49a241f8d365f830dd6578997668e58360dd2` (12 heads confirmed on 2026-10-22 before workstream-specific commits). All issues are real, linked tracking issues. Every `Pxx-nnn` row in [the feature matrix](FEATURE_MATRIX.md) belongs to exactly one track. **None are marked complete merely because the structure exists.** Owner-provided videos and original reference APK are the requirements evidence; unpublished/proprietary behavior must be researched and reimplemented safely.

Parent issue: [#66 MBWhatsApp parity](https://github.com/officialpritam007/Project-x/issues/66). Release: [#24 final APK](https://github.com/officialpritam007/Project-x/issues/24). CI stability: [#91](https://github.com/officialpritam007/Project-x/issues/91).

| Track | Worktree branch | GitHub issue | Open acceptance items | Scope |
|---|---|---|---:|---|
| 00 | [`parity/00-foundation`](https://github.com/officialpritam007/Project-x/tree/parity/00-foundation) | [#114](https://github.com/officialpritam007/Project-x/issues/114) | 12 | Build/Compose architecture |
| 01 | [`parity/01-shell-home`](https://github.com/officialpritam007/Project-x/tree/parity/01-shell-home) | [#115](https://github.com/officialpritam007/Project-x/issues/115) | 32 | Navigation/Chats home |
| 02 | [`parity/02-settings-appearance`](https://github.com/officialpritam007/Project-x/tree/parity/02-settings-appearance) | [#116](https://github.com/officialpritam007/Project-x/issues/116) | 42 | Settings/themes/appearance |
| 03 | [`parity/03-settings-privacy`](https://github.com/officialpritam007/Project-x/tree/parity/03-settings-privacy) | [#117](https://github.com/officialpritam007/Project-x/issues/117) | 33 | Privacy/notifications |
| 04 | [`parity/04-chat-actions`](https://github.com/officialpritam007/Project-x/tree/parity/04-chat-actions) | [#118](https://github.com/officialpritam007/Project-x/issues/118) | 40 | Messages/actions/search |
| 05 | [`parity/05-composer-emoji`](https://github.com/officialpritam007/Project-x/tree/parity/05-composer-emoji) | [#119](https://github.com/officialpritam007/Project-x/issues/119) | 40 | Composer/emoji/GIF/voice |
| 06 | [`parity/06-media-editor`](https://github.com/officialpritam007/Project-x/tree/parity/06-media-editor) | [#120](https://github.com/officialpritam007/Project-x/issues/120) | 31 | Media/editor/stickers/docs |
| 07 | [`parity/07-calls-audio`](https://github.com/officialpritam007/Project-x/tree/parity/07-calls-audio) | [#121](https://github.com/officialpritam007/Project-x/issues/121) | 23 | Voice/video calls |
| 08 | [`parity/08-moments-social`](https://github.com/officialpritam007/Project-x/tree/parity/08-moments-social) | [#122](https://github.com/officialpritam007/Project-x/issues/122) | 21 | Stories/Posts/Groups/Channels |
| 09 | [`parity/09-profile-contact-search`](https://github.com/officialpritam007/Project-x/tree/parity/09-profile-contact-search) | [#123](https://github.com/officialpritam007/Project-x/issues/123) | 24 | Profile/contact/global search |
| 10 | [`parity/10-backend-security`](https://github.com/officialpritam007/Project-x/tree/parity/10-backend-security) | [#124](https://github.com/officialpritam007/Project-x/issues/124) | 24 | Supabase/FCM/security |
| 11 | [`parity/11-qa-release`](https://github.com/officialpritam007/Project-x/tree/parity/11-qa-release) | [#125](https://github.com/officialpritam007/Project-x/issues/125) | 24 | QA/release |

**Total: 346 items; 12 branches; 12 owner-visible workstream issues.**

- To report completion, link specific IDs, source commit SHA, tested API/device conditions and visual reference evidence.
- Any newly found option must become a new item ID; do not silently drop previously recorded IDs.
- Closing an issue requires every item implemented/verified or explicitly blocked/deferred with owner-facing rationale.
- Integration order, safety invariants, worktree bootstrapping and release gates are in [WORKTREE_PLAN.md](WORKTREE_PLAN.md).

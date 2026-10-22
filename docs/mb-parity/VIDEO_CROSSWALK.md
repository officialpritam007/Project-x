# Screen recordings → track acceptance crosswalk

This maps the 22 October 2026 MBWhatsApp reference recordings to **real, stable feature IDs** in [FEATURE_MATRIX.md](FEATURE_MATRIX.md). R1: `Screenrecording_20261022_155911.mp4` (2:43); R2: `Screenrecording_20261022_164540.mp4` (1:33). Timestamps are **approximate**. A video frame does not demonstrate whether an action persists or works end-to-end.

**Evidence use:** Pick a scene ID, then inspect all feature IDs in its row. The ID's `Pxx` prefix identifies exactly one of the 12 [worktree owners](WORKTREE_PLAN.md). The 346-feature inventory contains extended requirements too; those must **not** be misrepresented as visible video evidence. User recordings and private frames stay out of GitHub.

| Scene | Video | Approximate time | Visible behavior/option group | Stable IDs |
|---|---|---|---|---|
| V01 | R1 | 00:00–00:11 | Profile Settings dashboard and grouped options | `P02-001`, `P02-002`, `P09-001`, `P09-003`, `P02-012` |
| V02 | R1 | 00:12–00:25 | Preferences sections and entry navigation | `P02-002`, `P02-003`, `P02-004`, `P02-005`, `P02-006`, `P02-007`, `P02-008`, `P02-009`, `P02-010`, `P02-011` |
| V03 | R1 | 00:26–00:33 | General font DPI blur colors wallpaper launcher and cache | `P02-015`, `P02-016`, `P02-017`, `P02-018`, `P02-019`, `P02-020`, `P02-021`, `P02-023`, `P02-026`, `P02-027`, `P02-028`, `P02-030`, `P02-031` |
| V04 | R1 | 00:33–00:47 | Home tabs glass layout and row settings | `P01-016`, `P01-017`, `P01-018`, `P01-019`, `P01-020`, `P01-021`, `P01-022`, `P01-025`, `P01-027`, `P01-028`, `P01-030`, `P02-032`, `P02-033`, `P02-034` |
| V05 | R1 | 00:50–00:53 | Multimedia Photo HD Video HD | `P02-009`, `P06-004`, `P06-005`, `P06-006` |
| V06 | R1 | 00:54–01:15 | Conversation bubble styles colors fonts stickers and reactions | `P04-005`, `P04-006`, `P04-007`, `P04-008`, `P04-015`, `P04-016`, `P05-025`, `P05-026`, `P05-027`, `P04-025` |
| V07 | R1 | 01:18–01:30 | Privacy online read receipts typing status/view-once and contact controls | `P03-001`, `P03-002`, `P03-003`, `P03-004`, `P03-006`, `P03-008`, `P03-009`, `P03-011`, `P03-012`, `P03-031` |
| V08 | R1 | 01:31–01:39 | Notifications disable auto-hide DND landscape per-contact alerts | `P03-015`, `P03-016`, `P03-017`, `P03-018`, `P03-021`, `P03-024`, `P03-025`, `P03-026`, `P03-029` |
| V09 | R1 | 01:39–01:47 | Chat list filters unread counts and footer | `P01-001`, `P01-003`, `P01-004`, `P01-005`, `P01-006`, `P01-008`, `P01-009`, `P01-010`, `P01-011` |
| V10 | R1 | 01:48–02:10 | Conversation message selection and reaction context actions | `P04-001`, `P04-004`, `P04-005`, `P04-009`, `P04-011`, `P04-014`, `P04-015`, `P04-017`, `P04-018`, `P04-020`, `P04-021`, `P04-023`, `P04-024` |
| V11 | R1 | 02:10–02:15 | Calls tabs history favorites and keypad | `P07-011`, `P07-012`, `P07-013`, `P07-014`, `P07-015`, `P07-016` |
| V12 | R1 | 02:15–02:30 | Updates status channel and community screens | `P08-001`, `P08-002`, `P08-005`, `P08-006`, `P08-007`, `P08-013`, `P08-014`, `P08-015`, `P08-016`, `P08-017` |
| V13 | R1 | 02:30–02:43 | Contact details and calling/privacy actions | `P09-004`, `P09-005`, `P09-006`, `P09-007`, `P09-008`, `P09-009`, `P09-024` |
| V14 | R2 | 00:00–00:10 | Composer keyboard and emoji tray | `P05-001`, `P05-002`, `P05-003`, `P05-028`, `P04-035` |
| V15 | R2 | 00:03–00:19 | Emoji search recent categories and suggestions | `P05-004`, `P05-005`, `P05-006`, `P05-007`, `P05-008`, `P05-009`, `P05-010`, `P05-011`, `P05-012`, `P05-013`, `P05-014`, `P05-015` |
| V16 | R2 | 00:18–00:30 | GIF and sticker tabs packs previews and create | `P05-016`, `P05-017`, `P05-018`, `P05-019`, `P05-020`, `P05-021`, `P05-022`, `P05-023`, `P05-024`, `P06-014`, `P06-018` |
| V17 | R2 | 00:30–00:53 | Voice record hold cancel lock pause waveform and send | `P05-029`, `P05-030`, `P05-031`, `P05-032`, `P05-033`, `P05-034`, `P05-035`, `P05-036`, `P05-037`, `P05-038` |
| V18 | R2 | 00:53–01:12 | Message selection actions including reactions reply forward copy star delete | `P04-014`, `P04-015`, `P04-016`, `P04-017`, `P04-018`, `P04-019`, `P04-022`, `P04-023`, `P04-024`, `P04-026`, `P04-027`, `P04-028` |
| V19 | R2 | 01:12–01:21 | Contact details and call actions | `P09-004`, `P09-005`, `P09-006`, `P09-007`, `P09-009`, `P03-006`, `P07-004`, `P07-005` |
| V20 | R2 | 01:24–01:33 | Full search and media filters with jump-to-message | `P01-015`, `P04-029`, `P04-030`, `P04-031`, `P04-032`, `P09-015`, `P09-016`, `P09-017`, `P09-018` |

## No-omission handoff

- Each row starts **unverified**, even when old Liquid Chat source has a corresponding switch, because build/device proof and exact pixel matching remain open.
- New settings, control labels, mod APK layouts or user-requested features must add a stable inventory ID and scene evidence if actually observed; never delete old IDs.
- For every implemented option record: original screenshot/frame, its Liquid Chat route, real handler, persistence, authorization, accessibility, error handling, regression coverage and visual comparison.
- Work that requires entirely new Groups/Channels/Communities backend, paid infra or a foreign proprietary service must be explicitly `BLOCKED-BACKEND` or scoped separately, never displayed as a fake functional UI.
- Never restore others' deleted messages/statuses, defeat View Once restrictions or falsely display end-to-end encryption. Safe native equivalents are acceptable.
- Preserve Supabase history, FCM, Liquid Glass option, free-tier, and voice/video calls. The final calling-enabled APK is allowed only after successful exact-main CI and one two-phone acceptance gate.

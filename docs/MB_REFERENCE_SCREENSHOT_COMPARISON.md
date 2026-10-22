# Liquid Chat ↔ MBWhatsApp 3.3.4 — pixel-accurate screen comparison

**Status: SCREENSHOT INPUTS NOT YET AVAILABLE. No pixel-parity percentage is known.**

## Authentic reference and scope

- Original owner-provided `MBWhatsApp_3.3.4_by_stefanoYG.apk` SHA-256:
  `6ef11c28fff9615cc82480116e6c37c088daa7cc06184e66671ff84dda99bc69`
- Original APK is 124,096,338 bytes. Its compiled Android XML/layout resources
  are not runtime screenshots and must not be substituted for golden pixel images.
- Running the original and candidate APKs on the **same model, Android version,
  display density, resolution, font and navigation/status-bar configuration**
  is a prerequisite for defensible screen-level pixel comparison.
- Dynamic contents (timestamps, photos, contact details, voice durations, loading
  states, privacy prompts) need matched benign fixtures and identical UI state.
  Never copy personal chats, private tokens or signed-media URLs into the repo.
- Foreign logos/product brand labels are deliberately replaced with Liquid Chat;
  these intentional identity changes cannot be called exact full-screen parity.
- User requested **no CI, emulator/application test, or final APK run until the
  implementation phase is finished**. Captures and measured results are pending.

## Mandatory paired captures (identical basenames)

| Basename | Original state | Liquid Chat state |
|---|---|---|
| `chats_light.png` | Chat list, fresh with equivalent nonprivate fixtures | Chats tab, Reference mode |
| `chats_dark.png` | Same dark theme | Chats tab dark |
| `chat_empty.png` | Empty 1:1 conversation | Empty private chat |
| `chat_text.png` | Received/sent long and short messages | Same message contents and timestamps |
| `chat_reply.png` | Reply quote and receipt state | Same reply and receipt |
| `chat_reaction.png` | Emoji reaction and long-press preview | Same interaction state |
| `chat_composer.png` | Keyboard visible / multiline input | Same draft, keyboard and IME |
| `chat_attachment.png` | Media actions opened | Reference attachment sheet |
| `chat_media.png` | Image/voice/video/PDF bubble | Equivalent locally owned media |
| `photo_editor.png` | Photo editor with selected crop | Crop/rotate/flip toolbar |
| `photo_album.png` | Multi-image selection and caption | Prepared album dialog |
| `sticker_picker.png` | Sticker selection | Account-local sticker shelf |
| `calls_all.png` | Call log | Calls tab All |
| `calls_missed.png` | Missed filter | Calls tab Missed |
| `calls_detail.png` | Call detail action | Call History action sheet |
| `stories_row.png` | Story carousel | Chats Stories carousel |
| `story_photo.png` | Photo story viewer | Full-screen photo story |
| `story_video.png` | Video story viewer | Private video story |
| `story_editor.png` | Story creation | Liquid Chat story editor |
| `profile.png` | Profile home | My Profile |
| `settings_home.png` | Preference landing | Liquid Chat Preferences |
| `settings_conversation.png` | Conversation settings | Liquid Chat Conversation Screen |
| `settings_privacy.png` | Privacy settings | Liquid Chat Privacy |
| `contact_media.png` | Contact information and gallery | Protected contact media |

More screen-state variants can be added by simply including matching PNG filenames.
Do not pretend unsupported original services (WhatsApp-server linking, channels,
third-party proprietary account functions or message flooding) are Liquid Chat features.

## Exact per-pixel comparison

Install Pillow in a local isolated environment (`python -m pip install Pillow`).
Once **real** paired screenshots exist:

```bash
python scripts/compare_mb_screenshots.py \
  --reference screenshots/mb334 \
  --candidate screenshots/liquidchat \
  --output build/mb-screenshot-diff \
  --tolerance 0
```

The tool refuses to resize screenshots. It creates pixel-difference PNGs,
50/50 overlays and `report.json`, reporting exact differing pixel count,
match rate, mean RGB deviation and dimensions. Use `--ignore-top` and
`--ignore-bottom` only if the captured OS status/navigation bars truly
cannot be aligned, and disclose exclusions in the report.

**Passing requires zero differing RGB pixels** under threshold 0 for each
comparable captured state. The project must not claim 100% UI parity until the
owner's authentic reference screenshots and candidate screenshot pairs pass.

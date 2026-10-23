# Liquid Chat — Product & UI Design

**Updated:** 2026-10-23 | **Design status:** working implementation guidance, not certified reference parity

## Intent

Build a polished, **Liquid Chat-branded** Android experience that combines translucent, rounded Liquid Glass surfaces with reliable readability and native system behavior. The goal is recognizable design language and working interaction, not imitation of another app's proprietary logo, network or unavailable features.

## References and hierarchy

- [Original Liquid Chat Figma integration](FIGMA_UI.md) and its [Figma source](https://www.figma.com/design/VRHamP7Sz4YmHCROClffbW?node-id=0-1)
- [Detailed screen audit](LIQUID_CHAT_FULL_UI_SCREEN_AUDIT.md), [MB reference audit](MBWHATSAPP_334_REFERENCE_AUDIT.md) and [screenshot comparison](MB_REFERENCE_SCREENSHOT_COMPARISON.md)
- [Original PRD](../Liquid_Chat_PRD-1.md) and [current owner PDF tracker](PDF_FOUR_PHASE_TRACKER.md)

Later owner PDF/reference requirements add extensive navigation/Settings/appearance parity. Do not relabel planned routes as working screens before real backend and UI integration.

## Visual foundations

| Element | Design guidance |
|---|---|
| Palette | Original ice-blue/pearl Liquid Glass style remains selectable. Green-accent reference variants must not remove original branding/theme. |
| Surfaces | Rounded cards, low-contrast strokes, restrained shadow and translucent chrome; message text stays on readable surfaces. |
| Blur | Live Haze backdrop where available; Android 10/11 readable scrim; opaque reduced-transparency fallback. Never blur foreground text. |
| Spacing | Use a consistent 8/12/16/24/32 dp rhythm, including safe-area insets; adapt to font scaling and screen width. |
| Shape | Role-consistent rounded chat bubbles, sheets, pills and navigation; use saved/available bubble presets consistently. |
| Typography | Clear hierarchy; preserve layout and tap access at 200% text scale. Imported fonts must fall back safely. |
| Feedback | Visible loading, empty, offline, retry, permission-denied and error states; meaningful disabled and in-progress states. |
| Motion | Short, purposeful transitions; honor reduced motion and avoid unwanted chat-scroll repositioning. |

## Navigation and screen contracts

**Existing source routes** include Chats, Calls, Contacts, Settings and Profile with a conversation view, Stories/Posts and supporting screens. The newer PDF reference requests Updates/Calls/Communities/Chats/Profile. This is a **gap**, not permission to show a fake Communities tab. Route IDs and hidden destinations must remain reachable under visibility/reorder preferences.

- **Chats/home:** clear list of authorized conversations, useful filters, unread states, recent Stories where available, compose/new-contact action.
- **Conversation:** unambiguous sender bubbles, timestamp/receipt status, accessible composer and attachment flow, local/offline behavior, anchored scroll and error/retry states.
- **Calls:** experimental one-to-one call history/action/ongoing controls; visually clear device permission, mute, audio-route and call-end behavior.
- **Contacts:** saved contacts, explicit username lookup, correct empty/error states.
- **Settings/profile:** real account and persisted appearance/privacy/notification/storage controls; placeholders must be labeled unavailable or omitted.
- **Media & Stories:** fit/scale responsibly, preserve aspect ratio, privacy-aware viewers and truthful loading/revocation states.

## Interaction invariants

1. A tap that looks actionable must navigate, mutate permitted state or explain why unavailable—never silently do nothing.
2. A send does not clear newer draft text; failures preserve recoverable user work. Keep keyboard focus stable when sending.
3. No UI state may imply delivery/read/privacy permissions without backend evidence.
4. Protected media must respect revoked access; deleted or hidden content must not flash during cache refresh.
5. Keep accessibility semantics, sensible focus order and touch-target sizing across compact and large-screen layouts.
6. Treat theme import/export as appearance data, never an export of tokens, chats or personal secrets.

## Review checklist

For each screen: compare same-state reference screenshot and live Compose render; inspect light/dark/custom, reduced transparency/motion, 200% font, keyboard/gesture insets, empty/loading/error/offline states, actual navigation, account switch and permission denial. Add an automated UI/semantics test where feasible. Record artifact/commit/device and any mismatch in [TRACKER.md](TRACKER.md) and the [UI audit](LIQUID_CHAT_FULL_UI_SCREEN_AUDIT.md). No pixel-perfect or physical-device acceptance claim without corresponding evidence.

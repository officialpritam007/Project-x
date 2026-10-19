# Quick reactions and chat-list preview privacy

This change is additive to merged PR #78 and deliberately retains its deletion safety.

- MB Conversation Settings: choose one to six quick-reaction emojis from the seven allowed by the existing Supabase message_action RPC. Existing six defaults stay unchanged on upgrade. Selection is persisted locally and reflected in the authenticated message reaction menu. Export/import of Liquid Chat JSON themes includes the validated reaction list.
- MB Home Screen: separately hide last-message snippets in inbox rows; this does not erase messages, modify read receipts, reveal blocked content or change FCM push notification preview settings. This local privacy toggle is not exported in the appearance theme.
- Neither feature changes WebRTC calls, public Supabase schema, FCM service configuration, payments or the current validated per-chat deletion cleanup.

## Owner's single two-phone acceptance round
- Select an emoji palette with five supported emojis including clap; receive a text and react/retract on each device, verifying remote persistence.
- Reject a seventh emoji and reset the palette; restart and confirm the previous valid setting persists.
- Import/export the selected palette in local theme JSON and reject unsupported values.
- Hide inbox snippets, switch account, receive a new text/photo and reopen Chats: no content preview in list, no changed actual chat history, and notifications still use separate settings.
- Delete a scheduled-message chat with a custom notification sound and wallpaper; verify all old account/chat private resources are cleaned without touching another conversation.

CI requires build/JVM/lint/backend plus emulator API29/35/36 on this exact combined branch and again on merged main. Full MBWhatsApp app parity is not claimed.

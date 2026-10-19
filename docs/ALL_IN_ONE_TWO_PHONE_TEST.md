# Liquid Chat — one comprehensive two-phone acceptance round

Run this once with the final **calling-enabled All-in-One Test APK** identified in [Master Tracker #24](https://github.com/officialpritam007/Project-x/issues/24). Development CI and emulator tests are performed before this round. These 76 cases are physical/provider acceptance checks for the Option 3 calling-enabled rc5 candidate, not assertions that emulator CI proves real device behaviour.

## Prepare both phones

- Record APK version, source SHA, SHA-256/certificate and Actions run from the final report. Use the same artifact on both phones; never substitute an unsigned AAB or an older APK.
- Record handset, Android/API version, carrier, Wi-Fi and battery-saving settings. Ideally include an older supported phone and a recent Android 15/16 phone.
- Use two different accounts **A** and **B**. Use test conversations and media; keep existing personal data intact. Save each other as contacts for Stories/Posts. A third disposable account is optional for audience-denial checks already covered by SQL.
- Install as an update where certificates match. If Android reports a signing conflict, stop that installation and retain pending messages; do not uninstall/clear personal app data merely to finish this checklist.
- Accept the updated in-app policy. Test denied permissions first where specified, then grant notifications, microphone and camera through user-visible permission prompts.
- Mark every row **PASS / FAIL / BLOCKED / N/A**, with the phone, time, connection and brief evidence. Do not paste passwords, tokens, service-account JSON or private chat content into an issue.

## Identity, storage and rendering

| ID | Exercise | Expected result |
|---|---|---|
| A01 | Cold launch and restore an existing account on A/B | Correct profile/inbox; no crash, previous-account content or endless loader |
| A02 | Create/sign in to a disposable account with email; test invalid credentials | Valid journey succeeds; friendly validation/failure; no accidental duplicate profile |
| A03 | Google sign-in and return callback, cancel once | Correct account/profile; cancellation remains recoverable |
| A04 | Request reset email, complete fresh link; retry expired/used link | Real mail delivery works; valid reset succeeds; stale/replayed link fails safely |
| A05 | Switch Chats/Calls/Contacts/Settings/Profile repeatedly in light/dark | Liquid Glass and icon-only navigation retained; stable cached photo/call/Post state |
| A06 | Enable 200% font, reduced motion and reduced transparency; use TalkBack | Main actions reachable, labels readable/announced, no duplicate inbox gesture target |
| A07 | Tap and long-press an inbox row | Exactly one navigation or one contextual menu; no double open |
| A08 | Edit name/bio/avatar, return between tabs; remove avatar | Change propagates; cached old avatar is invalidated; no continuous reload |
| A09 | Toggle Favorite/Archive/Pin/Mark unread and use all inbox filters | Correct rows and indicators; manual unread appears even with zero unread count |
| A10 | Lose network while a previously loaded profile/feed is visible | Cache stays usable with honest retry/error state; no empty full-screen reload loop |
| A11 | In Data & Storage, cancel/confirm local image-cache clearing with an offline draft/outbox item | Cancel preserves cache; confirmation clears only local image cache while drafts, queued sends and remote media remain safe |

| A12 | Open contact details in light/dark and 200% text; scroll hero/tabs and close | Save/close/Media/Voice/Starred remain reachable; readable empty state and retry; no controls clipped by system bars |
| A13 | Populate Shared Media with more than one page; switch Media/Voice/Starred, open image and play voice | Adaptive photo grid, full-width cards, correct protected content; pagination, deletion and authorization remain intact |
| A14 | Open conversation/inbox action sheets and Attach; scroll to the final action at 200% text | Labels/icons wrap; exactly one current block action; delete remains distinct and asks for confirmation; Gallery/Camera launch the existing flows |

## Messaging, keyboard, privacy and offline state

| ID | Exercise | Expected result |
|---|---|---|
| M01 | Send rapid text both directions, including Bengali, emoji and long multiline text | Exactly one correct-sender message per submission, correct chronology |
| M02 | Keep Gboard open; send and continue typing immediately | Keyboard/focus remain; new draft is not cleared by an earlier send callback |
| M03 | Type/edit-away/edit-back, change reply target, background/reopen | Exact current draft/reply survives; old submission cannot erase the newer revision |
| M04 | Type on B while A views the chat; stop typing and background B | Typing is in the bubble area, clears appropriately; online/last-seen respects toggles |
| M05 | Send while A/B is offline, restart A, reconnect | Durable queue retries without duplicate UUID/content; no false delivery/read receipt |
| M06 | Force a permanent send failure; Retry and Discard | Failure stays explicit; Retry works once; discard does not later resurrect the item |
| M07 | Scroll far back in a long timeline while B sends and receipts update | Visible history anchor remains stable; no jumps or full timeline reload |
| M08 | Load older messages and Jump to latest | Pagination works; latest action reaches new messages; no wrong-sender results |
| M09 | Reply/swipe-reply and tap quoted message; expand/collapse long text | Correct quote/target and content-sized bubble; read-more works |
| M10 | Edit/react/star/unstar; revisit after restart | Correct authorized mutation/catch-up on both phones |
| M11 | Copy and multi-select-forward text to another existing one-to-one chat | Selected text/order retained; destination/sender correct; no group creation |
| M12 | Search text with Sender/Type/Date filters, including query changes mid-search | Only matching downloaded history; no stale/foreign/deleted result; jump navigates correctly |
| M13 | Delete-for-everyone a text/photo/voice on A; restart/reconnect B | Tombstone only; body, quote, reaction, attachment and search content stay absent |
| M14 | Play cached voice on B and old uncached voice from Shared Media; scroll offscreen, then delete/hide it | Both playback paths stop when deletion/hide reaches B; old gallery item disappears without retaining its private body in Room |
| M15 | Delete-for-me an item/history, restart and sync both devices | A's hidden content stays hidden; B's own copy follows its intended independent visibility |
| M16 | Toggle receipts/online/last-seen privacy; inspect from the peer | Hidden state not inferred from heartbeat; read/delivery labels match permitted watermarks |
| M17 | Block in either direction; attempt messaging/calling/media/Stories/Posts | New unauthorized actions/access denied; unblocking recovers permitted access |
| M18 | Long-press sent/failed/deleted messages; check six reactions, Reply/Forward/Copy and Delete | Quick reactions, actions and safety stay clearly separated; >=48dp targets; destructive confirm remains; no deleted body/media leaks |

## Notification acceptance

| ID | Exercise | Expected result |
|---|---|---|
| N01 | B sends to A's visible active chat | Message appears; no extra system notification now or after leaving the chat |
| N02 | B sends while A views another chat, Home, screen lock and after process restart | One authorized alert where Android permits it; correct account/contact/content |
| N03 | Reconnect after intermittent network and wait through retry intervals | No repeated old-message alert; bounded retry does not create notification loops |
| N04 | Tap message notification; delete/mute/block content before a delayed push | Correct chat opens; fresh server authorization suppresses deleted/muted/blocked content |
| N05 | Deny notifications, then re-enable and restore/login/token rotation | No crash; registration recovers; token/device delivery verified on both phones |
| N06 | Log out A while a push is pending; sign into another account | No cross-account alert/content or registration to a stale login |
| N07 | Exercise Doze/OEM battery saver, app restart and phone restart | Record delivery delay/restriction honestly; no guaranteed wakeup claim |
| N08 | Deny/re-enable Android notification permission, then return to Liquid Chat Notifications | Status refreshes on resume, OS settings shortcut opens, in-app notification previews remain an independent choice |

## One-to-one voice and video calls

| ID | Exercise | Expected result |
|---|---|---|
| C01 | Start voice then video from chat and call-history redial | Call buttons visible/enabled; permission prompts before capture; correct peer/direction |
| C02 | Deny/revoke mic or camera permission then retry from settings | Clear recoverable permission UI; no unauthorized capture/service crash |
| C03 | Inspect outgoing/incoming/ringing/connecting/active states; accept from each phone | Correct identity/direction and one active call; connected timer begins after media connects |
| C04 | Talk alternately and simultaneously for at least two minutes, both directions | Actual voice on both phones; acceptable echo/volume; not just a connected label |
| C05 | Video call with both cameras; switch front/back, mute mic, camera off/on | Real remote frames and audio; controls match actual state; no permanently black sink |
| C06 | Drag/swap self-preview, hide controls, minimize to floating video/pill, return | Call/engine identity preserved; controls/insets remain reachable; no duplicate audio |
| C07 | Select earpiece/speaker/wired/Bluetooth, attach/detach headset | Actual selected output matches UI; record unsupported/OEM cases |
| C08 | Receive a cellular/audio-focus interruption during active call | Safe call termination/cleanup; no silent background mic/camera capture |
| C09 | Decline, caller cancels, unanswered timeout, immediate redial | Both UIs converge; no stale ringing or duplicate history/notification |
| C10 | Start calls simultaneously or overlap an outgoing call with an incoming invitation | Busy/one-call rule; delayed invitation cannot replace current call |
| C11 | Incoming call while Home, locked, process restarted; open Answer/Decline notification | Fresh server validation and correct action where Android allows; no mic/camera FGS from push |
| C12 | Background an accepted/outgoing call, reopen, press notification End | Active service notification visible; permitted continuity; End stops capture on both ends |
| C13 | Remove task, terminate active service, log out/account-switch during call | Local media stops; no stale callback/renderer/account leakage; peer ends or times out |
| C14 | Brief network loss, Wi-Fi↔mobile, airplane mode/reconnect | Recovery or bounded honest failure; no indefinite connecting/capture after ending |
| C15 | Repeat voice/video on same Wi-Fi, different Wi-Fi and separate mobile carriers | Record actual success/failure. **STUN-only NAT failure is a known limitation, not a passing test.** |
| C16 | Review missed/answered/incoming/outgoing history, filters, paging, redial | Correct peer/kind/result, stable cache and no continuous loading |
| C17 | Return after every ended call; inspect Android mic/camera indicators | Capture indicators stop; later calls still acquire audio/video resources |

## Media, Stories and Posts

| ID | Exercise | Expected result |
|---|---|---|
| S01 | Send gallery/camera image with caption; view/zoom/shared-media | Correct bounded image, authorized thumbnail and recipient access; real upload progress |
| S02 | Interrupt upload, restart, retry; exceed a documented quota/format limit with disposable media | Recoverable failure; original/queued file retained where intended; no false Sent or extra charge setup |
| S03 | Hold/release voice, slide-cancel, hands-free lock/discard/send | Actual playable recording; cancellation leaves no queued phantom; mic stops on background |
| S04 | Play/pause/seek voice at 1×/1.5×/2×, scroll away/back | Correct speed/state; one player; chat/account/background transitions release it |
| S05 | Scroll Shared Media/Voice/Starred while unrelated text arrives; switch tabs/load older; delete an item during a delayed page load | No needless full reload, duplicate/reordered page, restored deleted item or stale account result |
| S06 | Publish photo Story with caption; inspect horizontal carousel/full-screen viewer | Loaded image gets viewing time; progress/tap navigation/hold-pause works |
| S07 | Publish video shorter than 30s and trim a longer video; include portrait/landscape/ultra-wide | Local bounded MP4 export, real preparation/upload progress, playback and mute/unmute |
| S08 | Tap previous on first video, navigate between photo/video, background or receive call | Video restarts correctly; Story playback pauses; no audio/video continues after closing |
| S09 | Delete Story and return between viewer/carousel; inspect expired Story after 24h | Mutation refresh and server expiry deny stale content; cleanup may follow on schedule |
| S10 | Peer not saved by creator, removed contact and both-way blocked audience | No unauthorized Story/Post feed or fresh signed URL access |
| S11 | Create Post, refresh, archive, restore, delete and paginate | Correct private feed, stable thumbnails and mutation refresh; no permanent empty/loading placeholder |
| S12 | Keep media visible through signed-link renewal and then revoke access | Stable permitted image, bounded retry on transfer error, eventual revocation at link/authorization expiry |
| S13 | Submit a disposable content report and block | Correct private reference/reason; no public report evidence or privilege escalation |
| S14 | Star a text, photo and voice message, open Contact > Starred, then delete/revoke media | Correct text, authenticated photo preview and voice player; deleted/revoked content never resurfaces |

## Final cleanup and result

| ID | Exercise | Expected result |
|---|---|---|
| R01 | Offline logout after explicitly resolving/discarding test unsent items | Local session/cache/media/notifications erased; remote revocation uncertainty stated |
| R02 | Log in as another account, then back as original | No prior-account cache/search/avatar/voice/call state leaks or hidden-message resurrection |
| R03 | Delete only a disposable account with recent reauthentication | Profile/device/session access revoked; cleanup follows workflow; recipient-text exception understood |
| R04 | Reopen/restart both phones after the full round | No crash/ANR/endless loader, stray capture or duplicate old notifications |
| R05 | Record any failure with the exact case ID, device/network, time and minimal sanitized evidence | One consolidated report; no repeated development-phase installations |

Release remains a test candidate until applicable rows pass. A failed provider/network case is recorded as failed/blocked, never converted to production acceptance by emulator CI. Seen/unseen Story receipts, full Telecom, TURN, arbitrary documents, automatic link previews and conditional P2 features listed in the implementation report are not present and must not be tested as implemented.

## Additional MBWhatsApp parity acceptance (rc6 candidate; do not skip prior cases)

**One owner round only, after automated integration is green.** The reference APK's actual visible UI must be used for side-by-side comparison. Visual differences or reference features that are not present remain **incomplete** and cannot be signed off merely because the app launches.

| ID | Exercise | Expected result |
|---|---|---|
| MB01 | Navigate Settings > MB Preferences > General/Home/Conversation and Back | No blank/inert screen, focus trap, or overlapping modal/navigation |
| MB02 | Change Haze blur 0/55/100, background light/dark, restart | Blur changes live; 55 matches prior baseline; reduced-transparency override works |
| MB03 | Select Cyan/Blue/Green/Purple/Rose/Amber global colors, restart | Correct visible Material3 accent and persisted selection; no washed-out content |
| MB04 | Change bubble shape Tail/Rounded/Compact, five colors and body 14–22sp | Both incoming and outgoing bubbles change, original message/ticks/actions remain |
| MB05 | Toggle bottom-tab labels, icon/label/title sizes at 200% system font | All five destinations remain reachable, no clipping, no calling buttons hidden |
| MB06 | Set profile name as home title; separately scale chat names/previews | Correct authenticated identity, bounded text, no cross-account leakage |
| MB07 | Hide/re-enable Archive filter while a chat is archived | Archived data persists; filter disappears/reappears; no lost conversation |
| MB08 | Enable Ghost Mode from home, compare Privacy screen and second phone | Server hides online/last seen and future read receipts; no false retroactive changes |
| MB09 | Disable Ghost Mode; turn one privacy switch back off manually | Both controls restored then manual half-state is not mislabeled Ghost Mode |
| MB10 | Schedule text 2+ minutes ahead in an existing chat | Listed as scheduled, not in active outbox and not received early |
| MB11 | Cancel a scheduled message | Removed from list and not sent after due |
| MB12 | Reboot sender before due, leave receiver active | Scheduled message persists locally; delivered no earlier than due, possible system delay noted |
| MB13 | Go offline before due, reconnect afterward | Normal queued outbox retries; no duplicate message UUID / no phantom delivery |
| MB14 | Sign out or delete conversation with a pending schedule | Pending local schedules are removed and cannot be sent later from old account |
| MB15 | Switch between two accounts while a schedule exists | No cross-account schedule/body disclosure or send under wrong session |
| MB16 | Send normal text while future schedule exists | Immediate send unaffected; scheduled text stays pending until due |
| MB17 | Verify voice/video call buttons, outgoing/ringing/active audio-video after styling changes | Calling-enabled config unchanged; no UI overlay or audio-route regression |
| MB18 | Compare actual MBWhatsApp reference screens at matched device widths | Record all missing controls, icons, layout/animation mismatches; never claim pixel parity without evidence |

**Known limitations:** schedules are device-local and WorkManager is subject to Android's timing restrictions; logout/uninstall/data clearance removes them. Real carrier TURN reliability, precise theme/font import/export, WhatsApp-specific status/once-view retrieval, document scanning, custom stickers and more reference menu items are still unimplemented. Do not mistake the rc6 candidate for the full MBWhatsApp clone.

## MB per-contact notifications (include in the owner's one final two-phone round)

| ID | Exercise | Expected result |
|---|---|---|
| MB19 | Open conversation > Custom notifications > enable separate channel | Android creates a chat-specific channel without mutating global Messages channel |
| MB20 | Open Android channel settings, choose a different sound/vibration, send three fresh FCM messages from peer | Distinct per-chat user-chosen channel, only one alert per message, no duplicates |
| MB21 | Set conversation muted on server; send a fresh message | No unauthorized incoming alert; custom channel cannot bypass mute |
| MB22 | Disable per-chat toggle, send another fresh message | Falls back to global Messages channel; user's previous channel settings not silently reset |
| MB23 | Account switch/logout and inspect Android notification channels | No prior account's contact labels remain, no alert routed to stale user channel |
| MB24 | Block notifications at OS level, send a message | Android block is respected; no app bypass of global/channel permission |
| MB25 | Receive voice/video call with per-chat message sound enabled | Calling channel and incoming-call controls remain independent and functional |

| MB26 | Switch System/Serif/Mono/Cursive font, restart and import/export a theme | All Material3 screen typography updates live; choice persists; no missing glyphs or unreadable chat/navigation labels |


| MB27 | Hide outgoing typing while entering text, then toggle it back on | Peer stops seeing this device's typing signal (including immediately after toggle); incoming typing still works; preference survives restart and is not altered by theme import |


| MB28 | Pick a photo under 10 MB in conversation > Wallpaper, close/reopen, restart | Wallpaper renders below glass messages, persists for same account/conversation, not transmitted to recipient |
| MB29 | Switch photo → built-in palette → My saved photo | Selection is reversible without unexpected message/data loss |
| MB30 | Sign out of one account and log in to a different user | Previous user's wallpaper is securely removed from app files and cannot be shown to the other account |
| MB31 | Reject oversized, invalid, or non-image gallery selection | Visible error; current chat wallpaper and outbox remain unchanged |


| MB32 | Choose an image for Chats home wallpaper, switch tabs and reopen app | New wallpaper renders only under Chats, remains legible in dark/light, never transmitted or uploaded |
| MB33 | Remove Home wallpaper, then choose a second photo | Background immediately reverts to Liquid Glass, replacement shows latest photo without stale Coil image |
| MB34 | Open home gallery and switch account before returning | Gallery result cannot overwrite the second account's home image or resurrect logged-out account data |


| MB38 | Home photo selected: change dimming 0/50/100, restart and switch dark mode | Veil updates without reloading full inbox; 50 preserves previous defaults; text remains selectable |
| MB39 | Conversation photo selected: adjust separate dimming slider, export/import theme | Only chat photo changes, Home remains independent, values persist and roundtrip |
| MB36 | Set chat-specific photo, custom notification channel and scheduled text, then delete that chat | No residual photo, sound/channel contact name, draft, schedule or stale appearance; other chats and Home wallpaper stay intact |
| MB37 | Simulate failed server chat-deletion request | Existing local schedules, wallpaper and notification preferences remain intact and retryable |


| MB36 | Customize colors, fonts, bubbles, reactions and wallpapers; turn on screenshot/typing/snippet privacy, then MB Themes > Restore defaults | Visual defaults restored and persisted, private wallpapers/files and messages untouched; screenshot protection, typing privacy, hidden previews and accessibility flags remain enabled |

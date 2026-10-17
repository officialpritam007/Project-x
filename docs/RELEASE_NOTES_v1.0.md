# Liquid Chat v1.0 candidate — unreleased

Production stabilization in progress. Existing one-to-one Liquid Glass UI, Supabase core, Firebase FCM and protected media fallback retained. No paid service, groups or calls enabled. PR #2 keyboard-focus fix is already on main.

Not a production approval. Signed AAB, policy URLs, moderation and two-device provider acceptance remain release gates. Do not uninstall an existing build with pending messages; verify signing certificate/update compatibility first.

Phase 1 candidate: protect newer drafts/captions during queue completion, preserve accepted message status during worker/sync races, and check retries atomically. Pending CI; successful real-phone IME/FCM testing remains required. Server FCM credential is currently missing (confirmed 503 path), so background push is a release blocker.

Phase 2 candidate: local sign-out clears this device even if server revocation fails, reports that limitation honestly, and provides explicit offline-profile recovery feedback. Provider auth/FCM and real-device acceptance remain release gates.

Phase 1 and Phase 2 CI passed and both fixes merged. Phase 7 candidate adds User Policy acceptance before sharing, message/Story/Post reporting and author block controls, preserving existing glass styling. New reporting SQL passed prototype rollback tests; owner moderation operations still require setup.

Privacy follow-up: sign-out clears camera scratch and image disk cache and reports incomplete local media removal. The additive private reporting RPC passed deployed rollback tests; no user report data was created or read.

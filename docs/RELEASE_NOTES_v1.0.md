# Liquid Chat v1.0 candidate — unreleased

Production stabilization in progress. Existing one-to-one Liquid Glass UI, Supabase core, Firebase FCM and protected media fallback retained. No paid service, groups or calls enabled. PR #2 keyboard-focus fix is already on main.

Not a production approval. Signed AAB, policy URLs, moderation and two-device provider acceptance remain release gates. Do not uninstall an existing build with pending messages; verify signing certificate/update compatibility first.

Phase 1 candidate: protect newer drafts/captions during queue completion, preserve accepted message status during worker/sync races, and check retries atomically. Pending CI; successful real-phone IME/FCM testing remains required. Server FCM credential is currently missing (confirmed 503 path), so background push is a release blocker.

Phase 2 candidate: local sign-out clears this device even if server revocation fails, reports that limitation honestly, and provides explicit offline-profile recovery feedback. Provider auth/FCM and real-device acceptance remain release gates.

# Liquid Chat v1.0 candidate — unreleased

Production stabilization in progress. Existing one-to-one Liquid Glass UI, Supabase core, Firebase FCM and protected media fallback retained. No paid service, groups or calls enabled. PR #2 keyboard-focus fix is already on main.

Not a production approval. Signed AAB, policy URLs, moderation and two-device provider acceptance remain release gates. Do not uninstall an existing build with pending messages; verify signing certificate/update compatibility first.

Phase 1 candidate: protect newer drafts/captions during queue completion, preserve accepted message status during worker/sync races, and check retries atomically. Pending CI; successful real-phone IME/FCM testing remains required. Server FCM credential is currently missing (confirmed 503 path), so background push is a release blocker.

Phase 2 candidate: local sign-out clears this device even if server revocation fails, reports that limitation honestly, and provides explicit offline-profile recovery feedback. Provider auth/FCM and real-device acceptance remain release gates.

Phase 1 and Phase 2 CI passed and both fixes merged. Phase 7 candidate adds User Policy acceptance before sharing, message/Story/Post reporting and author block controls, preserving existing glass styling. New reporting SQL passed prototype rollback tests; owner moderation operations still require setup.

Privacy follow-up: sign-out clears camera scratch and image disk cache and reports incomplete local media removal. The additive private reporting RPC passed deployed rollback tests; no user report data was created or read.

Policy/reporting PR #6 passed CI and merged. Release-candidate phase targets API36, adds minified AAB and debug/release QA, API29/35/36 instrumentation, artifact provenance/native compatibility checks and a manual owner-key signing workflow without publishing. Branding icons/store assets and policy/Play/operations drafts are included. Candidate CI must pass before merge; signed/device/provider/Play gates are still open.

PR7/API36 and PR8/development-signing correction passed CI and merged; main62e7337 passed all five jobs in run39045292103. Source-selected tests and exact debug APK installation/launch on API29/35/36 passed. Owner signing and preserving baseline upgrades, live push/auth/deletion/provider and physical/Play gates remain open.

P1 follow-up candidate: explicitly exclude local messages/drafts/preferences/media from OS backups and transfers, and prevent smoke setup from touching physical or unacknowledged devices. Gradle distribution checksum is pinned and CI invokes the verified wrapper. This does not remove local data or add export/restore. The old device remains necessary for unsent local content; device migration/export is a separate deferred feature. New phase awaits CI.

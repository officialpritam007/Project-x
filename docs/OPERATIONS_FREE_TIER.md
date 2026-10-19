# Free-tier operating checklist

No billing/subscription upgrade, paid project, SMS service, paid analytics or automatic paid fallback is introduced. Keep Supabase core, FCM and the existing protected media fallback. The project listing/advisors do not prove the organization's subscription or complete monthly usage; owner must verify the actual dashboard/billing state.

Inspect database, storage, egress, Edge invocations, realtime traffic and auth/email provider allowances at 70%, 85% and 95% of the currently published free allowance. At each threshold investigate load/cleanup, restrict optional uploads or defer nonessential usage rather than enabling billing. Current storage fallback code has a serialized 512 MB global / 100 MB finalized-media-per-account budget, with per-file and voice limits; these app budgets are not a promise that all provider monthly egress/invocation quotas cannot be exhausted.

Daily operator review should cover failed/pending notification dispatch, cleanup/erasure backlog, Edge failures and auth failures using aggregate counts without message bodies, tokens or signed URLs. The earlier missing-FCM-credential snapshot is historical: the 19 October review observed recent notify/call-notify HTTP 200, but endpoint success or successful pg_cron SQL alone does not prove physical device delivery. Record current observations in #24, and never automatically replay terminal old-message failures into fresh notifications.

Resolve urgent core messaging/privacy failures first; ordinary bugs should receive an owner triage decision within a day. Keep moderation/support ownership and escalation contacts current. Record provider pauses/outages and communicate degraded service honestly. Supabase Free pause/restore and provider changes require owner attention; do not activate a paid replacement silently.

Keep signing backup and database/storage backup encrypted and owner-controlled outside the public repo. Rehearse restoration using an isolated target, compatible migrations and fixture accounts. Backups, quota-alert delivery, physical performance/battery profiling and a real support/appeal contact are not completed merely by this checklist.

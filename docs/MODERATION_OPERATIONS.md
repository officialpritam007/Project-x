# Moderation operations — owner setup required

Operator: Pritam Pal. App-side User Policy acceptance is versioned and account-scoped on this device, before the profile/messaging/upload UI. It is cleared on local logout/deletion. This is a client consent gate, not a server attestation; older clients do not implement it. Public release still requires operator acceptance of this process, support contact, hosted final privacy/deletion pages, policy review and actual device acceptance.

Users can report contacts or individual incoming accepted messages, report visible Stories/Posts, and block authors. Existing private report storage stays inaccessible to application roles. Message evidence uses the already deployed participant-validated p_messages RPC. A new additive report_moment RPC verifies an active session, audience, both-way block, owner, expiry/archive and ready media, and binds story/post ID server-side in the reason prefix. It shares the existing two-reports/minute limit. No report or private user content was inspected during implementation; fixtures roll back.

## Owner workflow

Use the Supabase SQL editor as the project owner, not an app client or publicly shared dashboard. Do not give moderators service-role keys in the APK or public repo.

```sql
-- Private owner console only. Never paste returned user content in public issues/logs.
select id, actor, target, reason, message_ids, created_at
from private.reports
where resolved_at is null
order by created_at
limit 50;
```

For selected message IDs, inspect only the reported conversation records required for that case. A moment report's server-bound `[story:UUID]` or `[post:UUID]` prefix identifies its private.moments row and associated private image. Do not make a public bucket or issue permanent media links. An expired/deleted reference may no longer be available; do not claim immutable evidence capture.

1. Review urgent exploitation, credible threats and imminent harm immediately; follow applicable reporting obligations with qualified advice. Review ordinary reports within 24 hours, recording decisions privately.
2. Establish context and minimize access to unrelated private conversation content. Protect reporter identity from the reported user.
3. Apply a proportionate remedy in the owner console, following a separately reviewed action procedure. Content removal or account suspension affecting real users requires explicit owner approval; this implementation does not automate destructive moderation.
4. Record the case decision privately, then mark only that case resolved using a parameterized owner-console query. Do not bulk-delete reports.
5. Provide a real support/appeal contact in the final store listing/privacy policy. Keep access and retention proportionate; resolved reports are removed after 90 days by operational retention; unresolved reports remain pending review. Validate this retention and the appeal process before public release.

## Additive migration backup / recovery

Preflight verified report_moment did not exist. The new migration only creates two functions with restricted grants; no existing table/data/function is changed. Its checked-in SQL is the complete source backup. Rollback means disabling the new client controls first and then revoking/dropping ONLY these new functions after owner review, retaining private.reports rows. Existing message/contact reporting is unaffected. No rollback is executed automatically and no applied migration is edited.

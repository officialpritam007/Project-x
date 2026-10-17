# Account deletion acceptance

## In app

Settings → Account → Delete account. Reauthenticate with password or Google; the server requires a session created within five minutes. Confirm irreversible deletion only for an isolated fixture account during QA. The account Edge Function queues tracked cleanup, attempts global session revocation, removes owned media/auth identity and reports pending cleanup honestly. Sent text can remain in the recipient's history under Deleted account. Test partial provider failures and retry completion before public release.

SQL authorization tests covered recent-auth checks and tracked erasure with rollback fixtures; this is not evidence of real provider/Auth/media destruction. No production user was erased in this work.

## Required external route — BLOCKED on owner configuration/hosting

Owner must provide a public HTTPS page with:

1. Liquid Chat / Pritam Pal identification and a prominent deletion request action.
2. A real private support request or self-service authenticated route that works without the app installed, including Google-only accounts.
3. Identity verification without asking the user to disclose passwords/tokens to an operator.
4. A clear account/owned-media scope, retained recipient text and security/report retention explanation.
5. Request acknowledgement and a real operator completion/support process.
6. Checks for mobile accessibility, no app-install requirement, working links, correct provider redirects and replay/expired request handling.

No placeholder email/form is presented as a working path. The owner must host, exercise and enter the URL in Play Console Data Safety. A raw draft or a dead contact form does not satisfy this gate. Do not collect deletion requests in public GitHub issues.

Reference: https://support.google.com/googleplay/android-developer/answer/13327111

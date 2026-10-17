# Release gates — do not publish yet

## Candidate engineering

- [x] Original plan read, latest repo audited, PR #2 found already merged with passing CI.
- [x] Phase 0 PR #3, Phase 1 PR #4 and Phase 2 PR #5 each merged after all required CI jobs passed.
- [x] Confirmed missing server FCM credential through aggregate 503 evidence; no secret value read.
- [x] Authorization, actions, moments/voice and new content-report SQL rollback tests executed successfully.
- [x] Additive reporting migration deployed without changing existing migrations/data; pre-existing advisors documented.
- [x] Policy/privacy follow-up PR #6 CI passed and merged.
- [ ] API36/AGP8.10.1 release candidate PR CI pass: debug/release tests/lint, R8/AAB, Edge, API29/35/36 instrumentation, signing/alignment/provenance.
- [ ] Main push CI pass for the final merged source SHA; inspect reports and download checksum-matched artifacts.

## Owner/provider blockers

- [ ] Set FCM_SERVICE_ACCOUNT_JSON privately in Supabase using the same Firebase project as Android FCM configuration; verify scoped HTTP v1 permissions. Never send it in chat or commit it.
- [ ] Confirm the four public Actions FCM variables; rebuild and verify initialization/token rotation/foreground/background/Doze/terminated delivery on two devices.
- [ ] Verify actual SMTP email confirmation/recovery and Google OAuth PKCE/callback/reauthentication.
- [ ] Confirm actual Supabase Free/billing state and current auth/DB/storage/egress/Edge/realtime quotas. No paid upgrade is authorized.
- [ ] Configure owner-controlled upload signing secrets and offline key backup; run manual signed-candidate workflow only after matching main CI is green.
- [ ] Install/test the signed minified candidate and upgrade path; verify certificate compatibility and preserve pending messages.
- [ ] Complete physical device A/B acceptance: text/image/voice, rapid sends/Gboard/Vivo IME, offline/retry, lifecycle, receipts, delete/hide/block/presence, media revocation, account switching and camera/mic permissions.
- [ ] Operate moderation/report review, support/appeals and data-retention process; finalize actual support contact, age/audience and applicable policy obligations.
- [ ] Host final privacy policy and a functioning external deletion-request path including Google-only accounts; test mobile access and enter URLs in Play Console.
- [ ] Validate Data Safety, app access reviewer login (private), IARC/content rating, ads/audience, listing/icon/graphic and clean-device screenshots.
- [ ] Confirm Play Console access, developer verification/account type/date, package registration and production eligibility. New-personal-account testing may require 12 continuous opted-in testers for 14 days; verify the actual account.
- [ ] Approve the specific signed AAB checksum and requested internal/closed-track upload. Review pre-launch findings afterward.
- [ ] Obtain separate final owner approval for any production rollout; Google review/eligibility is external.

Production: **NO-GO**. Play internal/closed-track upload: **blocked until signed artifact and owner/Console acceptance**. Debug/emulator QA candidate can be reviewed after final CI; this is not physical-device/provider certification.

No Play upload, public rollout, production account erasure, secret rotation or billing change was performed.

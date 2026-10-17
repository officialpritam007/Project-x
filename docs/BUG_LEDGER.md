# Bug ledger

| ID | Severity / classification | Reproduction / evidence | Required fix / acceptance |
|---|---|---|---|
| MSG-01 | P0 ALREADY FIXED, device acceptance open | PR #2 disabled focused field during queue; main keeps it enabled | Physical Gboard/Vivo consecutive sends |
| MSG-02 | P0 SOURCE-CONFIRMED | Edit text/reply while send queues, restore identical text before completion; equality-only callback clears newer revision | Revision-based composer; regression test |
| AUTH-04 | P0 SOURCE-CONFIRMED | Sign out offline: Backend.logout throws before local cleanup; AppViewModel stops | Clear local session/cache/jobs despite remote failure; honest revocation notice |
| BUILD-02 | P0 SOURCE-CONFIRMED | targetSdk=35; October 2026 new app/update needs 36 | Upgrade supported toolchain + API36 CI |
| BUILD-04 | P0 SOURCE-CONFIRMED | No release signing config/workflow | Owner-controlled upload key; fail-closed release task |
| SEC-06/07 | P0 SOURCE-CONFIRMED / partial | Report-user exists; no terms gate or content-report triage completion | UGC terms/content reporting/moderation before public release |
| AUTH-10/SEC-09 | P0 SOURCE-CONFIRMED | No public deletion URL/privacy pack in repo | Prepare accurate drafts; host and test owner route |
| PROVIDERS | P0 BLOCKED BY OWNER/PROVIDER | No physical devices/Play Console/provider acceptance evidence | Complete QA matrix with two accounts |
| SEC-05 | P1 RISK / NEEDS TEST | Auth advisor leaked-password warning | Free-compatible password controls; provider settings review |
| PERF-04 | P1 SOURCE-CONFIRMED cost risk | Active contact state polls every 3s; full sync/min | Measure quotas/battery before changing cadence |
| MSG-04/08 | P0 RISK / NEEDS TEST | SQL watermarks exist; background device delivery unverified | Two-device reconnect/Doze/restart test |
| UI/PERF | P1 RISK / NEEDS TEST | Existing glass/UI emulator tests pass at baseline | High-font/low-end/Android29–36 device acceptance |
| UP-01..15 | DEFERRED P2 | Initial release not stabilized | Preserve separately from release blockers |

No untested feature is classified as a confirmed defect. No open row is implicitly accepted.

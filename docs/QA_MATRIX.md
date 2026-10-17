# QA evidence

| Check | Status | Evidence / limit |
|---|---|---|
| Baseline testDebugUnitTest/lintDebug/assembleDebug | PASS (CI) | main d2817bf, run 39022973604 build job |
| Baseline connectedDebugAndroidTest | PASS (CI API35) | same run device-db-checks; synthetic fixtures, not provider end-to-end |
| Baseline Edge checks/media parser | PASS (CI) | same run edge-checks |
| Local Gradle | BLOCKED | wrapper download: Network is unreachable; no Android SDK/adb |
| SQL authorization | PASS | Re-run connected backend 17 October: uniqueness, pair identity, idempotency, spoof/nonparticipant/block/session/media/erasure isolation; transaction rolled back |
| SQL message actions | PASS | Re-run: reply/action windows, hide/delete catch-up, private stars, notification authorization, receipt privacy; rolled back |
| SQL moments/voice | PENDING | connected fixture suite |
| Two physical accounts/devices | BLOCKED | No connected devices; no real delivery/IME/camera acceptance claim |
| SMTP / OAuth / FCM HTTP v1 | UNVERIFIED | Console configuration and actual delivery required |
| API29/API36 | NOT RUN | Existing CI covers API35 only |
| Signed release AAB / install upgrade | BLOCKED | Upload credentials not configured/verified |
| Play Console/prelaunch/closed testing | BLOCKED | No access/eligibility evidence |

New patch CI results must be appended with immutable head SHA and run URL before merge.

# Data Safety worksheet — not yet submitted

Use the final signed artifact and deployed provider configuration. Pritam Pal must review the Play definitions and submit the form; this repository is not an automated legal declaration.

| Data | Source / purpose | Collection/control assessment |
|---|---|---|
| Email, account/user IDs | Supabase Auth; sign-in, account management/security | Collected; required for account functionality |
| Profile name, username, bio, avatar | User profile/discovery | Collected; avatar/bio optional; distinguish mandatory fields |
| Text and message metadata/actions | Conversations, sync, receipts, replies/reactions/stars | Collected; sent to intended recipient and backend; no E2EE |
| Photos and voice/audio | User-triggered sharing, Stories/Posts/avatar | Collected when submitted; optional media actions |
| Saved contacts/social relationships | In-app UUID relationships, not device address book | Collected; review Contacts/User IDs/App interactions definitions |
| Device/installation IDs and FCM tokens | Firebase/notification registration | Collected in FCM-enabled builds; denying notification permission does not by itself guarantee no token/installation ID is created |
| Reports/blocks/privacy preferences | Abuse prevention, safety, account controls | Collected; explanation and selected evidence references; owner-only review |
| IP/session/security/provider diagnostics | Authentication/service security | Confirm exact provider logs and retention; do not claim zero collection |
| Payments/location/address book | No payment/location/contact permission flow in audited source | Not intentionally collected; verify final artifact/dependencies |
| Advertising/analytics/crash reporting | No new ads/analytics/Crashlytics added | Verify final SDK behavior and Play definitions before answering |

Data sharing in Play's form has specific service-provider/user-initiated-transfer exceptions. Do not answer “not shared” merely because data is sent only to Supabase/Google/Cloudinary or recipients; evaluate the current definitions and active contracts/configuration. No data-sale claim should be invented.

Transport encryption: HTTPS in app/network configuration; do not conflate this with end-to-end encryption or encrypted Room history. Account deletion: in-app flow exists, external URL still needs hosting/verification. Data retention exceptions must match recipient text, reports and operational jobs. Security review/independent validation badges: do not claim them from CI or a provider advisor.

Final owner tasks: confirm actual optional/required data choices, purposes, sharing exceptions, deletion URL, support contact, audience, retention, providers, SDK collection and signed-build permissions. Record final form export privately with the release SHA; never store reviewer passwords here.

References:
https://support.google.com/googleplay/android-developer/answer/10787469
https://support.google.com/googleplay/android-developer/answer/10144311
https://firebase.google.com/support/privacy

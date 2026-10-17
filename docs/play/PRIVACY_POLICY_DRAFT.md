# Liquid Chat Privacy Policy — publication draft

Developer/operator: **Pritam Pal**. Proposed effective date: 17 October 2026. **Owner must supply a real support/privacy contact, finalize retention/processing disclosures and host this policy before public release. This file alone is not a completed Play policy URL.**

## Information processed

Liquid Chat processes your account email/authentication identifiers, profile name/username/bio/avatar, account and app-contact relationships, messages and message metadata, photos, voice messages, Stories/Posts, privacy choices, message actions, blocks, reports and push/device registration identifiers. Supabase/provider authentication and security systems may process IP/network/session metadata. Password inputs are transmitted over HTTPS for authentication and are not stored as plaintext passwords by the Android app.

We use this information to authenticate accounts, deliver and synchronize conversations, store media, apply audience/privacy choices, prevent abuse, process reports and provide deletion/support. Photo and voice sharing are user-initiated. The microphone is used for foreground recording after permission. The app uses selected/captured images and does not request device address-book or location access in the audited manifest.

## Providers and access

Supabase provides authentication, Postgres, realtime, Edge Functions and the private media fallback. Google Firebase Cloud Messaging provides push/device delivery. Authenticated Cloudinary media is optional and must be disclosed as active if configured. Network transport uses HTTPS. Service operators/providers can process stored content as needed to operate the service; this app does **not** provide end-to-end encryption.

Other participants receive content you intentionally share. Story/Post access follows the creator's saved-contact audience and block rules. Reports may include an explanation and references to selected messages or content; authorized operators may review relevant content. Reports are not exposed to ordinary app users.

## Device storage and controls

Authentication/PKCE tokens are encrypted with Android Keystore. Cached messages/drafts are stored in app-private Room storage, not an encrypted chat database. Android backups are disabled. Media URLs are short-lived (currently photos 60 seconds, voice 180 seconds); an already issued URL or saved/downloaded copy may remain usable until expiry or outside the app's control. Do not promise immediate worldwide revocation of saved copies.

Use Settings for privacy preferences, report/block and account deletion. Sign-out removes this device's local account cache, pending messages, media/camera scratch, notification state and session material. If the network is unavailable, server session revocation cannot be confirmed even after local sign-out. Cleanup/storage errors are reported rather than represented as completed erasure.

## Retention and deletion

Message text and recipient history can remain after account deletion under an anonymized Deleted account identity. Delete-for-me hides the user's history; it does not erase the other participant's copy. Delete-for-everyone is subject to the app's ownership/time window and cannot erase screenshots or downloaded copies.

Account deletion requires recent sign-in, removes account/profile identifiers and device/session registrations, revokes access and queues owned-media cleanup. Failed cleanup can retry in the background; do not promise immediate completion during provider outages. Stories expire after 24 hours, with scheduled cleanup afterward; Posts remain until managed/deleted. Operational retention currently removes completed notification jobs and cron records after 30 days and resolved reports after 90 days; unresolved reports remain pending review. Owner must validate and publish any additional legal/security retention and backup policy before release.

## External deletion and support

Provide the final public deletion-request URL and a support/privacy contact here before publication. The external path must work without installing the app, identify Liquid Chat/Pritam Pal, explain identity verification and retained recipient text, and provide a request/confirmation route. Never request passwords, tokens, private signing files or service-role credentials by email or public GitHub issue.

## Additional owner decisions

Confirm target audience/age restrictions, applicable child-safety obligations, lawful processing/transfer requirements, operational support/appeal ownership, any crash/analytics tools introduced later, and a process for notifying users about material changes. No advertising or analytics SDK was intentionally added in this change; review the actual final dependencies/providers before making the Play declaration.

This draft must be reviewed against the final deployed build and actual operations, then linked inside the app/Play Console as required.

# Dependency inventory and advisory review

## Scope and current evidence

This is a dependency/catalog review, not a completed independent security scan or legal clearance. The Codex Security scan preflight reference remains unavailable; no formal scan completion is claimed. OSV coverage excludes unpublished vulnerabilities, native/OS components and runtime exploitability. Refresh the snapshot before launch and after dependency changes.

The prior frozen Deno graph contained15 npm records. Registry license metadata was14 MIT and1 BSD-2-Clause, with all registry dist.integrity fields matching the lock. OSV returned GHSA-8r88-6cj9-9fh5 / CVE-2025-48370 for auth-js2.69.1. Published severity is Low. A verified tarball from that lock lacks UUID validation in admin.deleteUser. The app passes user_id from private.deletion_jobs/deletion_batch, typed PostgreSQL UUID; no attacker-controlled path string reaches this call in the reviewed route. No application exploit or actual user erasure was performed.

Use exact supabase-js2.52.1, which pins auth-js2.71.1 after the upstream UUID fix and Solana-dependency removal. Storage-js2.7.1 and postgrest-js1.19.4 stay unchanged. All four function imports and the regenerated Deno2.5.4 lock agree. The new graph has16 npm records; the local live OSV snapshot completed with0 matches and all registry integrity metadata matching. This does not imply no vulnerabilities exist.

Admin regression fixtures inject fetch, deny real runtime network access, verify malformed IDs generate0 HTTP calls, and verify valid UUID deletion routing. Four Deno tests (including two media parsers) pass locally; host tools have9 passing regressions. The ws dependency reads only WS_NO_BUFFER_UTIL/WS_NO_UTF_8_VALIDATE in this test environment; permission is granted for those flags only, never for Supabase secrets or network. Initial missing-env-permission failure was corrected before acceptance.

## Android resolved inventory

Run after an Android release dependency resolution on Java17/Gradle8.11.1 with SDK36:

```sh
./gradlew --no-daemon -I scripts/dependency_inventory.init.gradle :app:exportDependencyInventory
python3 scripts/audit_dependencies.py \
  --inventory app/build/reports/dependencies/release-inventory.json \
  --lock supabase/functions/deno.lock \
  --output app/build/reports/dependencies/security-audit.json
```

CI exports the actually selected releaseRuntimeClasspath Maven modules, artifact SHA256, POM SHA256 and publisher-declared licenses into check-reports. The init script does not change dependencies/locks, run package code or fetch license URLs. POM parsing blocks external entities/DOCTYPEs. Missing/inherited/error license entries stay explicitly unresolved. An offline local Gradle fixture verified direct/transitive modules, binary hashes and unknown-license labels. Actual app export and Maven OSV results require this phase's CI and downloaded inventory; they are not inferred from declared dependencies.

The audit tool follows OSV pagination, checks result cardinality, reports failures as UNKNOWN and exits2 on incomplete metadata/integrity mismatch; matches exit1 for triage. Exit0 means complete catalog queries with no matches, not a release or legal approval. Its Deno v5 parser preserves package/version across peer-context suffixes. Pagination, missing-response and peer-context regressions run with fake responses and no network.

Build plugins/annotation processors, complete license text/NOTICE obligations, Maven publisher provenance, actual native/OS vulnerabilities and an independent repository audit remain additional review scope. Artifact hashes document what was built; they are not proof that the publisher is trustworthy.

## Compiled permission review

Accepted API36 candidate badging lists INTERNET, RECORD_AUDIO, POST_NOTIFICATIONS, WAKE_LOCK, ACCESS_NETWORK_STATE, RECEIVE_BOOT_COMPLETED, FOREGROUND_SERVICE, FCM RECEIVE and the app's signature-only dynamic-receiver permission. Direct permissions serve network/voice/push; merged background permissions come from WorkManager/FCM. No location, contacts, phone-call, storage-read or advertising-ID permission was observed in that artifact. Recheck each final merged artifact; this is not verification of every runtime consent journey.

## Backend deployment and recovery

Before SDK rollout, all four deployed functions were read back and matched accepted main0649caf6 source exactly; metadata/source snapshots were retained. Existing gateway verify_jwt=false flags are preserved because each handler performs its own authenticated-user or cron-secret checks. No grant, session/RLS rule, SQL migration, provider credential, scheduler, quota or billing setting changes.

Deploy only after phase CI passes. Keep prior source/lock at main0649caf6 for a reversible code-only recovery; never restore/delete user data or rewrite deployed migrations to roll back an SDK change. Re-run selected authorization/media suites if any handler behavior changes. Post-deploy verification is code readback, ACTIVE metadata and unauthorized request denial only. Real account erasure and media destruction are not test probes and remain owner/device acceptance gates.

References: https://github.com/supabase/auth-js/security/advisories/GHSA-8r88-6cj9-9fh5 ; https://github.com/supabase/auth-js/pull/1063 ; https://supabase.com/docs/guides/security/npm-security ; https://google.github.io/osv.dev/post-v1-querybatch/ . The recent Supabase CBC-cipher deprecation was reviewed; the client toolchain supports modern GCM/TLS, while actual provider/Android-device acceptance is still required before launch.

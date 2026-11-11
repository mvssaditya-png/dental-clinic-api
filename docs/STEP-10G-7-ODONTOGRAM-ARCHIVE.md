# Step 10G.7 — Odontogram archival

## Scope and approved decisions

Archive preserves the chart ID, observations, tooth history and encounter references. No physical deletion, restoration or automatic replacement is implemented. Staff may explicitly create a replacement active chart. ODONTOGRAM_ARCHIVE is granted only to the global system SUPER_ADMIN, ADMIN and DOCTOR roles; ordinary editing does not imply archive permission.

## Contracts

- `POST /api/odontograms/{id}/archive?clinicId=...`: ODONTOGRAM_ARCHIVE, quoted `If-Match: "3"`, JSON `{ "reason": "Incorrect patient chart" }`, success 204.
- `GET /api/odontograms/{id}?clinicId=...`: ODONTOGRAM_VIEW; returns active or archived chart metadata including version, archivedAt, archivedBy and archiveReason.
- `GET /api/patients/{patientId}/odontograms/archived?clinicId=...&page=0&size=20`: ODONTOGRAM_VIEW; content, number, size, totalElements, totalPages. Maximum size 100; ordering archivedAt DESC, id DESC.
- Existing patient chart discovery returns only ACTIVE charts. Existing chart-ID tooth/history reads remain available for authorized archived-chart review.

TenantClinicResolver resolves effective clinic. Parent/patient reads are clinic scoped. Missing/cross-clinic chart is 404. Missing precondition is 428, malformed precondition/reason 400, stale version 412, already archived 409. Authentication/authority rules remain enforced.

Reason uses the existing clinical-removal validator: nonblank, maximum 1,000 Unicode code points, no prohibited controls or invalid surrogate sequences. Line breaks are rejected by this shared rule. Meaningful Unicode is preserved exactly.

## Migration V25

Replaces the global patient-chart unique constraint with an ACTIVE-only patient unique index. Existing IDs/references are unchanged. Adds chart aggregate version (initial zero), nullable archive metadata, archive index and append-only odontogram_archive_audit. Legacy archived charts retain unknown actor/date/reason; no historical event is fabricated or reconstructed.

Database triggers reject archive-audit update/delete/truncate, chart deletion/truncate, and updates to an already archived chart. These protections do not claim to prevent privileged database administrators from disabling triggers. V21/V22 and their immutable clinical-history protections remain unchanged.

Only the isolated verification database dental_clinic_v21_verification_archive_20261111 received V25 during verification. No application/shared/production migration or deployment was performed.

## Transactions and concurrency

Archive obtains the existing pessimistic chart lock, checks ACTIVE and the reviewed version, updates metadata/version, and inserts audit in one transaction. Tooth insertion and update increment the chart aggregate under the same chart lock. An intervening tooth change invalidates archival review; an archive committed first blocks further application tooth writes.

Existing tooth-write lock order remains Case Sheet/lifecycle guard then chart. Archive locks chart only and never subsequently acquires a Case Sheet lock. Creation retains the patient lock and checks for an active chart; the partial unique index is the database backstop. No clinical references are cascaded away.

## Frontend

More → Remove Odontogram is capability gated. Confirmation fetches authoritative state, requires a reason and checks the reviewed version again before one POST. Synchronous submission protection prevents duplicate clicks. Uncertain responses trigger read-only reconciliation, never automatic mutation retry. Confirmed archival closes the dialog and refreshes active discovery. Archived charts remain readable with mutations disabled. Replacement creation remains explicit. Existing consultation integration uses the shared chart component.

## Changed files

Backend: OdontogramResponse.java, Odontogram.java, OdontogramRepository.java, OdontogramService.java, OdontogramArchiveController.java, OdontogramArchiveService.java, V25__odontogram_archive.sql, OdontogramDatabaseTest.java, OdontogramArchiveDatabaseTest.java, and this document.

Frontend: src/features/odontogram/{OdontogramScreen.tsx,odontogram.ts,OdontogramArchive.tsx,odontogram-archive.ts}; src/lib/auth/{authorization.ts,capabilities.ts}; tests/{authorization.test.ts,odontogram-archive.test.ts}.

## Verification

- Full backend tests: 380 discovered, 238 passed, 142 opt-in tests skipped, zero failures/errors.
- PostgreSQL Odontogram executions: 29 archive tests, 20 existing mutation tests, 10 configuration tests; archive class inherits 20 existing tests, so these execution counts include deliberate repeat coverage. Five precondition unit tests also passed.
- Coverage includes concurrent archive/archive and archive/tooth writes, stale versions, rollback on audit failure, immutable audit, Unicode boundaries, clinic isolation, grants and explicit replacement.
- Separate-output Maven verify/package passed: target/10g7-package/dental-clinic-api-0.0.1-SNAPSHOT.jar. Running target JAR was not replaced by packaging.
- Final full frontend suite: 1,443 passed, zero failed/skipped. TypeScript and Web export passed. Both repository diff checks passed. No configured frontend lint script is available.

## Manual verification and limitations

Manual desktop/iPhone validation remains required: authorized versus view-only controls; cancel/unsaved reason; Unicode reason; successful archive notice; archived tooth/history readability; explicit replacement; consultation context; clinic/account switch; stale review; offline uncertainty and read-only retry. No manual browser result is claimed.

Archive actor is exposed as its recorded user ID, not an invented name. Legacy archive metadata may be unavailable. Restore is intentionally unsupported. Unrelated opt-in backend suites remain skipped. Apply V25 using the normal local deployment process before manually testing the new APIs; do not point verification at shared or production databases.

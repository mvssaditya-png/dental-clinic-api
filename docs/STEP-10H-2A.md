# Step 10H.2A - Case Sheet finalization review concurrency

## Compatibility
PATCH /api/case-sheets/{id}/finalize now requires If-Match: "<version>". Body remains {"clinicId":"<effective clinic UUID>"}. Missing header returns 428; malformed/weak/wildcard/multiple values return 400; stale version returns 412. Success remains 200 CaseSheetResponse. Old clients cannot finalize without updating their contract. No unconditional fallback exists.

## Aggregate and transaction design
Reuse V24 CaseSheet.version (@Version). Text updates and finalization already advance it. Diagnosis and finding CREATE, UPDATE and soft REMOVE (both legacy DELETE and JSON removal routes) now dirty and flush the locked Case Sheet parent in the same transaction as child changes and audit insertion. Even no-op entry edits invalidate review. Failures roll back child, version and audit together. Parent updatedAt now also reflects clinical child changes.

Locks remain Case Sheet -> Consultation -> child/chart. Finalization parses the expected version, acquires the existing Case Sheet lock, validates active consultation and compares the stored aggregate version before applying readiness/finalization. No automatic retry. Existing DRAFT, chief complaint and nonremoved structured diagnosis prerequisites, permissions and clinic isolation remain unchanged.

The aggregate covers the five Case Sheet text fields and its structured diagnoses/findings. Audit history is derived append-only evidence, not independently mutable clinical content. Patient-level Odontogram observations, condition catalogue configuration, patient demographics and Treatment Plans are separate records and do not invalidate this token. Their displayed data must not be represented as part of the finalized Case Sheet snapshot.

## Read protocol for future UI
Existing CaseSheetResponse.version is authoritative for both detail and consultation lookup. No new read endpoint is needed. To review across separate APIs: read parent with version V; load complete active diagnoses and findings; read parent again. Only present a confirmable review when identity/status/version still match V. On mismatch discard the candidate review and reload all content. Retain V from that review; never silently replace it immediately before finalization. If a mutation commits after the second read, finalization rejects V with 412. Fresh requests must not reuse stale cached child lists. This protocol must be implemented in Step 10H.3, which is not part of this change.

Text PUT continues to require its loaded version; a concurrent structured-entry change now also returns 412 for stale text PUT. This conservative conflict is intentional and avoids merging clinical reviews.

## Migration
None. V24 and all earlier migrations are unchanged. No backfill, clinical history rewrite or shared/production database migration.

## Files in this step
- casesheet/entity/CaseSheet.java
- casesheet/service/CaseSheetService.java
- casesheet/service/CaseSheetEntryService.java
- casesheet/controller/CaseSheetController.java
- tests: ConsultationLifecycleDatabaseTest, CaseSheetEntryDatabaseTest, OdontogramDatabaseTest
- docs/STEP-10H-2A.md

All paths above are relative to the existing Java source/test package roots. Existing Step 10H.2 working-tree changes are retained; frontend untouched in this step.

## Verification
- Initial focused run: 58 tests, 57 passed, one legacy expectation failed because removal-before-finalization now correctly returns 412 instead of 400. Updated that regression assertion.
- Final full backend test run: 339 discovered; 227 passed, 112 opt-in skipped, zero failures/errors. Includes all 60 focused Case Sheet/lifecycle tests (30 lifecycle PostgreSQL, 17 Case Sheet PostgreSQL, 13 entry unit tests).
- Full run packaging could not rename the existing target application JAR. Separate-output package verification is used rather than stopping an existing application process. Its generated POM lives only under ignored target and retains production source/dependencies/plugins, changing source/resource/output paths; tests were already executed by the full run.
- Databases explicitly restricted to dental_clinic_v18_verification_10h2_20261110 and dental_clinic_v21_verification_10h2_20261110 on localhost. No application/shared/production migrations.

## Limits and deployment notes
- Application mutation paths enforce aggregate increments; direct privileged SQL writes bypass the application contract and are unsupported.
- No frontend completion/finalization UI is included. Step 10H.3 must implement the read protocol above and safe 412 recovery.
- Deploy matching clients for finalization; clients without If-Match receive 428. Step 10H.2 text editor already handles version conflicts.
- Separate patient-level data are not frozen by Case Sheet finalization.
- Manual check: review version V, add/edit/remove a diagnosis or finding in another session, finalize with V; expect 412 and DRAFT unchanged. Reload complete content, renew confirmation, finalize with its version; expect FINALIZED. Verify no automatic consultation or appointment transition.
Separate-output Maven verify with tests skipped (already passed in full run): BUILD SUCCESS. Executable JAR is under target/10h2a-package; no deployment or restart performed.
Final Odontogram PostgreSQL regression run: 20 passed, zero failures/errors/skips. These opt-in tests were skipped in the full run and executed separately. Final git diff --check passed.

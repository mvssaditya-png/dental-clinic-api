# Step 10H.2 - Compatibility and locking design (before implementation)

Case Sheet PUT /api/case-sheets/{id}?clinicId=... requires one quoted numeric If-Match header. Missing=428, malformed=400, stale=412. Response adds version. Existing callers without the header can read but cannot save; deploy the updated editor with this backend. No fallback unconditional write is provided. All five replacement fields remain unchanged.

Consultation PATCH body and permissions are unchanged; no If-Match is invented. Invalid/duplicate transitions become 409 instead of 400. Row locks give serial transition semantics: two different requests may both succeed if the resulting serialized sequence is valid. This is not an expected-status contract.

Lock order: existing Case Sheet row -> Consultation row -> chart/tooth/entry rows. Consultation transitions first locate and lock any existing sheet, then lock/refresh consultation. Case Sheet creation locks only consultation before inserting a new sheet (it never waits on an existing sheet lock). Reads do not take these locks. No treatment-plan or appointment mutation semantics change.

Cancellation remains allowed after finalization if consultation is STARTED/IN_PROGRESS: finalization-first followed by cancellation is a valid serial outcome. Cancellation-first blocks finalization and encounter-bound clinical writes. Preventing cancellation of a finalized visit would be a new rule, not introduced here.

V24 adds a Case Sheet version baseline of zero and an append-only consultation status audit table. No historical events or terminal timestamps are invented. Successful COMPLETED/CANCELLED transitions set endedAt from the server and insert an event atomically. No audit read endpoint or new permission is introduced.

Tests must run on isolated verification databases only, never the application datasource.

## Implemented behavior
- Consultation transitions and Case Sheet create/edit/finalize, diagnosis/finding writes, and Odontogram observations share the encounter lock. Ordinary reads and unrelated treatment-plan writes were not changed.
- Sheet text uses JPA @Version under the existing sheet row lock. Responses are flushed before mapping, including finalization, so returned versions are authoritative. Text PUT always updates the server timestamp, including a no-op replacement.
- Consultation timestamps/audit are written in the same transaction; rollback removes both. Audit rows cannot be updated, deleted or truncated through ordinary SQL.
- Existing authorization annotations and TenantClinicResolver remain in force. Missing/wrong-clinic consultations in the new guard return 404. Inactive consultation-dependent mutations return 409. Existing sheet/odontogram validation otherwise retains its established errors.
- No API reason field exists for consultation status changes, so no reason was invented.
- Case Sheet version is optional for read compatibility in the frontend; malformed supplied versions are rejected and unversioned sheets cannot be saved. The editor sends its loaded version, never a silently substituted latest version, and preserves unsaved text on 409/412/428 until explicit reload.
- No audit-history read endpoint was added. No automatic finalization, appointment completion, Treatment Plan lifecycle change, authentication change or historical backfill.

## Changed files
Backend:
- casesheet/controller/CaseSheetController.java
- casesheet/dto/CaseSheetResponse.java
- casesheet/entity/CaseSheet.java
- casesheet/repository/CaseSheetRepository.java
- casesheet/service/CaseSheetService.java
- casesheet/service/CaseSheetEntryService.java
- consultation/repository/ConsultationRepository.java
- consultation/service/ConsultationService.java
- consultation/service/ConsultationLifecycleGuard.java (new)
- consultation/controller/ClinicalLifecycleErrors.java (new)
- odontogram/service/OdontogramService.java
- src/main/resources/db/migration/V24__consultation_lifecycle_safety.sql (new)
- src/test/java/com/dentalclinic/casesheet/CaseSheetEntryTest.java (new dependency wiring)
- src/test/java/com/dentalclinic/consultation/ConsultationLifecycleDatabaseTest.java (new)
- docs/STEP-10H-2.md

Frontend:
- src/features/case-sheets/case-sheet.ts
- src/features/case-sheets/case-sheet-edit.ts
- tests/case-sheet-edit.test.ts

## Verification
- Focused lifecycle/Case Sheet backend run: 48 passed, no failures/skips (18 new lifecycle PostgreSQL tests, 17 existing Case Sheet PostgreSQL tests, 13 entry unit tests).
- Full backend verify/package: 327 discovered, 215 passed, 112 opt-in tests skipped, zero failures/errors.
- Frontend focused: 248 passed, zero failures/skips.
- TypeScript passed; both repository diff checks passed.
- Lint was attempted but the frontend has no lint script; no dependency was added.
- The full suite includes the existing legacy Case Sheet migration test.
- PostgreSQL datasource was explicitly pinned to dental_clinic_v18_verification_10h2_20261110. Additional Odontogram regressions use dental_clinic_v21_verification_10h2_20261110.
- No application/shared/production datasource was migrated or changed; no backend restart/deployment was performed.

## Manual checklist (not performed)
1. Deploy backend V24 and the matching editor together in an approved environment; do not expose old unconditional PUT clients.
2. Open one DRAFT sheet in two sessions; save one, then save the other. Confirm 412/reload behavior and that the first save survives.
3. Omit/malform If-Match; confirm 428/400 without a clinical write.
4. Race cancellation with finalization and observation saving. Verify serial outcomes, no partial observation history and no deadlock.
5. Exercise valid consultation transitions through the existing authorized API; confirm terminal endedAt, unchanged startedAt/appointment status, and exactly one persisted event for each success.
6. Repeat invalid/duplicate transitions and revoked/cross-clinic permissions; confirm no status/audit writes.
7. Check editor unsaved-change warning, explicit reload, mobile layout and uncertain-network read-only reconciliation.

## Remaining limits
Status transitions are serialized, not client-versioned: a later different transition may succeed if valid after the first. Repeating an already-applied transition returns 409; callers must reconcile via GET.
Case Sheet version covers its own entity/text/status, not a new aggregate version for diagnoses/findings.
Append-only triggers protect ordinary database operations; a database owner can still alter/drop protections. Operational privileges/backups remain necessary.
Case Sheet finalization and consultation/appointment completion remain separate operations. A future single completion operation needs separate approval.

Final regression verification: all 20 existing OdontogramDatabaseTest PostgreSQL tests passed, zero failures/errors/skips. These were opt-in/skipped in the full suite and executed separately against the isolated V21 verification database. Backend packaging and final diff checks passed.

# Step 10H.5 - Conditional Treatment Plans

## Result and approved policy

Implemented and automated verification completed on 11 November 2026. Consultation completion uses validation recorded at Case Sheet finalization. Later cancellation of a linked plan does not reopen or retroactively invalidate that finalized visit. No automatic clinical mutations or billing behavior were added.

## Architecture findings

- Case Sheet text and structured diagnoses/findings share the existing aggregate version. Text replacement and finalization already require quoted If-Match. Existing locks are Case Sheet then Consultation.
- Treatment Plan creation copies Case Sheet, consultation, appointment, patient and clinic references from the verified DRAFT Case Sheet. These provide a reliable encounter association.
- Plan and item writers lock the parent plan. They do not acquire a Case Sheet lock afterwards. PRESENTED is the first documented status requiring active saved procedures; approval/patient acceptance is a separate event.
- Consultation completion already requires a FINALIZED Case Sheet. Appointment completion remains separate. Existing frontend review brackets structured entries with version-consistent parent reads and does not automatically retry writes.

## Clinical rules

Drafts may have no decision. Finalization requires REQUIRED, NOT_REQUIRED or DEFERRED. There is no default decision.

- REQUIRED: same clinic, patient, consultation and Case Sheet; nonremoved plan in PRESENTED, APPROVED, IN_PROGRESS, COMPLETED or DECLINED; at least one nonremoved, noncancelled item. DRAFT, CANCELLED, removed, empty and unrelated plans do not qualify. DECLINED is retained as documented presentation/refusal, not patient acceptance. Approval is not required.
- NOT_REQUIRED: no plan required; existing plans are untouched.
- DEFERRED: nonblank reason required even for draft saves; no plan required. Unicode whitespace-only reasons are rejected server-side.
- Existing chief complaint, active structured diagnosis, DRAFT/active-consultation, permissions and clinic rules remain.
- Historical finalized records retain a null decision and remain completable. The UI labels this historical absence; no data is backfilled.

## Concurrency and evidence

Decision/reason edits increment the existing Case Sheet aggregate version. Finalization validates the exact reviewed If-Match under the existing locks. For REQUIRED, candidate plans are locked in stable ID order after Case Sheet and Consultation. Item/status writers already serialize on those plan locks. Eligibility is checked while those locks are held.

Finalization atomically inserts immutable evidence containing decision/reason, actor, timestamp and encounter identity; REQUIRED also records qualifying plan ID, number, status and aggregate version. The Case Sheet finalization and evidence insert commit or roll back together. Completion checks matching evidence, not the current plan status. The evidence table rejects UPDATE, DELETE and TRUNCATE. This is a finalization snapshot, not a new draft edit-history API.

## V26 migration

`V26__case_sheet_treatment_decision.sql` adds nullable decision/reason columns and an enum-value check to `case_sheet`. It creates `case_sheet_treatment_finalization`, with foreign keys, required-evidence/deferral checks and immutability triggers. V25 and earlier migrations are unchanged. Existing clinical rows are not updated.

Applied only by tests to the isolated databases:
- dental_clinic_v18_verification_10h2_20261110
- dental_clinic_v21_verification_archive_20261111

No application, shared or production database was migrated. Deploy through the normal reviewed Flyway process; do not delete evidence or backfill guessed decisions. Rollback requires retaining V26 data and assessing older-server behavior, because old servers do not enforce the new rule.

## API contract and compatibility

Existing `PUT /api/case-sheets/{id}?clinicId=...` adds these replacement-body fields alongside the five existing text fields:

```json
{
  "chiefComplaint": "Pain",
  "historyOfPresentIllness": null,
  "clinicalExamination": null,
  "diagnosisSummary": null,
  "treatmentNotes": null,
  "treatmentPlanRequirement": "DEFERRED",
  "treatmentDecisionReason": "Awaiting diagnostic results"
}
```

Quoted If-Match remains mandatory. Missing/malformed/stale preconditions remain 428/400/412. Invalid enum/blank DEFERRED reason returns 400. Missing decision or qualifying plan blocks finalization with 409. Existing authorization and tenant resolution are unchanged.

Case Sheet read/update responses add `treatmentPlanRequirement`, `treatmentDecisionReason`, and nullable `treatmentDecisionBlocker`. The blocker is derived from current backend state for DRAFT sheets; it is advisory until finalization revalidates under locks. Creation still creates an empty draft; the existing editor saves the decision through PUT.

Compatibility impact: PUT remains complete replacement; omitted decision/reason clear them on a draft. Upgrade clients together with the server to preserve existing selections. Older clients cannot finalize new undecided sheets. Historical finalized records are exempt without fabricating evidence. No treatment-plan endpoints or status contracts changed.

## Frontend behavior

Existing Case Sheet entry/edit has compact accessible radio options, helper text and an optional/required multiline reason. Selection and reason participate in dirty state, payload comparison, stale-version detection and uncertain-save reconciliation. Detail and finalization review display persisted decisions. Finalized values are read-only.

Visit Completion displays authoritative decision blockers. REQUIRED blockers offer existing View/Create Treatment Plan actions under existing capabilities, preserving encounter/clinic context. They open the existing consultation modal, do not submit/create automatically, and consume the navigation request so finalization remounts do not reopen it. NOT_REQUIRED/DEFERRED do not display a misleading plan blocker. After changing a plan, use the existing readiness refresh/review; finalization always rechecks server state.

## Files changed for 10H.5

Backend (paths under src/main/java/com/dentalclinic unless noted):
- casesheet/entity/CaseSheet.java
- casesheet/entity/TreatmentPlanRequirement.java (new)
- casesheet/dto/UpdateCaseSheetRequest.java
- casesheet/dto/CaseSheetResponse.java
- casesheet/service/CaseSheetService.java
- casesheet/service/TreatmentDecisionService.java (new)
- consultation/service/ConsultationService.java
- src/main/resources/db/migration/V26__case_sheet_treatment_decision.sql (new)
- src/test/java/com/dentalclinic/consultation/TreatmentDecisionDatabaseTest.java (new)
- src/test/java/com/dentalclinic/consultation/ConsultationLifecycleDatabaseTest.java
- src/test/java/com/dentalclinic/casesheet/CaseSheetEntryDatabaseTest.java
- src/test/java/com/dentalclinic/odontogram/OdontogramDatabaseTest.java (explicit test-fixture decision only)
- this document

Frontend:
- src/features/case-sheets/TreatmentDecision.tsx (new)
- src/features/case-sheets/case-sheet.ts
- src/features/case-sheets/case-sheet-edit.ts
- src/features/case-sheets/case-sheet-start.ts
- src/features/case-sheets/EditCaseSheetScreen.tsx
- src/features/case-sheets/CaseSheetDetailScreen.tsx
- src/features/consultations/visit-completion.ts
- src/features/consultations/visit-completion-presentation.ts
- src/features/consultations/VisitCompletion.tsx
- src/features/consultations/ConsultationDetailScreen.tsx
- src/features/treatment-plans/ConsultationTreatmentPlans.tsx (existing modal entry request only)
- tests/case-sheet-edit.test.ts
- tests/case-sheet-start.test.ts
- tests/visit-completion.test.ts

Unrelated existing Odontogram/archive/authorization working-tree changes are preserved and are not part of this implementation.

## Verification results

- Main backend run: 400 tests discovered, 276 passed, 124 opt-in skipped, zero failures/errors. Archive tests are run separately because their database guard requires a different isolated database.
- Separate Odontogram/archive/configuration run: 64 passed, zero skipped/failures/errors. Thirty of these were skipped in the main run; 34 archive tests were excluded there. Combined unique test executions: 340 passed, 94 still opt-in skipped.
- TreatmentDecisionDatabaseTest: 49 passed (19 decision-specific cases plus 30 inherited lifecycle regressions). CaseSheetEntryDatabaseTest: 17 passed; ConsultationLifecycleDatabaseTest: 30 passed.
- Covered missing/required/deferred decisions, qualifying statuses, unrelated/removed/cancelled/empty plans, cancelled items, legacy finalization, evidence rollback and deletion protection, stale edits, both cancellation/finalization lock orders and completion after cancellation. Existing inherited permission, clinic, lifecycle and concurrency regressions passed.
- Full frontend suite: 1,466 passed, zero failures/skips. Final focused Case Sheet/edit/start/completion suite: 100 passed after navigation refinement.
- TypeScript: passed. Expo Web export: passed to the temporary export directory.
- Maven verify/package with tests skipped after the test runs: passed using a separate output POM; executable JAR at target/10h5-package/dental-clinic-api-0.0.1-SNAPSHOT.jar. Original target JAR SHA-256 remained unchanged.
- git diff --check: passed in both repositories.
- Lint unavailable: package.json has no lint script. No lint dependency/configuration was introduced.

The first full backend attempt incorrectly combined incompatible isolated-database suites; database guards rejected 34 archive tests before database access. They subsequently passed on their designated database. Initial frontend fixture assertions were updated for the new explicit fields; final runs pass.

## Manual Web/iPhone/Android checklist and limitations

1. Open an authorized DRAFT Case Sheet; confirm no decision is selected automatically. Save/reopen all three choices; Deferred must reject blank reason and retain entered text.
2. With complaint and diagnosis recorded, confirm missing decision blocks finalization. REQUIRED with no presented linked plan must show actionable navigation; creation must preserve patient/Case Sheet/clinic context.
3. Add procedures and present the linked plan, refresh readiness, review and explicitly finalize. Confirm no approval, invoice or automatic completion occurs.
4. Confirm NOT_REQUIRED and reasoned DEFERRED finalize without a plan. Check finalized historical sheets show the historical missing-decision label.
5. Finalize a REQUIRED visit, later cancel its linked plan, then explicitly complete consultation and appointment. The original validation remains effective.
6. Use two tabs to edit the decision/finalize: stale review must fail without overwriting or retrying. Switch clinic/account during a pending request; no stale result or write may be applied.
7. Check radio/reason layout, keyboard scrolling, touch targets, finalization review, existing modal Back/Close and return context on desktop, iPhone and Android.

Physical-device and authenticated manual browser checks remain outstanding. PostgreSQL suites unrelated to this change remain opt-in/skipped as counted above. No deployment, application database writes, performed procedures, invoices or payments were performed. No permission grants or archive semantics changed.

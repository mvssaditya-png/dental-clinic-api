# Step 10E.4C.2 - versioned draft Treatment Plan items

Backend-only implementation. No frontend or reorder changes.

## Migration and authorization

V19__treatment_plan_item_history.sql adds item version (existing rows start at 0), nullable removed_at/removed_by, active-item index and a separate append-only audit table. Existing clinical data and prices remain unchanged; no historical events are fabricated.

TREATMENT_PLAN_HISTORY_VIEW is granted only to the system SUPER_ADMIN, ADMIN and DOCTOR roles. RECEPTIONIST and ATTENDER receive no grant. CASE_SHEET_HISTORY_VIEW is unchanged. Controllers enforce runtime authorities, not role-name bypasses. TenantClinicResolver retains existing active-clinic behavior: clinic-bound users use their own clinic; platform users must supply clinicId.

## Endpoints

All paths below start with /api/treatment-plans/{planId}. clinicId is an optional query parameter for clinic-bound users and required operational context for platform users.

### PUT /items/{itemId}

Authority: TREATMENT_PLAN_EDIT. Required header: If-Match: "0" (current item version).

Replacement body:

~~~json
{
  "quantity": 2,
  "estimatedVisits": 3,
  "toothNumber": "16",
  "description": null,
  "clinicalNotes": "Review at next visit",
  "patientExplanation": null,
  "followUpInstructions": null
}
~~~

quantity must be a positive 32-bit integer; estimatedVisits is null or positive. Tooth number is trimmed and limited to 10 Unicode code points. Optional missing/blank text becomes null; catalogue defaults are not reapplied. Returns HTTP 200 with the existing TreatmentPlanItemResponse plus additive numeric version. Procedure identity, name/code, saved unit price, sequence and discounts cannot be replaced through this DTO.

### POST /items/{itemId}/remove

Authority: TREATMENT_PLAN_EDIT. Required If-Match header. JSON body: {"reason":"Incorrect procedure selected"}. Reason is nonblank, up to 1000 Unicode code points; control characters and invalid surrogates are rejected. Unicode text is preserved. Returns 204. Soft removal retains clinical fields and lifecycle status.

### GET /item-history?page=0&size=20

Authority: TREATMENT_PLAN_HISTORY_VIEW. Page must be nonnegative, size positive and capped at 100. Sort: createdAt DESC, id DESC. Response:

~~~json
{
  "content": [{
    "id": "audit UUID",
    "itemId": "item UUID",
    "operation": "UPDATE",
    "actorId": "user UUID",
    "timestamp": "local application timestamp",
    "before": {},
    "after": {},
    "removalReason": null
  }],
  "number": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
~~~

Snapshot maps contain item identity, clinical fields, pricing, version and lifecycle metadata. Operations: CREATE, UPDATE, REMOVE, STATUS. Existing add/status APIs retain their contracts while recording new events.

New mutation errors: 428 missing If-Match; 400 malformed version/request; 412 stale version; 404 scoped missing plan/item; 409 non-DRAFT, removed item, downstream reference or unsupported legacy-discount quantity change. Existing authentication/authorization handling remains in place.

## Consistency and pricing

All competing treatment-plan writers acquire the same clinic-scoped pessimistic parent lock: add item, edit item, remove item, header/discount update, plan status and item status. Item version is checked after locking. Item mutation, parent recalculation and audit insertion share a transaction. There is no automatic retry or full mutation idempotency.

Quantity changes use saved unit price times quantity with two-decimal HALF_UP rounding. Nonzero legacy item discounts (or inconsistent gross/final amounts) block quantity changes; text edits preserve those values. Parent totals reuse existing discount calculation and capping. Removed rows are excluded from normal lists, totals, active counts and presentation/completion checks; the last item can be removed, leaving a zero-total draft which cannot be presented.

Existing invoice, material-use, follow-up or consent references block edit/removal. No downstream workflow policies were changed. Arbitrary SQL writers are outside the application parent-lock protocol.

## Audit mapping correction

The audit entity is marked Hibernate Immutable. JSON snapshot normalization previously caused Hibernate dirty checking to attempt a follow-up UPDATE after INSERT; the database correctly rejected it. Immutable mapping prevents that ORM update. Database UPDATE/DELETE/TRUNCATE triggers remain in force independently, and physical item DELETE/TRUNCATE is blocked. Database owners can still administer triggers; this is not a claim of protection from a privileged DBA.

## Verification and limitations

The new database tests require TREATMENT_ITEM_TEST_DATABASE=true and a disposable PostgreSQL database named dental_clinic_v19_verification_*. Supply SPRING_DATASOURCE_URL/USERNAME/PASSWORD for that test environment, then run Maven verify. Never point these fixture-writing tests at application data. Migration testing uses an isolated schema: migrate to V18, insert a legacy row, then migrate to V19 and verify metadata/prices/grants.

Coverage includes concurrent edit/edit, edit/add, remove/edit, remove/presentation, presentation/edit and header-discount/edit; actual audit-insert failure rolls back item and totals. Permission tests use the migrated role grants and proxied method security. Tests also cover downstream references, Unicode boundaries, immutable audit SQL protection and legacy prices/discounts.

Migration has been tested only on the disposable database, not applied to the application database. Before deployment use the normal backup and Flyway rollout process. Existing clients must tolerate the additive version field. Full idempotency and reordering remain out of scope. Uncertain clients should read current state before deciding whether to retry.

## Changed files

- src/main/resources/db/migration/V19__treatment_plan_item_history.sql
- src/main/java/com/dentalclinic/common/exception/GlobalExceptionHandler.java
- src/main/java/com/dentalclinic/treatment/controller/TreatmentPlanItemController.java
- src/main/java/com/dentalclinic/treatment/dto/TreatmentPlanItemResponse.java
- src/main/java/com/dentalclinic/treatment/dto/UpdateTreatmentPlanItemRequest.java
- src/main/java/com/dentalclinic/treatment/dto/RemoveTreatmentPlanItemRequest.java
- src/main/java/com/dentalclinic/treatment/entity/TreatmentPlanItem.java
- src/main/java/com/dentalclinic/treatment/entity/TreatmentPlanItemAudit.java
- src/main/java/com/dentalclinic/treatment/repository/TreatmentPlanRepository.java
- src/main/java/com/dentalclinic/treatment/repository/TreatmentPlanItemRepository.java
- src/main/java/com/dentalclinic/treatment/repository/TreatmentPlanItemAuditRepository.java
- src/main/java/com/dentalclinic/treatment/service/TreatmentPlanService.java
- src/main/java/com/dentalclinic/treatment/service/TreatmentPlanItemService.java
- src/main/java/com/dentalclinic/treatment/service/TreatmentItemAudit.java
- src/main/java/com/dentalclinic/treatment/service/TreatmentItemException.java
- src/test/java/com/dentalclinic/treatment/TreatmentPlanItemDatabaseTest.java
- src/test/java/com/dentalclinic/treatment/TreatmentPlanItemMigrationTest.java
- src/test/java/com/dentalclinic/treatment/TreatmentPlanItemValidationTest.java
- src/test/java/com/dentalclinic/clinic/ClinicDatabaseIntegrationTest.java (permission-count regression updated from 53 to 54)
- docs/STEP-10E-4C-2.md

## Executed results

- Full Maven verify with the V19 database suite enabled: 203 discovered, 181 passed, 22 other database tests gated off; BUILD SUCCESS.
- Full Maven verify with the older Case Sheet/clinic database suites enabled: 203 discovered, 186 passed, 17 V19 database tests gated off; BUILD SUCCESS.
- Across these configurations: all 203 unique tests passed, zero remaining failures. The 17 V19 database tests comprise 16 service/security/concurrency tests and one V18-to-V19 legacy-row migration test. Three additional validation tests cover versions, Unicode reason boundaries and positive integers.
- The old clinic test's fixed permission count was corrected from 53 to 54; the exact existing four clinic permission grants remain unchanged.
- git diff --check passed, including separate checks of newly added files.
- Verification used only the isolated PostgreSQL server on loopback port 55439, with dental_clinic_v19_verification_local and dental_clinic_v18_verification_compat. The disposable server was stopped after verification; test data/logs remain under target/pg-v19-verification and target. Build logs: target/v19-verify.log and target/v19-final-verify.log.

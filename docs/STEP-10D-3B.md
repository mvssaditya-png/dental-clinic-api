# Step 10D.3B: structured clinical entry edits and removal

Backend only. Migration: V18__case_sheet_entry_history.sql. Existing migrations are unchanged.

## Contracts

All paths start with /api/case-sheets/{caseSheetId}; optional clinicId query uses the existing effective-clinic resolver. Tenant users remain bound to their clinic; platform users must provide clinic context.

| Method | Suffix | Authority | Success |
| --- | --- | --- | --- |
| PUT | /diagnoses/{diagnosisId} | CASE_SHEET_EDIT | 200 diagnosis response |
| DELETE | /diagnoses/{diagnosisId} | CASE_SHEET_EDIT | 204 |
| PUT | /findings/{findingId} | CASE_SHEET_EDIT | 200 finding response |
| DELETE | /findings/{findingId} | CASE_SHEET_EDIT | 204 |
| GET | /entry-history?page=0&size=20 | CASE_SHEET_HISTORY_VIEW | 200 paged history |

PUT and DELETE require If-Match containing exactly one quoted nonnegative integer version, for example: If-Match: "3". Wildcards, weak validators and lists are rejected. Use the version returned in the existing GET list or POST/PUT response.

DELETE has no body. Require X-Removal-Reason: Entered against the wrong tooth. The value must be nonblank, at most 1000 characters, and contain no control characters; surrounding whitespace is trimmed. It is stored with the REMOVE audit event. Header transport supports plain text; clients must not send line breaks. International text/header encoding must be validated before a future frontend exposes non-ASCII reasons.

Diagnosis PUT JSON:

    {"diagnosisName":"Dental caries","diagnosisType":"PRIMARY","diagnosisCode":null,"toothNumber":"46","notes":null}

Finding PUT JSON:

    {"findingType":"Tenderness","findingValue":"Present","toothNumber":"46","notes":null}

Both are full replacement operations: omitted/blank optional values become null. Diagnosis type is required on PUT and must be PRIMARY, SECONDARY or DIFFERENTIAL. Existing POST default PRIMARY remains. Required names/types and database length limits are validated; no new tooth numbering or duplicate-entry rule is imposed.

Existing response fields remain, with version (integer) and updatedAt (nullable local datetime) added to diagnosis/finding POST, GET-list and PUT responses. Creation time is unchanged. Removed rows do not appear in normal lists.

Errors on the new endpoints: 400 invalid input/reason/precondition syntax; 401 unauthenticated; 403 missing authority/inactive clinic; 404 inaccessible parent/child; 409 non-DRAFT/already removed/lock conflict; 412 stale version; 428 missing If-Match. Existing endpoint error contracts are retained.

History response:

    {"content":[{"id":"audit UUID","caseSheetId":"UUID","entryType":"DIAGNOSIS","entryId":"UUID","operation":"REMOVE","actorId":"UUID","occurredAt":"local datetime","before":{},"after":{},"removalReason":"Correction"}],"number":0,"size":20,"totalElements":1,"totalPages":1}

Snapshots are typed EntrySnapshot records: id, caseSheetId, toothNumber, diagnosisName, diagnosisType, diagnosisCode, findingType, findingValue, notes, version, createdAt, updatedAt, removedAt, removedBy. Fields not applicable to the entry type are null. CREATE has a null before snapshot. UPDATE/REMOVE preserve both snapshots; removal keeps clinical values and records removal metadata. No arbitrary JSON, IP address or user agent is exposed. Size is capped at 100; negative page or size below 1 is rejected. Ordering is newest first with audit ID as tie-breaker.

## Security, transactions and retention

CASE_SHEET_HISTORY_VIEW is granted only to global system SUPER_ADMIN, ADMIN and DOCTOR roles. Existing view/edit permissions are unchanged. No role bypass is added. History validates the parent and queries clinic plus parent plus actual child linkage, including removed children.

All clinical child writes lock the parent row before checking DRAFT. Parent text updates and finalization use the same lock. Child @Version plus If-Match rejects stale editors. Finalization requires an active structured diagnosis; removal of the final active diagnosis is allowed in DRAFT but blocks finalization. FINALIZED and AMENDED reject edits/removal. There is no restore endpoint.

Audit insert and clinical mutation share one transaction. The audit writer requires an existing transaction. New CREATE, UPDATE and REMOVE events are recorded; pre-deployment history is not fabricated. Existing entries retain creation metadata, active state and version zero.

Database triggers reject updates/deletes of CASE_SHEET_ENTRIES audit rows, audit table truncation, physical deletion/truncation of the two clinical entry tables. Consequently existing cascading parent deletes cannot erase those children. Inserts and normal soft removal remain permitted. No existing FK is dropped.

Residual limits: database owners/superusers can disable/drop triggers or alter records directly; this is not cryptographic tamper evidence. Use restricted runtime DB credentials operationally. Existing timestamps retain the application's local-datetime convention. Identical clinical entries remain possible. Do not run old backend instances once soft removal is enabled because old queries include removed entries. Do not automatically retry uncertain writes. No application database migration was executed during this work; verification used an isolated database.

## Verification

New unit, HTTP, PostgreSQL transaction/concurrency and V17-to-V18 migration tests are under src/test/java/com/dentalclinic/casesheet. Enable CASE_SHEET_TEST_DATABASE=true only with a datasource database named dental_clinic_v18_verification_*. Existing clinic integration tests can also be enabled with CLINIC_TEST_DATABASE=true. Never point these tests at an application database. The verification database is retained for review.

## Changed files

- src/main/resources/db/migration/V18__case_sheet_entry_history.sql
- src/main/java/com/dentalclinic/casesheet/controller/CaseSheetEntryController.java
- src/main/java/com/dentalclinic/casesheet/controller/CaseSheetEntryErrors.java
- src/main/java/com/dentalclinic/casesheet/dto/CaseSheetDiagnosisResponse.java
- src/main/java/com/dentalclinic/casesheet/dto/CaseSheetClinicalFindingResponse.java
- src/main/java/com/dentalclinic/casesheet/dto/CreateCaseSheetDiagnosisRequest.java
- src/main/java/com/dentalclinic/casesheet/dto/CreateCaseSheetClinicalFindingRequest.java
- src/main/java/com/dentalclinic/casesheet/dto/UpdateDiagnosisRequest.java
- src/main/java/com/dentalclinic/casesheet/dto/EntrySnapshot.java
- src/main/java/com/dentalclinic/casesheet/dto/EntryHistoryResponse.java
- src/main/java/com/dentalclinic/casesheet/entity/CaseSheetDiagnosis.java
- src/main/java/com/dentalclinic/casesheet/entity/CaseSheetClinicalFinding.java
- src/main/java/com/dentalclinic/casesheet/repository/CaseSheetRepository.java
- src/main/java/com/dentalclinic/casesheet/repository/CaseSheetDiagnosisRepository.java
- src/main/java/com/dentalclinic/casesheet/repository/CaseSheetClinicalFindingRepository.java
- src/main/java/com/dentalclinic/casesheet/repository/EntryHistoryRepository.java
- src/main/java/com/dentalclinic/casesheet/service/CaseSheetService.java
- src/main/java/com/dentalclinic/casesheet/service/CaseSheetEntryService.java
- src/main/java/com/dentalclinic/casesheet/service/EntryAudit.java
- src/main/java/com/dentalclinic/casesheet/service/EntryException.java
- src/main/java/com/dentalclinic/common/exception/GlobalExceptionHandler.java
- src/test/java/com/dentalclinic/casesheet/CaseSheetEntryTest.java
- src/test/java/com/dentalclinic/casesheet/CaseSheetEntryDatabaseTest.java
- src/test/java/com/dentalclinic/casesheet/CaseSheetEntryMigrationTest.java
- src/test/java/com/dentalclinic/clinic/ClinicDatabaseIntegrationTest.java (V18 database guard and permission count)
- docs/STEP-10D-3B.md

## Final validation results

Maven verify completed successfully: 149 tests, 0 failures, 0 errors, 0 skipped. This includes 25 new case-sheet tests (13 unit, 11 database/HTTP, 1 legacy migration) and all existing integration tests. PostgreSQL verification used dental_clinic_v18_verification_20261017. Git diff --check passed. The packaged backend JAR was built. Frontend files and dependencies were not changed.

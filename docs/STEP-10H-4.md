# Step 10H.4 - Clinic consultation worklist

## API
GET /api/consultations?clinicId=<UUID>&page=0&size=20&search=<text>&status=<enum>&from=YYYY-MM-DD&to=YYYY-MM-DD

Requires CONSULTATION_VIEW, identical to existing Consultation Detail. Uses TenantClinicResolver: clinic-bound users remain in their own active clinic even if another clinicId is supplied; platform users must explicitly select an active clinic. No role grants or detailed clinical access rules changed. Discovery is clinic-wide, not restricted to an assigned doctor, consistent with existing detail authorization.

page defaults 0; size defaults 20 and clamps to 100. Negative page, nonpositive size, overflowing offset, malformed UUID/date/enum, search over 100 trimmed characters and reversed date bounds return 400. Invalid enum/query values are handled locally to this new controller, without changing global error behavior.

Search is trimmed, blank means absent, case-insensitive substring matching across patient full name, patient number, appointment number and consultation UUID (no human-readable consultation number exists). Percent, underscore and the ! escape character are escaped as literals. Parameters are bound, never concatenated into SQL. Date bounds are inclusive APPOINTMENT calendar dates, not consultation timestamp conversions.

Results sort startedAt DESC NULLS LAST, consultationId DESC. Results and count share clinic constraints on consultation/appointment/patient/doctor plus matching appointment patient. DTO constructor projection joins patient, doctor user and appointment; no Case Sheet, diagnoses, findings or procedure collections are fetched. No new indexes/migrations/dependencies.

Response envelope:
```json
{"content":[{"consultationId":"<UUID>","clinicId":"<UUID>","appointmentId":"<UUID>","appointmentNumber":"APT00001","patientId":"<UUID>","patientName":"Synthetic Patient","patientNumber":"PAT00001","doctorName":"Doctor Name","consultationStatus":"STARTED","appointmentStatus":"IN_CONSULTATION","startedAt":"2026-11-01T09:00:00","endedAt":null,"appointmentDate":"2026-11-01"}],"number":0,"size":20,"totalElements":1,"totalPages":1}
```
Actual persisted values only. Legacy null start times remain null. Empty results return content=[] and totalPages=0.

## Frontend
- Existing /consultations sidebar route now renders ConsultationWorklistScreen.
- Runtime consultations.view and effective active clinic required before requests.
- 250ms debounced search/filters; filter changes clear rows immediately and reset page. No automatic all-page fetch.
- Query responses validate identity, clinic, enums, timestamps, dates and page envelope. Request generation and store ownership suppress late results after clinic/account/permission changes or unmount.
- Desktop table at >=768px; mobile cards below. No editing actions. Eye/View Consultation opens the existing detail route with actual consultation ID and appointment ID.
- Worklist-origin details have Back to Consultations; direct appointment-origin details retain Back to Appointment. Back restores filters/page via validated route context only when the clinic matches. Refresh clamps pages if results shrink.
- This step does not propagate worklist return parameters through unrelated descendant Case Sheet routes; those retain their existing return behavior. Browser/back stack remains available. No duplicated detail screen.
- Existing completion, finalization and patient-level clinical modules unchanged.

## Files
Backend: ConsultationDiscoveryController.java, ConsultationDiscoveryService.java, ConsultationSummary.java, ConsultationPage.java, ConsultationRepository.java, ConsultationDiscoveryDatabaseTest.java, this document.
Frontend: app/(app)/(main)/consultations.tsx, ConsultationWorklistScreen.tsx, consultation-worklist.ts, ConsultationDetailScreen.tsx (Back navigation only), tests/consultation-worklist.test.ts.
Previous uncommitted Step 10H.3A UI changes preserved.

## Validation and manual checklist
- 12 PostgreSQL discovery tests: passed (search, literal wildcards, statuses, dates, pagination/ties/null starts, tenant isolation/counts, permission, platform/active clinic, no N+1/entity collections/writes, HTTP invalid inputs).
- 28 focused frontend tests passed. Full frontend suite: 1403 passed, zero failures/skips. TypeScript and Web export passed.
- PostgreSQL tests use only dental_clinic_v18_verification_10h2_20261110 on localhost; test fixtures roll back. No application/shared/production migrations.
- Package verification uses generated target/10h4-package-pom.xml and target/10h4-package output to avoid touching the launcher JAR.

Manual (not performed): stop the local backend before rebuilding its normal executable JAR, then restart through the existing local-web launcher. Check authorized/denied users and platform clinic selection; names/numbers/UUID search; all status filters; date bounds; desktop 768/1024/1440 and mobile 320/375/430; long names; pagination and Back preservation; appointment-origin Back; clinic/account switching during slow reads; completed visit review without new list mutations.

Limits: contains search/cast UUID may need dedicated indexes at large scale; no speculative schema change added. Stable offset pagination can shift as new visits arrive. Null legacy start values do not invent dates. Existing detail API/client compatibility for legacy data remains separate. No browser/physical-device verification or deployment performed.

Validation update: separate-output Maven verify/package passed. No normal launcher JAR was replaced. Full frontend 1403/1403, focused frontend 28/28, TypeScript/Web export and frontend diff check passed. Lint could not run because no lint script is configured.

The initial full backend run found a pre-existing lifecycle fixture clock-skew failure: its DB-default started_at was 0.8ms later than JVM endedAt. ConsultationLifecycleDatabaseTest fixture now explicitly creates an already-started visit one minute earlier. This is a synthetic test-only correction; production timestamp generation and transitions are unchanged. Added this test file to the changed-file inventory. Full suite rerun passed.

Final verification (2026-11-11): backend full suite 351 discovered, 239 passed, 112 opt-in tests skipped, zero failures/errors; includes all 12 discovery PostgreSQL tests. Separate-output Maven verify/package passed with executable Spring Boot repackaging. Full frontend suite 1403 passed, zero failures/skips; TypeScript, Web export and both repository diff checks passed. No new implementation defects found during resumed verification. Lint remains unavailable (no configured script). Manual browser/device verification is still required.

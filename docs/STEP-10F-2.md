# Step 10F.2 - Patient clinical visits

GET /api/patients/{patientId}/clinical-visits?clinicId=...&page=0&size=20&excludeConsultationId=...

Requires both CONSULTATION_VIEW and CASE_SHEET_VIEW. No role-based bypass or new grants.
TenantClinicResolver preserves clinic-bound context; platform users must supply an active clinic.
Missing/wrong-clinic patients return 404. Invalid page/size returns 400; size is capped at 100,
page must be nonnegative, and offsets exceeding Integer.MAX_VALUE are rejected.

Envelope: content, number, size, totalElements, totalPages (same discovery conventions).
Each row: consultationId, consultationStatus, startedAt, endedAt, appointmentId,
appointmentNumber, appointmentDate, appointmentStartTime, doctorId, doctorName,
caseSheetId, caseSheetNumber, caseSheetStatus, visitNumber, chiefComplaint.
The five Case Sheet fields are null when no matching Case Sheet exists. endedAt can be null.
Doctor name is the current doctor's user display name, not a historical snapshot.

All consultation statuses are included independently of appointment status. Appointments without
consultations never appear. Each consultation is a separate visit even on the same date.
The excluded UUID is only a row-exclusion predicate; it does not cause another consultation lookup.

Ordering: startedAt DESC NULLS LAST, consultation ID DESC. Legacy null starts, if encountered,
follow every dated consultation and are ordered by ID; returned timestamps remain null.
The current V5 schema and entity require startedAt, so this fallback is defensive.

A constructor projection joins appointment and doctor/user, with a left join to the optional
Case Sheet. Both results and counts are clinic/patient scoped, with the same exclusion predicate.
Related appointment/doctor clinic and appointment patient are checked defensively. Case Sheet
join also checks clinic and patient. No full Case Sheet, diagnoses or findings are hydrated.
Patient validation plus page/count queries are bounded, not proportional to visits. No migration
or new index: existing clinic/patient data is used; assess query plans if volume later warrants it.

All service reads are transactional(readOnly=true); there are no mutations or lifecycle calls.
PostgreSQL tests run only with TREATMENT_ITEM_TEST_DATABASE=true and assert the disposable database
name prefix dental_clinic_v19_verification_. Fixtures roll back. The legacy-null test temporarily
relaxes NOT NULL within its rolled-back test transaction, never in the application database.

Validation: see target/clinical-visits-focused.log and target/clinical-visits-verify.log.

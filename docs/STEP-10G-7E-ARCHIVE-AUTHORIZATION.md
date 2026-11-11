# Step 10G.7E — Creator-based archive authorization

## Findings and implementation

Odontogram already persists nullable `created_by`. Creation assigns the authenticated user; tooth mutations only change `updated_by`. Prior archival required ODONTOGRAM_ARCHIVE but did not check ownership.

The new OdontogramArchiveAuthorization policy requires ODONTOGRAM_ARCHIVE plus either an ADMIN/SUPER_ADMIN runtime authority or a DOCTOR runtime authority whose user ID equals persisted created_by. Null/unavailable creator denies doctors. Other roles are denied even if accidentally assigned archive permission. Multiple roles follow the privileged ADMIN/SUPER_ADMIN branch when present. No grants changed.

The archive service invokes this policy after clinic-scoped pessimistic chart lookup and before mutation. Denial uses the existing generic AccessDeniedException/403 handling. Client ownership fields are never consumed. TenantClinicResolver behavior, aggregate preconditions, Unicode reason validation, transactional audit and chart locking are preserved.

JPA created_by is now updatable=false. No normal update API changes ownership. This is application mapping protection, not a new database trigger against privileged direct SQL.

## Read contract and frontend

OdontogramResponse adds `archiveAllowed`, a server-computed boolean for the current authenticated caller and ACTIVE state. This avoids exposing additional creator identity or duplicating role policy in the frontend. Existing reads and pagination are unchanged. Frontend requires this flag to be exactly true, plus existing view/archive capabilities and active status. Missing flags fail closed. Fresh pre-submit reads recheck eligibility. Server enforcement remains authoritative.

An old running backend will not return the flag, so Remove stays hidden until the updated backend is started. Deploy backend before frontend where practical. This does not affect archived read access.

## Migration and files

No migration, ownership backfill, permission grant or clinical-data change is needed. Existing V25 is unchanged.

Backend: Odontogram.java; OdontogramResponse.java; OdontogramService.java; OdontogramArchiveService.java; new OdontogramArchiveAuthorization.java; OdontogramArchiveDatabaseTest.java; this document.

Frontend: odontogram.ts; OdontogramArchive.tsx; odontogram-archive.ts; odontogram-archive.test.ts.

## Verification

69 focused backend tests passed, zero failed/skipped, using only dental_clinic_v21_verification_archive_20261111. Includes creator match/mismatch/unknown, administrative permission requirements, receptionist/attender denial, forged body fields, cross-clinic/platform context and existing precondition/audit/concurrency tests. The archive class inherits existing mutation tests, so totals include repeated regression coverage.

128 focused frontend tests passed, zero failures/skips. TypeScript, Web export and diff checks passed. No configured lint script exists. Separate-output Maven verify/package preserves the running application JAR.

Resume verification confirmed the completed logs and existing implementation; no feature was reimplemented. Maven artifact: `target/10g7-package/dental-clinic-api-0.0.1-SNAPSHOT.jar` (packaged 11 Nov 2026, 15:59). The standard `target/dental-clinic-api-0.0.1-SNAPSHOT.jar` remained dated 15:17 and was not replaced. Web export: `%TEMP%/dental-10g7e-export`. Logs: `%TEMP%/10g7e-backend.log`, `10g7e-frontend.log`, `10g7e-package.log`, and `10g7e-export.log`.

No failing focused tests remain. Full unrelated backend/frontend suites were not rerun for this authorization-only step. Manual browser/iPhone/Android and live role-session checks remain unverified. The updated backend must be started before the new eligibility flag can enable archive actions; packaging alone does not activate it.

## Manual verification

With two doctors in one clinic: creator sees Remove and can archive; other doctor sees no Remove and receives 403 for a direct attempt. ADMIN and SUPER_ADMIN with permission can archive within resolved clinic scope. Receptionist/attender cannot. Test legacy null creators, stale versions, foreign-clinic IDs and unchanged archived read-only history. Do not perform this checklist on real clinical charts; use approved synthetic records.

No deployment or application database migration was performed. Role/permission changes in existing authenticated sessions follow the application's existing refresh/revalidation behavior; this step does not redesign authentication.

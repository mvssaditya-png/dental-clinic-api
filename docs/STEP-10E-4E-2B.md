# Step 10E.4E.2B - Treatment Plan soft-removal API

## Contract

`POST /api/treatment-plans/{id}/remove?clinicId=...`

Requires authenticated `TREATMENT_PLAN_EDIT`. Clinic-bound users retain existing
effective-clinic resolution; platform users must supply an operational clinic.
Send `If-Match: "3"` and JSON `{"reason":"Duplicate draft"}`. Success is HTTP 204
with no body, after transaction commit. The existing plan-detail response now
includes the additive aggregate `version` field; header PUT still does not
require If-Match. V19 item versions remain separate.

Errors follow V19: 400 invalid reason/version, 401/403 authentication/permission,
404 missing or outside effective clinic, 409 lifecycle/removed/downstream conflict,
412 stale aggregate version, 428 missing If-Match. Version is checked after
locking and before lifecycle checks, so repeating an old successful request
returns 412 rather than implying idempotent success.

Reasons use the existing Unicode validator: required, nonblank, maximum 1,000
code points, no C0/C1 controls or unpaired UTF-16 surrogates. Accepted text is
stored verbatim. No automatic retry is implemented.

## Transaction and retention

Only active DRAFT plans are eligible, including populated drafts and drafts
whose originating Case Sheet is FINALIZED. No Case Sheet mutation occurs.
The parent write lock serializes removal with header, item and status mutations.
All existing plan/item mutation paths reject removed parents.

Removal retains all items (including already removed items), statuses, notes,
prices, discounts, totals, item audit and encounter references. Only parent removal
metadata, updater/time and aggregate revision change. One immutable REMOVE audit
event records actor, timestamp, reason, pre-removal plan snapshot, resulting
version/removal metadata and every attached item's ID/version. Audit failure
rolls the entire transaction back. V20 and earlier migrations are unchanged.

References checked: invoice -> plan; invoice item -> any child; material usage ->
any child; consent and follow-up -> plan or any child. Checks intentionally include
previously removed children and do not assume DRAFT means unreferenced.

## Downstream locking boundary

Invoice creation is the only currently implemented downstream reference-creation
service. It now locks the parent first and rejects removed plans before creating
invoices/items. Its existing billable-status rules remain unchanged. Consent,
follow-up and material-usage tables exist, but no creation services for those
references exist in the inspected source. Future writers must adopt the same
parent-first locking and active-parent check. Arbitrary SQL writers do not follow
this protocol and are not covered; this is not a database-trigger guarantee.

## Deliberately deferred

No frontend, recovery/history endpoint, permission change, new migration or active
list filtering. Existing reads/lists retain their behavior and can still display
removed plans. Do not expose removal in the frontend until the later read/recovery
and filtering work is complete. No physical deletion or cancellation cascade.

## Verification

Use only the disposable PostgreSQL test database with
`TREATMENT_ITEM_TEST_DATABASE=true` and a name beginning
`dental_clinic_v19_verification_`, as required by the existing tests. Set the
datasource environment to that isolated server, never application data.
Run `mvn -Dtest=TreatmentPlan*Test test` and `mvn verify`.

Coverage includes HTTP 204/400/401/403/428, service version/lifecycle errors,
Unicode boundaries, empty/populated removal, finalized origin, all downstream
reference types (including removed children), unchanged item/history/pricing data,
tenant/permission enforcement, audit rollback, concurrent item/header/status
mutations, and both invoice/removal lock orderings.

Executed results: initial focused run 67 passed; final full Maven verify/package
230 discovered, 208 passed, 22 unrelated database-gated tests skipped, zero failures
or errors. The final run includes 69 treatment-plan tests, of which 44 are PostgreSQL
service/migration tests. All diff whitespace checks passed, including untracked files.
Logs: `target/plan-removal-focused.log`, `target/plan-removal-verify.log`.
Only the disposable database `dental_clinic_v19_verification_v20` on loopback
port 55440 was used. Its server was stopped after validation; no application
database migration or clinical operation was performed.

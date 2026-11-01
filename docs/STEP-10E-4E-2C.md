# Step 10E.4E.2C - Removal recovery, history and active reads

## Contracts

`GET /api/treatment-plans/{id}/removal-state?clinicId=...`
requires `TREATMENT_PLAN_VIEW`. Example:

```json
{"id":"11111111-1111-1111-1111-111111111111","version":3,"removed":true}
```

Only those three fields are returned. No actor, reason, timestamp or clinical
snapshot is exposed through recovery. It works for active and removed plans;
missing or cross-clinic identities return 404. It never retries a mutation.

`GET /api/treatment-plans/{id}/history?clinicId=...&page=0&size=20`
requires `TREATMENT_PLAN_HISTORY_VIEW`, using unchanged role grants. Returns
`content`, `number`, `size`, `totalElements`, `totalPages`. Each content event has
`id`, `operation`, `actorId`, `timestamp`, `removalReason`, `planSnapshot`, and
`retainedItems` (objects with `itemId` and `version`). Actor identity is the stored
user UUID; no fabricated display name. Page defaults to 0, size to 20, size is
capped at 100. Negative page, nonpositive size and overflowing offset return 400.
Ordering is `createdAt DESC, id DESC`. Empty audit history is an empty page.

Both endpoints use effective clinic resolution, including explicit operational
clinic for platform users. They require a clinic-scoped parent lookup that includes
removed rows. Existing item-history reads now use that same unfiltered identity
lookup, retaining their separate history permission and existing response contract.

## Ordinary reads

Clinic, patient and Case Sheet discovery projections and matching count queries
exclude removed parents, including search/status filters. Older unpaginated patient
and Case Sheet repository queries also exclude removed parents. Existing ordering,
pagination and DTOs are preserved. Number generation and uniqueness still include
removed records; numbers are not recycled.

Ordinary plan-detail and item-list endpoints return 404 for removed parents.
No global Hibernate filter was introduced: audit/recovery and existing historical
entity references remain intact. Removal already blocks protected downstream
references, so ordinary read filtering does not hide a legitimately removed plan
needed by an existing invoice. Billing record reads themselves are unchanged.

All Step 2B mutation guards remain: header/status/item writes and invoice creation
reject removed parents under the existing parent-lock protocol. No removal retry,
new mutation, migration, role grant, frontend change or physical deletion.

## Validation

PostgreSQL tests use only the disposable loopback server on port 55440 and
`dental_clinic_v19_verification_v20`, with the existing
`TREATMENT_ITEM_TEST_DATABASE=true` gate. They cover recovery/history authorization,
cross-clinic/missing records, post-removal history, strict minimal recovery JSON,
history ordering/page bounds, active-list counts/search/status/pagination, legacy
discovery and numbering, plus all existing removal/mutation/concurrency regressions.

Run focused `mvn -Dtest=TreatmentPlan*Test test`, then `mvn verify` with the isolated
datasource environment. Never point these fixture-writing tests at application data.
Logs: `target/plan-removal-reads-focused.log` and `target/plan-removal-reads-verify.log`.

Executed: focused tests 74 passed; full Maven verify/package 235 discovered,
213 passed, 22 unrelated database-gated tests skipped, zero failures/errors.
Includes 49 PostgreSQL treatment-plan service/migration tests. Tracked and new-file
diff whitespace checks passed. The disposable server was stopped after validation;
the application database and frontend were not changed.

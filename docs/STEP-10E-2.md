# Step 10E.2 - Treatment plan discovery

Backend-only, read-only discovery. Existing treatment-plan endpoints and lifecycle rules are unchanged.

## Endpoints

- GET /api/patients/{patientId}/treatment-plans?clinicId=...&page=0&size=20
- GET /api/case-sheets/{caseSheetId}/treatment-plans?clinicId=...&page=0&size=20

Both require TREATMENT_PLAN_VIEW. Role names alone do not authorize access.
TenantClinicResolver remains authoritative: clinic-bound users use their own active clinic (a supplied clinic override is ignored); platform users must supply an existing active clinic.
The requested parent must exist in the effective clinic before any plan query runs.

## Pagination and response

Zero-based page, default 0. Default size 20, maximum 100 (larger values are capped).
Negative page, nonpositive size, nonnumeric values, invalid UUIDs and offsets beyond Integer.MAX_VALUE are rejected with HTTP 400.
Missing/out-of-clinic parents return HTTP 400 using existing business-error conventions, without distinguishing foreign records. A missing path segment is not a valid route (404).
Valid parents with no plans return 200 with empty content. Pages beyond the last page are empty.
Order is createdAt DESC, id DESC; sorting is fixed, not client-controlled. Database pagination and explicit count queries are used; no collection fetch joins.

Both responses have the shape:

```json
{
  "content": [{
    "id": "11111111-1111-4111-8111-111111111111",
    "clinicId": "22222222-2222-4222-8222-222222222222",
    "treatmentPlanNumber": "TP000001",
    "title": "Restorative care",
    "patientId": "33333333-3333-4333-8333-333333333333",
    "caseSheetId": "44444444-4444-4444-8444-444444444444",
    "consultationId": "55555555-5555-4555-8555-555555555555",
    "status": "DRAFT",
    "estimatedTotalVisits": 2,
    "finalAmount": 2500.00,
    "createdAt": "2026-10-20T10:30:00",
    "updatedAt": "2026-10-20T10:30:00"
  }],
  "number": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

Values are existing stored fields, including nullable title, estimated visits and originating references. finalAmount is the existing plan amount, not payments or outstanding balance. No currency or timezone is invented. No notes or item collections are returned. All plan statuses are included.

## Validation

Service tests cover parent existence, empty/multiple results, pagination bounds, effective clinic behavior and inactive/missing platform contexts. MVC/method-security tests cover permissions, parameter binding, compact response serialization and existing detail/items routes. Hibernate validates both JPQL projections and count queries without a database or schema actions.

No migrations, dependencies or frontend changes. Offset pagination is deterministic for unchanged data; concurrent inserts can move records between pages. Live PostgreSQL ordering/execution and performance are not exercised by these database-free tests.

# Step 10G.4A - Odontogram mutation safety

Backend only. Existing permissions (including ATTENDER's ODONTOGRAM_EDIT) are unchanged.

## Tooth PUT

`PUT /api/odontograms/{id}/teeth/{toothNumber}` retains its JSON body:
`clinicId`, required `caseSheetId`, nullable `conditionId`, nullable `notes`, optional `changeReason`.
Omitted/null conditions and omitted/blank notes retain replacement/clearing semantics.

- Normalized mutation tooth number must match `^[1-4][1-8]$` (400 otherwise).
- Existing tooth: require `If-Match: "<nonnegative version>"`.
- Absent tooth: require `If-None-Match: *`.
- Neither header: 428. Both headers, malformed values, overflow: 400.
- Existing tooth with create-only header, absent tooth with If-Match, stale version: 412.
- Existing lifecycle/context/no-change validation remains 400.
- Successful PUT remains 200; tooth GET and PUT responses now include numeric `version`.
- A newly created tooth starts at version 0; each successful replacement increments it.
- Client must reload and review after 412. Never automatically replay an uncertain write.

This intentionally tightens the mutation contract. Legacy PUT callers must supply preconditions.
Existing read callers can ignore the additive version field. Legacy tooth identifiers remain readable.

## Locking and transactions

Tooth writes lock Case Sheet, then Odontogram, then modify the versioned tooth.
The parent chart lock serializes first insertion across different Case Sheets.
Case Sheet finalization already locks the same Case Sheet row; its lifecycle rules are unchanged.
Tooth state and history are flushed together before constructing the response, in one transaction.
Optimistic failures are mapped narrowly to 412 and roll back history too.

Chart creation locks the clinic-scoped patient row before checking for an existing chart.
Duplicate active or archived charts return 409; creation remains explicit, returns 201, and does not require a Case Sheet.
No automatic reuse/reactivation and no new role or doctor-ownership restrictions.

## Migration V21

- Adds nonnegative tooth version with initial value 0 for existing rows.
- Preserves all identifiers/notes/history; no FDI database CHECK or data cleanup.
- Changes history chart/tooth FKs from CASCADE to RESTRICT.
- Rejects history UPDATE, DELETE and TRUNCATE using database triggers.
- Adds stable history ordering index; repository orders changedAt DESC, id DESC.
  Equal timestamp UUID order is deterministic, not evidence of actual temporal order.
- Does not snapshot historical condition names; existing condition references are unchanged.

## Verification

`ToothPreconditionTest` exercises precondition syntax, state matching and adult numbering.
`OdontogramDatabaseTest` is opt-in via ODONTOGRAM_TEST_DATABASE=true and rejects datasource
URLs outside localhost:5432/dental_clinic_v21_verification_* before application startup.
It tests PostgreSQL writes/concurrency, both finalization race orders, different encounters,
HTTP contracts, authorization, migration from V20 with a legacy tooth, Flyway validate,
append-only protections, atomic rollback and stable history ordering.
Use an isolated disposable verification database; never point these tests at clinical data.

Operational limitations: privileged database administrators can bypass ordinary trigger protection.
Chart locking serializes writes to different teeth of the same chart as a deliberate simple safety tradeoff.
No automatic retry/idempotency guarantee is introduced. Fresh reads remain necessary after uncertain outcomes.

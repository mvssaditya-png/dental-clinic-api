# Step 10G.5 — Conditions and colour configuration

## Completion and verification (2026-11-09)
Resumed existing backend commit 8e9555b and frontend commit 55a2d71 without restarting or reverting them. No application database or shared clinical data was changed. No deployment performed.

Backend focused package: 35 passed, 0 failed/errors/skipped (10 configuration PostgreSQL tests, 20 Odontogram PostgreSQL tests, 5 precondition tests). Isolated database: dental_clinic_v21_verification_cfg_20261109110950 on local PostgreSQL. Maven package succeeded.
Frontend focused suite: 206 passed, 0 failed/skipped; TypeScript and Expo Web export passed. No lint script is configured. Physical-device/browser visual verification was not performed.

## Migration V22
V22__odontogram_condition_configuration.sql adds condition versions, active-name uniqueness, immutable configuration audit, and immutable history-label companion records. V21 is unchanged. Existing condition IDs, references, observations and clinical history rows are preserved.
Legacy companion labels use the catalogue name/code at migration time and are marked BASELINE, not verified event-time labels. New history inserts atomically capture EVENT_TIME labels via a database trigger. History rendering uses these snapshots, never the current catalogue name. Missing snapshots are marked unavailable. UPDATE/DELETE/TRUNCATE protections apply to companion labels and configuration audit.

Existing and future clinics receive missing defaults: Dental Caries, Healthy, Filling, Root Canal Treatment, Crown, Missing Tooth, Implant, Fractured Tooth, Extraction Indicated. Initialization matches existing codes/names, preserves inactive records and customized colours, and creates no patient observations. Colour preferences are not universal clinical standards.

Migration prerequisite: legacy duplicate active normalized condition names must be assessed before rollout; the uniqueness constraint intentionally fails rather than merging or rewriting clinical identities. Migration was verified only in isolated databases here.

## API contract
- GET /api/odontogram-conditions?clinicId=...&includeInactive=false: active catalogue; ODONTOGRAM_VIEW or ODONTOGRAM_CONFIGURE. includeInactive=true requires ODONTOGRAM_CONFIGURE.
- GET /api/odontogram-conditions/{id}?clinicId=...: clinic-scoped condition including version and nullable colourHex.
- POST /api/odontogram-conditions: ODONTOGRAM_CONFIGURE, 201; existing create fields plus optional colourHex. Omitted code is generated. Name required (150 maximum), description/patientExplanation 4000 maximum, valid #RRGGBB colour.
- PUT /api/odontogram-conditions/{id}?clinicId=...: ODONTOGRAM_CONFIGURE, quoted If-Match version; body conditionName, description, colourHex, active, terminologyOnlyConfirmation. Returns 200 managed condition with incremented version. Code/identity preserved.
- Existing colour-config POST now requires the condition's quoted If-Match version and increments that version atomically. Clients must obtain the condition version before writing; legacy unversioned writers receive 428. Colour read response remains compatible.
- Missing precondition 428; malformed precondition/validation 400; stale version 412; duplicate/rename acknowledgement conflict 409; unavailable or wrong-clinic condition 404; authorization follows existing 401/403 handling.
- Tooth history adds labelSource (BASELINE/EVENT_TIME/UNAVAILABLE). No observation-write contract changes.

Effective clinic resolution and ODONTOGRAM_CONFIGURE grants are unchanged. Condition row locks/version checks serialize configuration writes; active-condition validation uses a read lock. Configuration changes never rewrite tooth observations. Audit stores actor, timestamp and before/after configuration atomically.

## Frontend
Manage ? Odontogram Conditions provides permission-gated list, add/edit, activation/deactivation, preset/HEX colour selection, terminology acknowledgement and read-before-retry recovery. Stale clinic/session results are discarded. Observation selection remains active-only; inactive existing conditions remain readable. Chart colour refresh uses existing configurations including colours for inactive historical conditions. All five observation states remain independently labelled/icon-coded; a cleared state requires loaded history.

This continuation fixed mobile identity sizing and Strict Mode editor lifecycle cleanup, added configuration/colour regression coverage, and updated stale navigation/authorization test expectations without changing grants or authority behavior.

## Limitations and manual checks
Terminology acknowledgement cannot algorithmically determine clinical meaning. A meaning change must use a new condition and deactivate the old one. BASELINE names cannot recover unknown historical names. Privileged database administrators can bypass database protections. Uncertain creates are never automatically replayed; review latest catalogue before another attempt. No configuration-audit history UI is added.

On Web and iPhone: verify Manage visibility, add/edit presets and HEX, terminology acknowledgement, deactivate/reactivate, conflict recovery, long names and narrow cards, and keyboard/close behavior. Verify refreshed chart colours, inactive observation readability, baseline notice, all five tooth states and unchanged Add/Edit/Clear plus Case Sheet viewer state. Do not use real clinical records for manual mutation checks.

## Backend implementation files
src/main/java/com/dentalclinic/odontogram/controller/OdontogramColourConfigController.java
src/main/java/com/dentalclinic/odontogram/controller/OdontogramConditionController.java
src/main/java/com/dentalclinic/odontogram/dto/CreateOdontogramConditionRequest.java
src/main/java/com/dentalclinic/odontogram/dto/ManagedOdontogramConditionResponse.java
src/main/java/com/dentalclinic/odontogram/dto/OdontogramToothHistoryResponse.java
src/main/java/com/dentalclinic/odontogram/dto/UpdateOdontogramConditionRequest.java
src/main/java/com/dentalclinic/odontogram/entity/OdontogramCondition.java
src/main/java/com/dentalclinic/odontogram/entity/OdontogramHistoryLabel.java
src/main/java/com/dentalclinic/odontogram/entity/OdontogramToothHistory.java
src/main/java/com/dentalclinic/odontogram/repository/OdontogramColourConfigRepository.java
src/main/java/com/dentalclinic/odontogram/repository/OdontogramConditionRepository.java
src/main/java/com/dentalclinic/odontogram/repository/OdontogramToothHistoryRepository.java
src/main/java/com/dentalclinic/odontogram/service/OdontogramColourConfigService.java
src/main/java/com/dentalclinic/odontogram/service/OdontogramConfigurationService.java
src/main/java/com/dentalclinic/odontogram/service/OdontogramService.java
src/main/resources/db/migration/V22__odontogram_condition_configuration.sql
src/test/java/com/dentalclinic/odontogram/OdontogramConfigurationDatabaseTest.java

## Frontend files in resumed implementation baseline
app/(app)/(main)/odontogram-conditions.tsx
src/features/odontogram/DentalChart.tsx
src/features/odontogram/ObservationCaseSheet.tsx
src/features/odontogram/ObservationDialog.tsx
src/features/odontogram/OdontogramConditionsScreen.tsx
src/features/odontogram/OdontogramMutations.tsx
src/features/odontogram/OdontogramScreen.tsx
src/features/odontogram/ToothHistory.tsx
src/features/odontogram/condition-configuration.ts
src/features/odontogram/odontogram-mutation.ts
src/features/odontogram/tooth-history.ts
src/features/odontogram/tooth-presentation.ts
src/navigation/app-navigation.ts
src/navigation/route-access.ts
src/navigation/shell-presentation.ts
tests/odontogram-case-sheet.test.ts
tests/odontogram-ui.test.ts

## Files changed in final verification continuation
 M src/features/odontogram/OdontogramConditionsScreen.tsx
 M tests/authorization.test.ts
 M tests/navigation.test.ts
 M tests/odontogram-ui.test.ts
?? tests/odontogram-configuration.test.ts

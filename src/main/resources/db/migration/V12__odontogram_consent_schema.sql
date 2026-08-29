-- =========================================================
-- DENTAL CLINIC APP
-- V12 - ODONTOGRAM SNAPSHOT & PATIENT CONSENT
-- =========================================================


-- =========================================================
-- 1. ODONTOGRAM SNAPSHOT
--
-- Immutable snapshot of an odontogram at a specific point
-- in the patient's clinical journey.
--
-- Typical use:
-- Doctor finalizes case sheet
--      ↓
-- Odontogram snapshot created
--      ↓
-- PDF generated
--      ↓
-- Historical chart remains unchanged forever
-- =========================================================

CREATE TABLE odontogram_snapshot (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                     clinic_id UUID NOT NULL,

                                     odontogram_id UUID NOT NULL,
                                     patient_id UUID NOT NULL,

                                     appointment_id UUID,
                                     consultation_id UUID,
                                     case_sheet_id UUID,

                                     snapshot_number VARCHAR(50) NOT NULL,

                                     snapshot_type VARCHAR(30) NOT NULL DEFAULT 'FINAL',

                                     status VARCHAR(30) NOT NULL DEFAULT 'FINALIZED',

                                     notes TEXT,

                                     finalized_by UUID NOT NULL,
                                     finalized_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                     pdf_url TEXT,

                                     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                     CONSTRAINT fk_odontogram_snapshot_clinic
                                         FOREIGN KEY (clinic_id)
                                             REFERENCES clinic(id),

                                     CONSTRAINT fk_odontogram_snapshot_odontogram
                                         FOREIGN KEY (odontogram_id)
                                             REFERENCES odontogram(id),

                                     CONSTRAINT fk_odontogram_snapshot_patient
                                         FOREIGN KEY (patient_id)
                                             REFERENCES patient(id),

                                     CONSTRAINT fk_odontogram_snapshot_appointment
                                         FOREIGN KEY (appointment_id)
                                             REFERENCES appointment(id),

                                     CONSTRAINT fk_odontogram_snapshot_consultation
                                         FOREIGN KEY (consultation_id)
                                             REFERENCES consultation(id),

                                     CONSTRAINT fk_odontogram_snapshot_case_sheet
                                         FOREIGN KEY (case_sheet_id)
                                             REFERENCES case_sheet(id),

                                     CONSTRAINT fk_odontogram_snapshot_finalized_by
                                         FOREIGN KEY (finalized_by)
                                             REFERENCES app_user(id),

                                     CONSTRAINT uq_odontogram_snapshot_number
                                         UNIQUE (
                                                 clinic_id,
                                                 snapshot_number
                                             ),

                                     CONSTRAINT chk_odontogram_snapshot_type
                                         CHECK (
                                             snapshot_type IN (
                                                               'INITIAL',
                                                               'FINAL',
                                                               'FOLLOW_UP',
                                                               'MANUAL'
                                                 )
                                             ),

                                     CONSTRAINT chk_odontogram_snapshot_status
                                         CHECK (
                                             status IN (
                                                        'FINALIZED',
                                                        'VOIDED'
                                                 )
                                             )
);


CREATE INDEX idx_odontogram_snapshot_patient
    ON odontogram_snapshot (
                            patient_id,
                            finalized_at DESC
        );

CREATE INDEX idx_odontogram_snapshot_odontogram
    ON odontogram_snapshot (
                            odontogram_id,
                            finalized_at DESC
        );

CREATE INDEX idx_odontogram_snapshot_case_sheet
    ON odontogram_snapshot (case_sheet_id);

CREATE INDEX idx_odontogram_snapshot_appointment
    ON odontogram_snapshot (appointment_id);

CREATE INDEX idx_odontogram_snapshot_consultation
    ON odontogram_snapshot (consultation_id);


-- =========================================================
-- 2. ODONTOGRAM SNAPSHOT TOOTH
--
-- Immutable tooth state captured at snapshot time.
--
-- IMPORTANT:
-- We snapshot clinical AND presentation information.
--
-- This means changing:
-- condition name
-- patient explanation
-- colour
--
-- in Settings later will NOT change historical PDFs.
-- =========================================================

CREATE TABLE odontogram_snapshot_tooth (
                                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                           clinic_id UUID NOT NULL,

                                           snapshot_id UUID NOT NULL,

                                           odontogram_tooth_id UUID,

                                           tooth_number VARCHAR(10) NOT NULL,

                                           condition_id UUID,

                                           condition_code_snapshot VARCHAR(100),
                                           condition_name_snapshot VARCHAR(150),

                                           condition_description_snapshot TEXT,
                                           patient_explanation_snapshot TEXT,

                                           colour_hex_snapshot VARCHAR(20),

                                           notes TEXT,

                                           source_case_sheet_id UUID,
                                           source_appointment_id UUID,

                                           last_changed_by UUID,
                                           last_changed_at TIMESTAMP,

                                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                           CONSTRAINT fk_odontogram_snapshot_tooth_clinic
                                               FOREIGN KEY (clinic_id)
                                                   REFERENCES clinic(id),

                                           CONSTRAINT fk_odontogram_snapshot_tooth_snapshot
                                               FOREIGN KEY (snapshot_id)
                                                   REFERENCES odontogram_snapshot(id)
                                                   ON DELETE CASCADE,

                                           CONSTRAINT fk_odontogram_snapshot_tooth_current
                                               FOREIGN KEY (odontogram_tooth_id)
                                                   REFERENCES odontogram_tooth(id),

                                           CONSTRAINT fk_odontogram_snapshot_tooth_condition
                                               FOREIGN KEY (condition_id)
                                                   REFERENCES odontogram_condition(id),

                                           CONSTRAINT fk_odontogram_snapshot_tooth_case_sheet
                                               FOREIGN KEY (source_case_sheet_id)
                                                   REFERENCES case_sheet(id),

                                           CONSTRAINT fk_odontogram_snapshot_tooth_appointment
                                               FOREIGN KEY (source_appointment_id)
                                                   REFERENCES appointment(id),

                                           CONSTRAINT fk_odontogram_snapshot_tooth_changed_by
                                               FOREIGN KEY (last_changed_by)
                                                   REFERENCES app_user(id),

                                           CONSTRAINT uq_odontogram_snapshot_tooth
                                               UNIQUE (
                                                       snapshot_id,
                                                       tooth_number
                                                   ),

                                           CONSTRAINT chk_snapshot_tooth_colour
                                               CHECK (
                                                   colour_hex_snapshot IS NULL
                                                       OR colour_hex_snapshot ~ '^#[0-9A-Fa-f]{6}$'
)
    );


CREATE INDEX idx_odontogram_snapshot_tooth_snapshot
    ON odontogram_snapshot_tooth (
                                  snapshot_id,
                                  tooth_number
        );

CREATE INDEX idx_odontogram_snapshot_tooth_condition
    ON odontogram_snapshot_tooth (condition_id);


-- =========================================================
-- 3. CONSENT TEMPLATE
--
-- Clinic-configurable consent documents.
--
-- Examples:
-- Root Canal Consent
-- Extraction Consent
-- Implant Consent
-- General Treatment Consent
--
-- Template content is versioned.
-- =========================================================

CREATE TABLE consent_template (
                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                  clinic_id UUID NOT NULL,

                                  template_code VARCHAR(100) NOT NULL,
                                  template_name VARCHAR(200) NOT NULL,

                                  procedure_id UUID,

                                  description TEXT,

                                  content TEXT NOT NULL,

                                  version INTEGER NOT NULL DEFAULT 1,

                                  is_active BOOLEAN NOT NULL DEFAULT TRUE,

                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  created_by UUID,
                                  updated_by UUID,

                                  CONSTRAINT fk_consent_template_clinic
                                      FOREIGN KEY (clinic_id)
                                          REFERENCES clinic(id),

                                  CONSTRAINT fk_consent_template_procedure
                                      FOREIGN KEY (procedure_id)
                                          REFERENCES procedure_master(id),

                                  CONSTRAINT fk_consent_template_created_by
                                      FOREIGN KEY (created_by)
                                          REFERENCES app_user(id),

                                  CONSTRAINT fk_consent_template_updated_by
                                      FOREIGN KEY (updated_by)
                                          REFERENCES app_user(id),

                                  CONSTRAINT uq_consent_template_version
                                      UNIQUE (
                                              clinic_id,
                                              template_code,
                                              version
                                          ),

                                  CONSTRAINT chk_consent_template_version
                                      CHECK (version > 0)
);


CREATE INDEX idx_consent_template_clinic
    ON consent_template (
                         clinic_id,
                         is_active
        );

CREATE INDEX idx_consent_template_procedure
    ON consent_template (procedure_id);


-- =========================================================
-- 4. PATIENT CONSENT
--
-- Represents an actual consent presented to / signed by
-- a patient.
--
-- IMPORTANT:
-- consent_text_snapshot stores exactly what the patient
-- agreed to.
--
-- Changing consent_template later must never modify
-- historical consent.
-- =========================================================

CREATE TABLE patient_consent (
                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                 clinic_id UUID NOT NULL,
                                 patient_id UUID NOT NULL,

                                 consent_template_id UUID,

                                 appointment_id UUID,
                                 consultation_id UUID,
                                 case_sheet_id UUID,

                                 treatment_plan_id UUID,
                                 treatment_plan_item_id UUID,

                                 procedure_id UUID,

                                 tooth_number VARCHAR(10),

                                 consent_number VARCHAR(50) NOT NULL,

                                 consent_title_snapshot VARCHAR(200) NOT NULL,
                                 consent_text_snapshot TEXT NOT NULL,
                                 template_version_snapshot INTEGER,

                                 status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

                                 presented_at TIMESTAMP,

                                 signed_at TIMESTAMP,
                                 signed_by_name VARCHAR(200),

                                 relationship_to_patient VARCHAR(100),

                                 signature_url TEXT,
                                 signed_document_url TEXT,

                                 declined_at TIMESTAMP,
                                 decline_reason TEXT,

                                 revoked_at TIMESTAMP,
                                 revocation_reason TEXT,

                                 witnessed_by UUID,

                                 notes TEXT,

                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                 created_by UUID,
                                 updated_by UUID,

                                 CONSTRAINT fk_patient_consent_clinic
                                     FOREIGN KEY (clinic_id)
                                         REFERENCES clinic(id),

                                 CONSTRAINT fk_patient_consent_patient
                                     FOREIGN KEY (patient_id)
                                         REFERENCES patient(id),

                                 CONSTRAINT fk_patient_consent_template
                                     FOREIGN KEY (consent_template_id)
                                         REFERENCES consent_template(id),

                                 CONSTRAINT fk_patient_consent_appointment
                                     FOREIGN KEY (appointment_id)
                                         REFERENCES appointment(id),

                                 CONSTRAINT fk_patient_consent_consultation
                                     FOREIGN KEY (consultation_id)
                                         REFERENCES consultation(id),

                                 CONSTRAINT fk_patient_consent_case_sheet
                                     FOREIGN KEY (case_sheet_id)
                                         REFERENCES case_sheet(id),

                                 CONSTRAINT fk_patient_consent_treatment_plan
                                     FOREIGN KEY (treatment_plan_id)
                                         REFERENCES treatment_plan(id),

                                 CONSTRAINT fk_patient_consent_treatment_item
                                     FOREIGN KEY (treatment_plan_item_id)
                                         REFERENCES treatment_plan_item(id),

                                 CONSTRAINT fk_patient_consent_procedure
                                     FOREIGN KEY (procedure_id)
                                         REFERENCES procedure_master(id),

                                 CONSTRAINT fk_patient_consent_witness
                                     FOREIGN KEY (witnessed_by)
                                         REFERENCES app_user(id),

                                 CONSTRAINT fk_patient_consent_created_by
                                     FOREIGN KEY (created_by)
                                         REFERENCES app_user(id),

                                 CONSTRAINT fk_patient_consent_updated_by
                                     FOREIGN KEY (updated_by)
                                         REFERENCES app_user(id),

                                 CONSTRAINT uq_patient_consent_number
                                     UNIQUE (
                                             clinic_id,
                                             consent_number
                                         ),

                                 CONSTRAINT chk_patient_consent_status
                                     CHECK (
                                         status IN (
                                                    'PENDING',
                                                    'PRESENTED',
                                                    'SIGNED',
                                                    'DECLINED',
                                                    'REVOKED'
                                             )
                                         ),

                                 CONSTRAINT chk_patient_consent_template_version
                                     CHECK (
                                         template_version_snapshot IS NULL
                                             OR template_version_snapshot > 0
                                         )
);


CREATE INDEX idx_patient_consent_patient
    ON patient_consent (
                        patient_id,
                        created_at DESC
        );

CREATE INDEX idx_patient_consent_case_sheet
    ON patient_consent (case_sheet_id);

CREATE INDEX idx_patient_consent_appointment
    ON patient_consent (appointment_id);

CREATE INDEX idx_patient_consent_treatment_plan
    ON patient_consent (treatment_plan_id);

CREATE INDEX idx_patient_consent_treatment_item
    ON patient_consent (treatment_plan_item_id);

CREATE INDEX idx_patient_consent_status
    ON patient_consent (
                        clinic_id,
                        status
        );


-- =========================================================
-- 5. SNAPSHOT -> CONSENT LINK
--
-- Allows the finalized odontogram/case snapshot to reference
-- the consent(s) associated with that clinical state.
--
-- One snapshot may have multiple consent forms.
-- One consent can also be associated with the same snapshot.
-- =========================================================

CREATE TABLE odontogram_snapshot_consent (
                                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                             clinic_id UUID NOT NULL,

                                             snapshot_id UUID NOT NULL,
                                             patient_consent_id UUID NOT NULL,

                                             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                             created_by UUID,

                                             CONSTRAINT fk_snapshot_consent_clinic
                                                 FOREIGN KEY (clinic_id)
                                                     REFERENCES clinic(id),

                                             CONSTRAINT fk_snapshot_consent_snapshot
                                                 FOREIGN KEY (snapshot_id)
                                                     REFERENCES odontogram_snapshot(id)
                                                     ON DELETE CASCADE,

                                             CONSTRAINT fk_snapshot_consent_patient_consent
                                                 FOREIGN KEY (patient_consent_id)
                                                     REFERENCES patient_consent(id),

                                             CONSTRAINT fk_snapshot_consent_created_by
                                                 FOREIGN KEY (created_by)
                                                     REFERENCES app_user(id),

                                             CONSTRAINT uq_snapshot_patient_consent
                                                 UNIQUE (
                                                         snapshot_id,
                                                         patient_consent_id
                                                     )
);


CREATE INDEX idx_snapshot_consent_snapshot
    ON odontogram_snapshot_consent (snapshot_id);

CREATE INDEX idx_snapshot_consent_patient_consent
    ON odontogram_snapshot_consent (patient_consent_id);
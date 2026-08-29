-- =========================================================
-- DENTAL CLINIC APP
-- V6 - ODONTOGRAM
-- =========================================================


-- =========================================================
-- 1. ODONTOGRAM
--
-- One primary odontogram per patient.
-- Represents the patient's longitudinal dental chart.
-- =========================================================

CREATE TABLE odontogram (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                            clinic_id UUID NOT NULL,
                            patient_id UUID NOT NULL,

                            status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                            created_by UUID,
                            updated_by UUID,

                            CONSTRAINT fk_odontogram_clinic
                                FOREIGN KEY (clinic_id)
                                    REFERENCES clinic(id),

                            CONSTRAINT fk_odontogram_patient
                                FOREIGN KEY (patient_id)
                                    REFERENCES patient(id),

                            CONSTRAINT fk_odontogram_created_by
                                FOREIGN KEY (created_by)
                                    REFERENCES app_user(id),

                            CONSTRAINT fk_odontogram_updated_by
                                FOREIGN KEY (updated_by)
                                    REFERENCES app_user(id),

                            CONSTRAINT uq_odontogram_patient
                                UNIQUE (patient_id),

                            CONSTRAINT chk_odontogram_status
                                CHECK (status IN (
                                                  'ACTIVE',
                                                  'ARCHIVED'
                                    ))
);


CREATE INDEX idx_odontogram_clinic
    ON odontogram (clinic_id);

CREATE INDEX idx_odontogram_patient
    ON odontogram (patient_id);


-- =========================================================
-- 2. ODONTOGRAM CONDITION
--
-- Clinical meaning of a tooth state.
--
-- IMPORTANT:
-- Colour is intentionally NOT stored here.
--
-- Examples:
-- HEALTHY
-- CARIES
-- MISSING
-- FRACTURED
-- RCT_PLANNED
-- RCT_COMPLETED
-- CROWN
-- IMPLANT
-- =========================================================

CREATE TABLE odontogram_condition (
                                      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                      clinic_id UUID NOT NULL,

                                      condition_code VARCHAR(100) NOT NULL,
                                      condition_name VARCHAR(150) NOT NULL,

                                      description TEXT,

                                      patient_explanation TEXT,

                                      is_active BOOLEAN NOT NULL DEFAULT TRUE,

                                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                      created_by UUID,
                                      updated_by UUID,

                                      CONSTRAINT fk_odontogram_condition_clinic
                                          FOREIGN KEY (clinic_id)
                                              REFERENCES clinic(id),

                                      CONSTRAINT fk_odontogram_condition_created_by
                                          FOREIGN KEY (created_by)
                                              REFERENCES app_user(id),

                                      CONSTRAINT fk_odontogram_condition_updated_by
                                          FOREIGN KEY (updated_by)
                                              REFERENCES app_user(id),

                                      CONSTRAINT uq_odontogram_condition_code
                                          UNIQUE (clinic_id, condition_code)
);


CREATE INDEX idx_odontogram_condition_clinic
    ON odontogram_condition (clinic_id);

CREATE INDEX idx_odontogram_condition_active
    ON odontogram_condition (clinic_id, is_active);


-- =========================================================
-- 3. ODONTOGRAM COLOUR CONFIG
--
-- Presentation configuration for each clinical condition.
-- A clinic can decide which colour represents a condition.
-- =========================================================

CREATE TABLE odontogram_colour_config (
                                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                          clinic_id UUID NOT NULL,
                                          condition_id UUID NOT NULL,

                                          colour_hex VARCHAR(20) NOT NULL,

                                          display_order INTEGER NOT NULL DEFAULT 0,

                                          is_active BOOLEAN NOT NULL DEFAULT TRUE,

                                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                          created_by UUID,
                                          updated_by UUID,

                                          CONSTRAINT fk_odontogram_colour_clinic
                                              FOREIGN KEY (clinic_id)
                                                  REFERENCES clinic(id),

                                          CONSTRAINT fk_odontogram_colour_condition
                                              FOREIGN KEY (condition_id)
                                                  REFERENCES odontogram_condition(id)
                                                  ON DELETE CASCADE,

                                          CONSTRAINT fk_odontogram_colour_created_by
                                              FOREIGN KEY (created_by)
                                                  REFERENCES app_user(id),

                                          CONSTRAINT fk_odontogram_colour_updated_by
                                              FOREIGN KEY (updated_by)
                                                  REFERENCES app_user(id),

                                          CONSTRAINT uq_odontogram_colour_condition
                                              UNIQUE (condition_id),

                                          CONSTRAINT chk_odontogram_colour_display_order
                                              CHECK (display_order >= 0),

                                          CONSTRAINT chk_odontogram_colour_hex
                                              CHECK (
                                                  colour_hex ~ '^#[0-9A-Fa-f]{6}$'
)
    );


CREATE INDEX idx_odontogram_colour_clinic
    ON odontogram_colour_config (clinic_id);


-- =========================================================
-- 4. ODONTOGRAM TOOTH
--
-- CURRENT STATE of each tooth.
--
-- This table is optimized for quickly loading the current
-- odontogram.
--
-- History is stored separately.
-- =========================================================

CREATE TABLE odontogram_tooth (
                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                  clinic_id UUID NOT NULL,
                                  odontogram_id UUID NOT NULL,

                                  tooth_number VARCHAR(10) NOT NULL,

                                  condition_id UUID,

                                  notes TEXT,

                                  last_case_sheet_id UUID,
                                  last_appointment_id UUID,

                                  last_changed_at TIMESTAMP,
                                  last_changed_by UUID,

                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  CONSTRAINT fk_odontogram_tooth_clinic
                                      FOREIGN KEY (clinic_id)
                                          REFERENCES clinic(id),

                                  CONSTRAINT fk_odontogram_tooth_odontogram
                                      FOREIGN KEY (odontogram_id)
                                          REFERENCES odontogram(id)
                                          ON DELETE CASCADE,

                                  CONSTRAINT fk_odontogram_tooth_condition
                                      FOREIGN KEY (condition_id)
                                          REFERENCES odontogram_condition(id),

                                  CONSTRAINT fk_odontogram_tooth_case_sheet
                                      FOREIGN KEY (last_case_sheet_id)
                                          REFERENCES case_sheet(id),

                                  CONSTRAINT fk_odontogram_tooth_appointment
                                      FOREIGN KEY (last_appointment_id)
                                          REFERENCES appointment(id),

                                  CONSTRAINT fk_odontogram_tooth_changed_by
                                      FOREIGN KEY (last_changed_by)
                                          REFERENCES app_user(id),

                                  CONSTRAINT uq_odontogram_tooth
                                      UNIQUE (odontogram_id, tooth_number)
);


CREATE INDEX idx_odontogram_tooth_odontogram
    ON odontogram_tooth (odontogram_id);

CREATE INDEX idx_odontogram_tooth_condition
    ON odontogram_tooth (condition_id);


-- =========================================================
-- 5. ODONTOGRAM TOOTH HISTORY
--
-- IMMUTABLE HISTORY of tooth changes.
--
-- Never update historical records.
-- Every meaningful tooth change creates a new row.
-- =========================================================

CREATE TABLE odontogram_tooth_history (
                                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                          clinic_id UUID NOT NULL,
                                          odontogram_id UUID NOT NULL,
                                          odontogram_tooth_id UUID NOT NULL,

                                          tooth_number VARCHAR(10) NOT NULL,

                                          previous_condition_id UUID,
                                          new_condition_id UUID,

                                          previous_notes TEXT,
                                          new_notes TEXT,

                                          case_sheet_id UUID,
                                          appointment_id UUID,
                                          consultation_id UUID,

                                          change_type VARCHAR(30) NOT NULL DEFAULT 'UPDATED',

                                          change_reason TEXT,

                                          changed_by UUID NOT NULL,
                                          changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                          CONSTRAINT fk_odontogram_history_clinic
                                              FOREIGN KEY (clinic_id)
                                                  REFERENCES clinic(id),

                                          CONSTRAINT fk_odontogram_history_odontogram
                                              FOREIGN KEY (odontogram_id)
                                                  REFERENCES odontogram(id)
                                                  ON DELETE CASCADE,

                                          CONSTRAINT fk_odontogram_history_tooth
                                              FOREIGN KEY (odontogram_tooth_id)
                                                  REFERENCES odontogram_tooth(id)
                                                  ON DELETE CASCADE,

                                          CONSTRAINT fk_odontogram_history_previous_condition
                                              FOREIGN KEY (previous_condition_id)
                                                  REFERENCES odontogram_condition(id),

                                          CONSTRAINT fk_odontogram_history_new_condition
                                              FOREIGN KEY (new_condition_id)
                                                  REFERENCES odontogram_condition(id),

                                          CONSTRAINT fk_odontogram_history_case_sheet
                                              FOREIGN KEY (case_sheet_id)
                                                  REFERENCES case_sheet(id),

                                          CONSTRAINT fk_odontogram_history_appointment
                                              FOREIGN KEY (appointment_id)
                                                  REFERENCES appointment(id),

                                          CONSTRAINT fk_odontogram_history_consultation
                                              FOREIGN KEY (consultation_id)
                                                  REFERENCES consultation(id),

                                          CONSTRAINT fk_odontogram_history_changed_by
                                              FOREIGN KEY (changed_by)
                                                  REFERENCES app_user(id),

                                          CONSTRAINT chk_odontogram_history_change_type
                                              CHECK (change_type IN (
                                                                     'CREATED',
                                                                     'UPDATED',
                                                                     'CLEARED'
                                                  ))
);


CREATE INDEX idx_odontogram_history_odontogram
    ON odontogram_tooth_history (
                                 odontogram_id,
                                 changed_at DESC
        );

CREATE INDEX idx_odontogram_history_tooth
    ON odontogram_tooth_history (
                                 odontogram_id,
                                 tooth_number,
                                 changed_at DESC
        );

CREATE INDEX idx_odontogram_history_case_sheet
    ON odontogram_tooth_history (case_sheet_id);

CREATE INDEX idx_odontogram_history_appointment
    ON odontogram_tooth_history (appointment_id);


-- =========================================================
-- 6. ODONTOGRAM ATTACHMENT
--
-- Media can be associated with the whole chart or a
-- particular tooth.
--
-- Examples:
-- X-Ray
-- Intraoral photograph
-- CBCT
-- Before/After image
-- =========================================================

CREATE TABLE odontogram_attachment (
                                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                       clinic_id UUID NOT NULL,

                                       odontogram_id UUID NOT NULL,
                                       odontogram_tooth_id UUID,

                                       patient_id UUID NOT NULL,
                                       case_sheet_id UUID,
                                       appointment_id UUID,

                                       attachment_type VARCHAR(50) NOT NULL,

                                       file_name VARCHAR(255) NOT NULL,
                                       file_url TEXT NOT NULL,

                                       mime_type VARCHAR(150),
                                       file_size BIGINT,

                                       description TEXT,

                                       uploaded_by UUID,

                                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                       CONSTRAINT fk_odontogram_attachment_clinic
                                           FOREIGN KEY (clinic_id)
                                               REFERENCES clinic(id),

                                       CONSTRAINT fk_odontogram_attachment_odontogram
                                           FOREIGN KEY (odontogram_id)
                                               REFERENCES odontogram(id)
                                               ON DELETE CASCADE,

                                       CONSTRAINT fk_odontogram_attachment_tooth
                                           FOREIGN KEY (odontogram_tooth_id)
                                               REFERENCES odontogram_tooth(id)
                                               ON DELETE CASCADE,

                                       CONSTRAINT fk_odontogram_attachment_patient
                                           FOREIGN KEY (patient_id)
                                               REFERENCES patient(id),

                                       CONSTRAINT fk_odontogram_attachment_case_sheet
                                           FOREIGN KEY (case_sheet_id)
                                               REFERENCES case_sheet(id),

                                       CONSTRAINT fk_odontogram_attachment_appointment
                                           FOREIGN KEY (appointment_id)
                                               REFERENCES appointment(id),

                                       CONSTRAINT fk_odontogram_attachment_uploaded_by
                                           FOREIGN KEY (uploaded_by)
                                               REFERENCES app_user(id),

                                       CONSTRAINT chk_odontogram_attachment_type
                                           CHECK (attachment_type IN (
                                                                      'XRAY',
                                                                      'CBCT',
                                                                      'CLINICAL_IMAGE',
                                                                      'INTRAORAL_IMAGE',
                                                                      'BEFORE_IMAGE',
                                                                      'AFTER_IMAGE',
                                                                      'OTHER'
                                               )),

                                       CONSTRAINT chk_odontogram_attachment_size
                                           CHECK (
                                               file_size IS NULL
                                                   OR file_size >= 0
                                               )
);


CREATE INDEX idx_odontogram_attachment_odontogram
    ON odontogram_attachment (odontogram_id);

CREATE INDEX idx_odontogram_attachment_tooth
    ON odontogram_attachment (odontogram_tooth_id);

CREATE INDEX idx_odontogram_attachment_patient
    ON odontogram_attachment (patient_id);

CREATE INDEX idx_odontogram_attachment_case_sheet
    ON odontogram_attachment (case_sheet_id);
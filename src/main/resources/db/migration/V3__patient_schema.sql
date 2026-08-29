-- =========================================================
-- DENTAL CLINIC APP
-- V3 - PATIENT MANAGEMENT SCHEMA
-- =========================================================


-- =========================================================
-- 1. PATIENT
-- =========================================================

CREATE TABLE patient (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                         clinic_id UUID NOT NULL,

                         patient_number VARCHAR(50) NOT NULL,

                         first_name VARCHAR(100) NOT NULL,
                         last_name VARCHAR(100),

                         gender VARCHAR(20) NOT NULL,
                         dob DATE,

                         blood_group VARCHAR(10),
                         marital_status VARCHAR(30),
                         occupation VARCHAR(150),
                         nationality VARCHAR(100),

                         phone VARCHAR(20) NOT NULL,
                         email VARCHAR(150),

                         aadhaar_number VARCHAR(20),

                         emergency_contact_name VARCHAR(150),
                         emergency_contact_phone VARCHAR(20),

                         reason_for_visit VARCHAR(255),

                         address_line1 VARCHAR(255),
                         address_line2 VARCHAR(255),
                         city VARCHAR(100),
                         state VARCHAR(100),
                         country VARCHAR(100) DEFAULT 'India',
                         pincode VARCHAR(20),

                         is_active BOOLEAN NOT NULL DEFAULT TRUE,

                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                         created_by UUID,
                         updated_by UUID,

                         CONSTRAINT fk_patient_clinic
                             FOREIGN KEY (clinic_id)
                                 REFERENCES clinic(id),

                         CONSTRAINT fk_patient_created_by
                             FOREIGN KEY (created_by)
                                 REFERENCES app_user(id),

                         CONSTRAINT fk_patient_updated_by
                             FOREIGN KEY (updated_by)
                                 REFERENCES app_user(id),

                         CONSTRAINT uq_patient_number
                             UNIQUE (clinic_id, patient_number),

                         CONSTRAINT chk_patient_gender
                             CHECK (gender IN (
                                               'MALE',
                                               'FEMALE',
                                               'OTHER'
                                 )),

                         CONSTRAINT chk_patient_dob
                             CHECK (dob IS NULL OR dob <= CURRENT_DATE)
);


-- Aadhaar should be unique within one clinic when provided
CREATE UNIQUE INDEX uq_patient_clinic_aadhaar
    ON patient (clinic_id, aadhaar_number)
    WHERE aadhaar_number IS NOT NULL;


CREATE INDEX idx_patient_clinic
    ON patient (clinic_id);


CREATE INDEX idx_patient_phone
    ON patient (clinic_id, phone);


CREATE INDEX idx_patient_first_name
    ON patient (clinic_id, LOWER(first_name));


CREATE INDEX idx_patient_last_name
    ON patient (clinic_id, LOWER(last_name));


CREATE INDEX idx_patient_active
    ON patient (clinic_id, is_active);


-- =========================================================
-- 2. PATIENT MEDICAL HISTORY
--
-- One medical-history record per patient for Phase 1.
-- =========================================================

CREATE TABLE patient_medical_history (
                                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                         clinic_id UUID NOT NULL,
                                         patient_id UUID NOT NULL,

                                         diabetes BOOLEAN NOT NULL DEFAULT FALSE,
                                         hypertension BOOLEAN NOT NULL DEFAULT FALSE,
                                         thyroid BOOLEAN NOT NULL DEFAULT FALSE,
                                         heart_disease BOOLEAN NOT NULL DEFAULT FALSE,
                                         kidney_disease BOOLEAN NOT NULL DEFAULT FALSE,
                                         pregnancy BOOLEAN NOT NULL DEFAULT FALSE,
                                         asthma BOOLEAN NOT NULL DEFAULT FALSE,

                                         allergies BOOLEAN NOT NULL DEFAULT FALSE,
                                         tobacco BOOLEAN NOT NULL DEFAULT FALSE,
                                         alcohol BOOLEAN NOT NULL DEFAULT FALSE,
                                         smoking BOOLEAN NOT NULL DEFAULT FALSE,

                                         allergy_notes TEXT,
                                         other_conditions TEXT,

                                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                         created_by UUID,
                                         updated_by UUID,

                                         CONSTRAINT fk_patient_medical_history_clinic
                                             FOREIGN KEY (clinic_id)
                                                 REFERENCES clinic(id),

                                         CONSTRAINT fk_patient_medical_history_patient
                                             FOREIGN KEY (patient_id)
                                                 REFERENCES patient(id)
                                                 ON DELETE CASCADE,

                                         CONSTRAINT fk_patient_medical_history_created_by
                                             FOREIGN KEY (created_by)
                                                 REFERENCES app_user(id),

                                         CONSTRAINT fk_patient_medical_history_updated_by
                                             FOREIGN KEY (updated_by)
                                                 REFERENCES app_user(id),

                                         CONSTRAINT uq_patient_medical_history
                                             UNIQUE (patient_id)
);


CREATE INDEX idx_patient_medical_history_clinic
    ON patient_medical_history (clinic_id);


CREATE INDEX idx_patient_medical_history_patient
    ON patient_medical_history (patient_id);


-- =========================================================
-- 3. PATIENT CUSTOM FIELD DEFINITION
--
-- Allows each clinic to add its own patient-registration
-- fields from Settings.
--
-- Examples:
-- Insurance Number
-- Referral Source
-- Preferred Language
-- =========================================================

CREATE TABLE patient_custom_field_definition (
                                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                                 clinic_id UUID NOT NULL,

                                                 field_key VARCHAR(100) NOT NULL,
                                                 field_label VARCHAR(150) NOT NULL,

                                                 field_type VARCHAR(30) NOT NULL,

                                                 is_required BOOLEAN NOT NULL DEFAULT FALSE,
                                                 display_order INTEGER NOT NULL DEFAULT 0,
                                                 is_active BOOLEAN NOT NULL DEFAULT TRUE,

                                                 options_json JSONB,

                                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                                 created_by UUID,
                                                 updated_by UUID,

                                                 CONSTRAINT fk_patient_custom_field_definition_clinic
                                                     FOREIGN KEY (clinic_id)
                                                         REFERENCES clinic(id),

                                                 CONSTRAINT fk_patient_custom_field_definition_created_by
                                                     FOREIGN KEY (created_by)
                                                         REFERENCES app_user(id),

                                                 CONSTRAINT fk_patient_custom_field_definition_updated_by
                                                     FOREIGN KEY (updated_by)
                                                         REFERENCES app_user(id),

                                                 CONSTRAINT uq_patient_custom_field_key
                                                     UNIQUE (clinic_id, field_key),

                                                 CONSTRAINT chk_patient_custom_field_type
                                                     CHECK (field_type IN (
                                                                           'TEXT',
                                                                           'NUMBER',
                                                                           'DATE',
                                                                           'BOOLEAN',
                                                                           'DROPDOWN',
                                                                           'MULTI_SELECT'
                                                         )),

                                                 CONSTRAINT chk_patient_custom_field_display_order
                                                     CHECK (display_order >= 0)
);


CREATE INDEX idx_patient_custom_field_definition_clinic
    ON patient_custom_field_definition (clinic_id);


CREATE INDEX idx_patient_custom_field_definition_active
    ON patient_custom_field_definition (clinic_id, is_active);


-- =========================================================
-- 4. PATIENT CUSTOM FIELD VALUE
-- =========================================================

CREATE TABLE patient_custom_field_value (
                                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                            clinic_id UUID NOT NULL,
                                            patient_id UUID NOT NULL,
                                            field_definition_id UUID NOT NULL,

                                            value_text TEXT,
                                            value_json JSONB,

                                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                            created_by UUID,
                                            updated_by UUID,

                                            CONSTRAINT fk_patient_custom_field_value_clinic
                                                FOREIGN KEY (clinic_id)
                                                    REFERENCES clinic(id),

                                            CONSTRAINT fk_patient_custom_field_value_patient
                                                FOREIGN KEY (patient_id)
                                                    REFERENCES patient(id)
                                                    ON DELETE CASCADE,

                                            CONSTRAINT fk_patient_custom_field_value_definition
                                                FOREIGN KEY (field_definition_id)
                                                    REFERENCES patient_custom_field_definition(id),

                                            CONSTRAINT fk_patient_custom_field_value_created_by
                                                FOREIGN KEY (created_by)
                                                    REFERENCES app_user(id),

                                            CONSTRAINT fk_patient_custom_field_value_updated_by
                                                FOREIGN KEY (updated_by)
                                                    REFERENCES app_user(id),

                                            CONSTRAINT uq_patient_custom_field_value
                                                UNIQUE (patient_id, field_definition_id),

                                            CONSTRAINT chk_patient_custom_field_has_value
                                                CHECK (
                                                    value_text IS NOT NULL
                                                        OR value_json IS NOT NULL
                                                    )
);


CREATE INDEX idx_patient_custom_field_value_clinic
    ON patient_custom_field_value (clinic_id);


CREATE INDEX idx_patient_custom_field_value_patient
    ON patient_custom_field_value (patient_id);


CREATE INDEX idx_patient_custom_field_value_definition
    ON patient_custom_field_value (field_definition_id);


-- =========================================================
-- 5. PATIENT DOCUMENT
--
-- appointment_id is intentionally not yet a foreign key.
-- The appointment table will be created in Layer 3.
-- We'll add that FK in the appointment migration.
-- =========================================================

CREATE TABLE patient_document (
                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                  clinic_id UUID NOT NULL,
                                  patient_id UUID NOT NULL,

                                  appointment_id UUID,

                                  document_type VARCHAR(50) NOT NULL,

                                  document_name VARCHAR(255),
                                  file_name VARCHAR(255) NOT NULL,

                                  file_url TEXT NOT NULL,

                                  mime_type VARCHAR(150),
                                  file_size BIGINT,

                                  uploaded_by UUID,

                                  is_active BOOLEAN NOT NULL DEFAULT TRUE,

                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  CONSTRAINT fk_patient_document_clinic
                                      FOREIGN KEY (clinic_id)
                                          REFERENCES clinic(id),

                                  CONSTRAINT fk_patient_document_patient
                                      FOREIGN KEY (patient_id)
                                          REFERENCES patient(id)
                                          ON DELETE CASCADE,

                                  CONSTRAINT fk_patient_document_uploaded_by
                                      FOREIGN KEY (uploaded_by)
                                          REFERENCES app_user(id),

                                  CONSTRAINT chk_patient_document_type
                                      CHECK (document_type IN (
                                                               'XRAY',
                                                               'CBCT',
                                                               'CLINICAL_IMAGE',
                                                               'PRESCRIPTION',
                                                               'LAB_REPORT',
                                                               'CONSENT_FORM',
                                                               'TREATMENT_PLAN',
                                                               'CASE_SHEET',
                                                               'INVOICE',
                                                               'RECEIPT',
                                                               'OTHER'
                                          )),

                                  CONSTRAINT chk_patient_document_file_size
                                      CHECK (file_size IS NULL OR file_size >= 0)
);


CREATE INDEX idx_patient_document_clinic
    ON patient_document (clinic_id);


CREATE INDEX idx_patient_document_patient
    ON patient_document (patient_id);


CREATE INDEX idx_patient_document_appointment
    ON patient_document (appointment_id);


CREATE INDEX idx_patient_document_type
    ON patient_document (patient_id, document_type);


-- =========================================================
-- 6. PATIENT ACTIVITY
--
-- User-facing patient timeline.
-- This is different from technical audit_log.
-- =========================================================

CREATE TABLE patient_activity (
                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                  clinic_id UUID NOT NULL,
                                  patient_id UUID NOT NULL,

                                  activity_type VARCHAR(100) NOT NULL,

                                  reference_type VARCHAR(100),
                                  reference_id UUID,

                                  title VARCHAR(255) NOT NULL,
                                  description TEXT,

                                  performed_by UUID,

                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  CONSTRAINT fk_patient_activity_clinic
                                      FOREIGN KEY (clinic_id)
                                          REFERENCES clinic(id),

                                  CONSTRAINT fk_patient_activity_patient
                                      FOREIGN KEY (patient_id)
                                          REFERENCES patient(id)
                                          ON DELETE CASCADE,

                                  CONSTRAINT fk_patient_activity_performed_by
                                      FOREIGN KEY (performed_by)
                                          REFERENCES app_user(id)
);


CREATE INDEX idx_patient_activity_clinic
    ON patient_activity (clinic_id);


CREATE INDEX idx_patient_activity_patient
    ON patient_activity (patient_id);


CREATE INDEX idx_patient_activity_created_at
    ON patient_activity (patient_id, created_at DESC);


CREATE INDEX idx_patient_activity_reference
    ON patient_activity (reference_type, reference_id);
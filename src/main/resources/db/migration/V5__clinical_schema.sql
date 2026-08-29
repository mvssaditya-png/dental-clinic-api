-- =========================================================
-- DENTAL CLINIC APP
-- V5 - CONSULTATION & CLINICAL CASE SHEET
-- =========================================================


-- =========================================================
-- 1. CONSULTATION
-- Actual doctor-patient clinical encounter.
-- Normally one consultation per appointment.
-- =========================================================

CREATE TABLE consultation (
                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                              clinic_id UUID NOT NULL,
                              appointment_id UUID NOT NULL,
                              patient_id UUID NOT NULL,
                              doctor_id UUID NOT NULL,

                              status VARCHAR(30) NOT NULL DEFAULT 'STARTED',

                              started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              ended_at TIMESTAMP,

                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                              created_by UUID,
                              updated_by UUID,

                              CONSTRAINT fk_consultation_clinic
                                  FOREIGN KEY (clinic_id)
                                      REFERENCES clinic(id),

                              CONSTRAINT fk_consultation_appointment
                                  FOREIGN KEY (appointment_id)
                                      REFERENCES appointment(id),

                              CONSTRAINT fk_consultation_patient
                                  FOREIGN KEY (patient_id)
                                      REFERENCES patient(id),

                              CONSTRAINT fk_consultation_doctor
                                  FOREIGN KEY (doctor_id)
                                      REFERENCES doctor_profile(id),

                              CONSTRAINT fk_consultation_created_by
                                  FOREIGN KEY (created_by)
                                      REFERENCES app_user(id),

                              CONSTRAINT fk_consultation_updated_by
                                  FOREIGN KEY (updated_by)
                                      REFERENCES app_user(id),

                              CONSTRAINT uq_consultation_appointment
                                  UNIQUE (appointment_id),

                              CONSTRAINT chk_consultation_status
                                  CHECK (status IN (
                                                    'STARTED',
                                                    'IN_PROGRESS',
                                                    'COMPLETED',
                                                    'CANCELLED'
                                      )),

                              CONSTRAINT chk_consultation_times
                                  CHECK (
                                      ended_at IS NULL
                                          OR ended_at >= started_at
                                      )
);


CREATE INDEX idx_consultation_clinic
    ON consultation (clinic_id);

CREATE INDEX idx_consultation_patient
    ON consultation (patient_id);

CREATE INDEX idx_consultation_doctor
    ON consultation (doctor_id);

CREATE INDEX idx_consultation_status
    ON consultation (clinic_id, status);


-- =========================================================
-- 2. CASE SHEET
-- Main structured clinical record for the consultation.
-- =========================================================

CREATE TABLE case_sheet (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                            clinic_id UUID NOT NULL,

                            consultation_id UUID NOT NULL,
                            appointment_id UUID NOT NULL,
                            patient_id UUID NOT NULL,
                            doctor_id UUID NOT NULL,

                            department_id UUID,

                            case_sheet_number VARCHAR(50) NOT NULL,

                            visit_number INTEGER NOT NULL DEFAULT 1,

                            chief_complaint TEXT,
                            history_of_present_illness TEXT,
                            clinical_examination TEXT,

                            diagnosis_summary TEXT,
                            treatment_notes TEXT,

                            status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',

                            finalized_at TIMESTAMP,
                            finalized_by UUID,

                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                            created_by UUID,
                            updated_by UUID,

                            CONSTRAINT fk_case_sheet_clinic
                                FOREIGN KEY (clinic_id)
                                    REFERENCES clinic(id),

                            CONSTRAINT fk_case_sheet_consultation
                                FOREIGN KEY (consultation_id)
                                    REFERENCES consultation(id),

                            CONSTRAINT fk_case_sheet_appointment
                                FOREIGN KEY (appointment_id)
                                    REFERENCES appointment(id),

                            CONSTRAINT fk_case_sheet_patient
                                FOREIGN KEY (patient_id)
                                    REFERENCES patient(id),

                            CONSTRAINT fk_case_sheet_doctor
                                FOREIGN KEY (doctor_id)
                                    REFERENCES doctor_profile(id),

                            CONSTRAINT fk_case_sheet_department
                                FOREIGN KEY (department_id)
                                    REFERENCES department(id),

                            CONSTRAINT fk_case_sheet_finalized_by
                                FOREIGN KEY (finalized_by)
                                    REFERENCES app_user(id),

                            CONSTRAINT fk_case_sheet_created_by
                                FOREIGN KEY (created_by)
                                    REFERENCES app_user(id),

                            CONSTRAINT fk_case_sheet_updated_by
                                FOREIGN KEY (updated_by)
                                    REFERENCES app_user(id),

                            CONSTRAINT uq_case_sheet_consultation
                                UNIQUE (consultation_id),

                            CONSTRAINT uq_case_sheet_number
                                UNIQUE (clinic_id, case_sheet_number),

                            CONSTRAINT chk_case_sheet_visit_number
                                CHECK (visit_number > 0),

                            CONSTRAINT chk_case_sheet_status
                                CHECK (status IN (
                                                  'DRAFT',
                                                  'FINALIZED',
                                                  'AMENDED'
                                    ))
);


CREATE INDEX idx_case_sheet_clinic
    ON case_sheet (clinic_id);

CREATE INDEX idx_case_sheet_patient
    ON case_sheet (patient_id);

CREATE INDEX idx_case_sheet_appointment
    ON case_sheet (appointment_id);

CREATE INDEX idx_case_sheet_doctor
    ON case_sheet (doctor_id);

CREATE INDEX idx_case_sheet_patient_history
    ON case_sheet (patient_id, created_at DESC);


-- =========================================================
-- 3. CASE SHEET DIAGNOSIS
--
-- Supports multiple diagnoses per consultation and
-- tooth-specific diagnoses.
-- =========================================================

CREATE TABLE case_sheet_diagnosis (
                                      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                      clinic_id UUID NOT NULL,
                                      case_sheet_id UUID NOT NULL,

                                      tooth_number VARCHAR(10),

                                      diagnosis_code VARCHAR(50),
                                      diagnosis_name VARCHAR(255) NOT NULL,

                                      diagnosis_type VARCHAR(30) NOT NULL DEFAULT 'PRIMARY',

                                      notes TEXT,

                                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      created_by UUID,

                                      CONSTRAINT fk_case_sheet_diagnosis_clinic
                                          FOREIGN KEY (clinic_id)
                                              REFERENCES clinic(id),

                                      CONSTRAINT fk_case_sheet_diagnosis_case_sheet
                                          FOREIGN KEY (case_sheet_id)
                                              REFERENCES case_sheet(id)
                                              ON DELETE CASCADE,

                                      CONSTRAINT fk_case_sheet_diagnosis_created_by
                                          FOREIGN KEY (created_by)
                                              REFERENCES app_user(id),

                                      CONSTRAINT chk_case_sheet_diagnosis_type
                                          CHECK (diagnosis_type IN (
                                                                    'PRIMARY',
                                                                    'SECONDARY',
                                                                    'DIFFERENTIAL'
                                              ))
);


CREATE INDEX idx_case_sheet_diagnosis_case_sheet
    ON case_sheet_diagnosis (case_sheet_id);

CREATE INDEX idx_case_sheet_diagnosis_tooth
    ON case_sheet_diagnosis (case_sheet_id, tooth_number);


-- =========================================================
-- 4. CASE SHEET CLINICAL FINDING
--
-- Structured findings such as:
-- swelling, deep caries, sensitivity, mobility etc.
-- =========================================================

CREATE TABLE case_sheet_clinical_finding (
                                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                             clinic_id UUID NOT NULL,
                                             case_sheet_id UUID NOT NULL,

                                             tooth_number VARCHAR(10),

                                             finding_type VARCHAR(100) NOT NULL,
                                             finding_value VARCHAR(255),

                                             notes TEXT,

                                             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                             created_by UUID,

                                             CONSTRAINT fk_case_sheet_finding_clinic
                                                 FOREIGN KEY (clinic_id)
                                                     REFERENCES clinic(id),

                                             CONSTRAINT fk_case_sheet_finding_case_sheet
                                                 FOREIGN KEY (case_sheet_id)
                                                     REFERENCES case_sheet(id)
                                                     ON DELETE CASCADE,

                                             CONSTRAINT fk_case_sheet_finding_created_by
                                                 FOREIGN KEY (created_by)
                                                     REFERENCES app_user(id)
);


CREATE INDEX idx_case_sheet_finding_case_sheet
    ON case_sheet_clinical_finding (case_sheet_id);

CREATE INDEX idx_case_sheet_finding_tooth
    ON case_sheet_clinical_finding (
                                    case_sheet_id,
                                    tooth_number
        );


-- =========================================================
-- 5. CLINICAL INPUT
--
-- Stores original doctor input for traceability.
--
-- Later:
-- Voice -> transcript -> AI -> structured case sheet
-- =========================================================

CREATE TABLE clinical_input (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                clinic_id UUID NOT NULL,

                                consultation_id UUID NOT NULL,
                                case_sheet_id UUID,

                                input_type VARCHAR(30) NOT NULL,

                                raw_text TEXT,
                                transcript_text TEXT,

                                audio_url TEXT,

                                ai_processed BOOLEAN NOT NULL DEFAULT FALSE,

                                ai_output_json JSONB,

                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                created_by UUID,

                                CONSTRAINT fk_clinical_input_clinic
                                    FOREIGN KEY (clinic_id)
                                        REFERENCES clinic(id),

                                CONSTRAINT fk_clinical_input_consultation
                                    FOREIGN KEY (consultation_id)
                                        REFERENCES consultation(id)
                                        ON DELETE CASCADE,

                                CONSTRAINT fk_clinical_input_case_sheet
                                    FOREIGN KEY (case_sheet_id)
                                        REFERENCES case_sheet(id)
                                        ON DELETE CASCADE,

                                CONSTRAINT fk_clinical_input_created_by
                                    FOREIGN KEY (created_by)
                                        REFERENCES app_user(id),

                                CONSTRAINT chk_clinical_input_type
                                    CHECK (input_type IN (
                                                          'FREE_TEXT',
                                                          'VOICE',
                                                          'TEMPLATE',
                                                          'IMAGE'
                                        ))
);


CREATE INDEX idx_clinical_input_consultation
    ON clinical_input (consultation_id);

CREATE INDEX idx_clinical_input_case_sheet
    ON clinical_input (case_sheet_id);


-- =========================================================
-- 6. CASE SHEET ATTACHMENT
--
-- Clinical files associated with this visit.
-- =========================================================

CREATE TABLE case_sheet_attachment (
                                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                       clinic_id UUID NOT NULL,

                                       case_sheet_id UUID NOT NULL,
                                       patient_id UUID NOT NULL,
                                       appointment_id UUID NOT NULL,

                                       attachment_type VARCHAR(50) NOT NULL,

                                       file_name VARCHAR(255) NOT NULL,
                                       file_url TEXT NOT NULL,

                                       mime_type VARCHAR(150),
                                       file_size BIGINT,

                                       description TEXT,

                                       uploaded_by UUID,

                                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                       CONSTRAINT fk_case_sheet_attachment_clinic
                                           FOREIGN KEY (clinic_id)
                                               REFERENCES clinic(id),

                                       CONSTRAINT fk_case_sheet_attachment_case_sheet
                                           FOREIGN KEY (case_sheet_id)
                                               REFERENCES case_sheet(id)
                                               ON DELETE CASCADE,

                                       CONSTRAINT fk_case_sheet_attachment_patient
                                           FOREIGN KEY (patient_id)
                                               REFERENCES patient(id),

                                       CONSTRAINT fk_case_sheet_attachment_appointment
                                           FOREIGN KEY (appointment_id)
                                               REFERENCES appointment(id),

                                       CONSTRAINT fk_case_sheet_attachment_uploaded_by
                                           FOREIGN KEY (uploaded_by)
                                               REFERENCES app_user(id),

                                       CONSTRAINT chk_case_sheet_attachment_type
                                           CHECK (attachment_type IN (
                                                                      'XRAY',
                                                                      'CBCT',
                                                                      'CLINICAL_IMAGE',
                                                                      'INTRAORAL_IMAGE',
                                                                      'LAB_REPORT',
                                                                      'PRESCRIPTION',
                                                                      'OTHER'
                                               )),

                                       CONSTRAINT chk_case_sheet_attachment_size
                                           CHECK (
                                               file_size IS NULL
                                                   OR file_size >= 0
                                               )
);


CREATE INDEX idx_case_sheet_attachment_case_sheet
    ON case_sheet_attachment (case_sheet_id);

CREATE INDEX idx_case_sheet_attachment_patient
    ON case_sheet_attachment (patient_id);


-- =========================================================
-- 7. PRESCRIPTION
-- =========================================================

CREATE TABLE prescription (
                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                              clinic_id UUID NOT NULL,

                              patient_id UUID NOT NULL,
                              appointment_id UUID NOT NULL,
                              case_sheet_id UUID NOT NULL,
                              doctor_id UUID NOT NULL,

                              prescription_number VARCHAR(50) NOT NULL,

                              instructions TEXT,
                              notes TEXT,

                              status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',

                              finalized_at TIMESTAMP,
                              finalized_by UUID,

                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                              created_by UUID,
                              updated_by UUID,

                              CONSTRAINT fk_prescription_clinic
                                  FOREIGN KEY (clinic_id)
                                      REFERENCES clinic(id),

                              CONSTRAINT fk_prescription_patient
                                  FOREIGN KEY (patient_id)
                                      REFERENCES patient(id),

                              CONSTRAINT fk_prescription_appointment
                                  FOREIGN KEY (appointment_id)
                                      REFERENCES appointment(id),

                              CONSTRAINT fk_prescription_case_sheet
                                  FOREIGN KEY (case_sheet_id)
                                      REFERENCES case_sheet(id),

                              CONSTRAINT fk_prescription_doctor
                                  FOREIGN KEY (doctor_id)
                                      REFERENCES doctor_profile(id),

                              CONSTRAINT fk_prescription_finalized_by
                                  FOREIGN KEY (finalized_by)
                                      REFERENCES app_user(id),

                              CONSTRAINT fk_prescription_created_by
                                  FOREIGN KEY (created_by)
                                      REFERENCES app_user(id),

                              CONSTRAINT fk_prescription_updated_by
                                  FOREIGN KEY (updated_by)
                                      REFERENCES app_user(id),

                              CONSTRAINT uq_prescription_number
                                  UNIQUE (clinic_id, prescription_number),

                              CONSTRAINT chk_prescription_status
                                  CHECK (status IN (
                                                    'DRAFT',
                                                    'FINALIZED',
                                                    'CANCELLED'
                                      ))
);


CREATE INDEX idx_prescription_patient
    ON prescription (patient_id);

CREATE INDEX idx_prescription_case_sheet
    ON prescription (case_sheet_id);

CREATE INDEX idx_prescription_appointment
    ON prescription (appointment_id);

CREATE INDEX idx_prescription_doctor
    ON prescription (doctor_id);


-- =========================================================
-- 8. PRESCRIPTION ITEM
-- =========================================================

CREATE TABLE prescription_item (
                                   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                   prescription_id UUID NOT NULL,

                                   medicine_name VARCHAR(255) NOT NULL,

                                   strength VARCHAR(100),
                                   dosage VARCHAR(100),
                                   frequency VARCHAR(100),
                                   duration VARCHAR(100),
                                   route VARCHAR(100),

                                   instructions TEXT,

                                   display_order INTEGER NOT NULL DEFAULT 0,

                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                   CONSTRAINT fk_prescription_item_prescription
                                       FOREIGN KEY (prescription_id)
                                           REFERENCES prescription(id)
                                           ON DELETE CASCADE,

                                   CONSTRAINT chk_prescription_item_display_order
                                       CHECK (display_order >= 0)
);


CREATE INDEX idx_prescription_item_prescription
    ON prescription_item (
                          prescription_id,
                          display_order
        );
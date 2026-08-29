-- =========================================================
-- DENTAL CLINIC APP
-- V7 - PROCEDURE MASTER & TREATMENT PLANNING
-- =========================================================


-- =========================================================
-- 1. PROCEDURE MASTER
--
-- Master catalog of clinical procedures offered by a clinic.
--
-- Examples:
-- Root Canal Treatment
-- Extraction
-- Composite Restoration
-- Crown
-- Scaling
-- Implant
-- =========================================================

CREATE TABLE procedure_master (
                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                  clinic_id UUID NOT NULL,

                                  procedure_code VARCHAR(50) NOT NULL,
                                  procedure_name VARCHAR(200) NOT NULL,

                                  description TEXT,
                                  patient_explanation TEXT,

                                  department_id UUID,

                                  default_duration_minutes INTEGER,
                                  default_estimated_visits INTEGER,

                                  default_notes TEXT,
                                  follow_up_instructions TEXT,

                                  is_active BOOLEAN NOT NULL DEFAULT TRUE,

                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  created_by UUID,
                                  updated_by UUID,

                                  CONSTRAINT fk_procedure_master_clinic
                                      FOREIGN KEY (clinic_id)
                                          REFERENCES clinic(id),

                                  CONSTRAINT fk_procedure_master_department
                                      FOREIGN KEY (department_id)
                                          REFERENCES department(id),

                                  CONSTRAINT fk_procedure_master_created_by
                                      FOREIGN KEY (created_by)
                                          REFERENCES app_user(id),

                                  CONSTRAINT fk_procedure_master_updated_by
                                      FOREIGN KEY (updated_by)
                                          REFERENCES app_user(id),

                                  CONSTRAINT uq_procedure_master_code
                                      UNIQUE (clinic_id, procedure_code),

                                  CONSTRAINT chk_procedure_duration
                                      CHECK (
                                          default_duration_minutes IS NULL
                                              OR default_duration_minutes > 0
                                          ),

                                  CONSTRAINT chk_procedure_visits
                                      CHECK (
                                          default_estimated_visits IS NULL
                                              OR default_estimated_visits > 0
                                          )
);


CREATE INDEX idx_procedure_master_clinic
    ON procedure_master (clinic_id);

CREATE INDEX idx_procedure_master_department
    ON procedure_master (department_id);

CREATE INDEX idx_procedure_master_active
    ON procedure_master (clinic_id, is_active);


-- =========================================================
-- 2. PROCEDURE PRICE
--
-- Keeps price history.
--
-- We don't store only one price in procedure_master because
-- prices can change over time.
-- =========================================================

CREATE TABLE procedure_price (
                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                 clinic_id UUID NOT NULL,
                                 procedure_id UUID NOT NULL,

                                 price NUMERIC(12,2) NOT NULL,

                                 effective_from DATE NOT NULL DEFAULT CURRENT_DATE,
                                 effective_to DATE,

                                 is_active BOOLEAN NOT NULL DEFAULT TRUE,

                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 created_by UUID,

                                 CONSTRAINT fk_procedure_price_clinic
                                     FOREIGN KEY (clinic_id)
                                         REFERENCES clinic(id),

                                 CONSTRAINT fk_procedure_price_procedure
                                     FOREIGN KEY (procedure_id)
                                         REFERENCES procedure_master(id)
                                         ON DELETE CASCADE,

                                 CONSTRAINT fk_procedure_price_created_by
                                     FOREIGN KEY (created_by)
                                         REFERENCES app_user(id),

                                 CONSTRAINT chk_procedure_price
                                     CHECK (price >= 0),

                                 CONSTRAINT chk_procedure_price_dates
                                     CHECK (
                                         effective_to IS NULL
                                             OR effective_to >= effective_from
                                         )
);


CREATE INDEX idx_procedure_price_procedure
    ON procedure_price (
                        procedure_id,
                        effective_from DESC
        );

CREATE INDEX idx_procedure_price_active
    ON procedure_price (
                        clinic_id,
                        procedure_id,
                        is_active
        );


-- =========================================================
-- 3. CONDITION -> PROCEDURE MAPPING
--
-- Connects odontogram conditions with suggested procedures.
--
-- Example:
--
-- CARIES
--   -> COMPOSITE_RESTORATION
--
-- IRREVERSIBLE_PULPITIS
--   -> ROOT_CANAL
--   -> CROWN
--
-- These are suggestions only.
-- Doctor makes the final clinical decision.
-- =========================================================

CREATE TABLE condition_procedure_mapping (
                                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                             clinic_id UUID NOT NULL,

                                             condition_id UUID NOT NULL,
                                             procedure_id UUID NOT NULL,

                                             is_primary BOOLEAN NOT NULL DEFAULT FALSE,

                                             display_order INTEGER NOT NULL DEFAULT 0,

                                             default_notes TEXT,
                                             patient_explanation TEXT,

                                             is_active BOOLEAN NOT NULL DEFAULT TRUE,

                                             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                             updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                             created_by UUID,
                                             updated_by UUID,

                                             CONSTRAINT fk_condition_procedure_clinic
                                                 FOREIGN KEY (clinic_id)
                                                     REFERENCES clinic(id),

                                             CONSTRAINT fk_condition_procedure_condition
                                                 FOREIGN KEY (condition_id)
                                                     REFERENCES odontogram_condition(id)
                                                     ON DELETE CASCADE,

                                             CONSTRAINT fk_condition_procedure_procedure
                                                 FOREIGN KEY (procedure_id)
                                                     REFERENCES procedure_master(id),

                                             CONSTRAINT fk_condition_procedure_created_by
                                                 FOREIGN KEY (created_by)
                                                     REFERENCES app_user(id),

                                             CONSTRAINT fk_condition_procedure_updated_by
                                                 FOREIGN KEY (updated_by)
                                                     REFERENCES app_user(id),

                                             CONSTRAINT uq_condition_procedure
                                                 UNIQUE (condition_id, procedure_id),

                                             CONSTRAINT chk_condition_procedure_display_order
                                                 CHECK (display_order >= 0)
);


CREATE INDEX idx_condition_procedure_condition
    ON condition_procedure_mapping (condition_id);

CREATE INDEX idx_condition_procedure_procedure
    ON condition_procedure_mapping (procedure_id);


-- =========================================================
-- 4. TREATMENT PLAN
--
-- Header representing the treatment proposal presented
-- to the patient.
-- =========================================================

CREATE TABLE treatment_plan (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                clinic_id UUID NOT NULL,

                                patient_id UUID NOT NULL,

                                appointment_id UUID,
                                consultation_id UUID,
                                case_sheet_id UUID,

                                doctor_id UUID NOT NULL,

                                treatment_plan_number VARCHAR(50) NOT NULL,

                                title VARCHAR(255),

                                status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',

                                subtotal NUMERIC(12,2) NOT NULL DEFAULT 0,

                                discount_type VARCHAR(20),
                                discount_value NUMERIC(12,2) NOT NULL DEFAULT 0,

                                discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0,

                                final_amount NUMERIC(12,2) NOT NULL DEFAULT 0,

                                estimated_total_visits INTEGER,

                                notes TEXT,
                                patient_notes TEXT,

                                presented_at TIMESTAMP,
                                approved_at TIMESTAMP,
                                declined_at TIMESTAMP,
                                completed_at TIMESTAMP,
                                cancelled_at TIMESTAMP,

                                approval_notes TEXT,
                                decline_reason TEXT,
                                cancellation_reason TEXT,

                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                created_by UUID,
                                updated_by UUID,

                                CONSTRAINT fk_treatment_plan_clinic
                                    FOREIGN KEY (clinic_id)
                                        REFERENCES clinic(id),

                                CONSTRAINT fk_treatment_plan_patient
                                    FOREIGN KEY (patient_id)
                                        REFERENCES patient(id),

                                CONSTRAINT fk_treatment_plan_appointment
                                    FOREIGN KEY (appointment_id)
                                        REFERENCES appointment(id),

                                CONSTRAINT fk_treatment_plan_consultation
                                    FOREIGN KEY (consultation_id)
                                        REFERENCES consultation(id),

                                CONSTRAINT fk_treatment_plan_case_sheet
                                    FOREIGN KEY (case_sheet_id)
                                        REFERENCES case_sheet(id),

                                CONSTRAINT fk_treatment_plan_doctor
                                    FOREIGN KEY (doctor_id)
                                        REFERENCES doctor_profile(id),

                                CONSTRAINT fk_treatment_plan_created_by
                                    FOREIGN KEY (created_by)
                                        REFERENCES app_user(id),

                                CONSTRAINT fk_treatment_plan_updated_by
                                    FOREIGN KEY (updated_by)
                                        REFERENCES app_user(id),

                                CONSTRAINT uq_treatment_plan_number
                                    UNIQUE (clinic_id, treatment_plan_number),

                                CONSTRAINT chk_treatment_plan_status
                                    CHECK (status IN (
                                                      'DRAFT',
                                                      'PRESENTED',
                                                      'APPROVED',
                                                      'IN_PROGRESS',
                                                      'COMPLETED',
                                                      'DECLINED',
                                                      'CANCELLED'
                                        )),

                                CONSTRAINT chk_treatment_subtotal
                                    CHECK (subtotal >= 0),

                                CONSTRAINT chk_treatment_discount_type
                                    CHECK (
                                        discount_type IS NULL
                                            OR discount_type IN (
                                                                 'PERCENTAGE',
                                                                 'FIXED'
                                            )
                                        ),

                                CONSTRAINT chk_treatment_discount_value
                                    CHECK (discount_value >= 0),

                                CONSTRAINT chk_treatment_discount_amount
                                    CHECK (discount_amount >= 0),

                                CONSTRAINT chk_treatment_final_amount
                                    CHECK (final_amount >= 0),

                                CONSTRAINT chk_treatment_estimated_visits
                                    CHECK (
                                        estimated_total_visits IS NULL
                                            OR estimated_total_visits > 0
                                        )
);


CREATE INDEX idx_treatment_plan_clinic
    ON treatment_plan (clinic_id);

CREATE INDEX idx_treatment_plan_patient
    ON treatment_plan (
                       patient_id,
                       created_at DESC
        );

CREATE INDEX idx_treatment_plan_case_sheet
    ON treatment_plan (case_sheet_id);

CREATE INDEX idx_treatment_plan_appointment
    ON treatment_plan (appointment_id);

CREATE INDEX idx_treatment_plan_doctor
    ON treatment_plan (doctor_id);

CREATE INDEX idx_treatment_plan_status
    ON treatment_plan (clinic_id, status);


-- =========================================================
-- 5. TREATMENT PLAN ITEM
--
-- Individual procedure/tooth within a treatment plan.
--
-- IMPORTANT:
-- Procedure name, price and visits are SNAPSHOTTED here.
-- Future changes to procedure_master/procedure_price must
-- not alter an old patient estimate.
-- =========================================================

CREATE TABLE treatment_plan_item (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                     clinic_id UUID NOT NULL,

                                     treatment_plan_id UUID NOT NULL,

                                     procedure_id UUID NOT NULL,

                                     odontogram_tooth_id UUID,
                                     condition_id UUID,

                                     tooth_number VARCHAR(10),

                                     procedure_code_snapshot VARCHAR(50) NOT NULL,
                                     procedure_name_snapshot VARCHAR(200) NOT NULL,

                                     description TEXT,

                                     quantity INTEGER NOT NULL DEFAULT 1,

                                     unit_price NUMERIC(12,2) NOT NULL DEFAULT 0,

                                     gross_amount NUMERIC(12,2) NOT NULL DEFAULT 0,

                                     discount_type VARCHAR(20),
                                     discount_value NUMERIC(12,2) NOT NULL DEFAULT 0,
                                     discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0,

                                     final_amount NUMERIC(12,2) NOT NULL DEFAULT 0,

                                     estimated_visits INTEGER,

                                     status VARCHAR(30) NOT NULL DEFAULT 'PLANNED',

                                     sequence_number INTEGER NOT NULL DEFAULT 0,

                                     clinical_notes TEXT,
                                     patient_explanation TEXT,
                                     follow_up_instructions TEXT,

                                     started_at TIMESTAMP,
                                     completed_at TIMESTAMP,
                                     cancelled_at TIMESTAMP,

                                     cancellation_reason TEXT,

                                     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                     created_by UUID,
                                     updated_by UUID,

                                     CONSTRAINT fk_treatment_plan_item_clinic
                                         FOREIGN KEY (clinic_id)
                                             REFERENCES clinic(id),

                                     CONSTRAINT fk_treatment_plan_item_plan
                                         FOREIGN KEY (treatment_plan_id)
                                             REFERENCES treatment_plan(id)
                                             ON DELETE CASCADE,

                                     CONSTRAINT fk_treatment_plan_item_procedure
                                         FOREIGN KEY (procedure_id)
                                             REFERENCES procedure_master(id),

                                     CONSTRAINT fk_treatment_plan_item_tooth
                                         FOREIGN KEY (odontogram_tooth_id)
                                             REFERENCES odontogram_tooth(id),

                                     CONSTRAINT fk_treatment_plan_item_condition
                                         FOREIGN KEY (condition_id)
                                             REFERENCES odontogram_condition(id),

                                     CONSTRAINT fk_treatment_plan_item_created_by
                                         FOREIGN KEY (created_by)
                                             REFERENCES app_user(id),

                                     CONSTRAINT fk_treatment_plan_item_updated_by
                                         FOREIGN KEY (updated_by)
                                             REFERENCES app_user(id),

                                     CONSTRAINT chk_treatment_plan_item_quantity
                                         CHECK (quantity > 0),

                                     CONSTRAINT chk_treatment_plan_item_unit_price
                                         CHECK (unit_price >= 0),

                                     CONSTRAINT chk_treatment_plan_item_gross
                                         CHECK (gross_amount >= 0),

                                     CONSTRAINT chk_treatment_plan_item_discount_type
                                         CHECK (
                                             discount_type IS NULL
                                                 OR discount_type IN (
                                                                      'PERCENTAGE',
                                                                      'FIXED'
                                                 )
                                             ),

                                     CONSTRAINT chk_treatment_plan_item_discount_value
                                         CHECK (discount_value >= 0),

                                     CONSTRAINT chk_treatment_plan_item_discount_amount
                                         CHECK (discount_amount >= 0),

                                     CONSTRAINT chk_treatment_plan_item_final_amount
                                         CHECK (final_amount >= 0),

                                     CONSTRAINT chk_treatment_plan_item_visits
                                         CHECK (
                                             estimated_visits IS NULL
                                                 OR estimated_visits > 0
                                             ),

                                     CONSTRAINT chk_treatment_plan_item_status
                                         CHECK (status IN (
                                                           'PLANNED',
                                                           'SCHEDULED',
                                                           'IN_PROGRESS',
                                                           'COMPLETED',
                                                           'CANCELLED'
                                             )),

                                     CONSTRAINT chk_treatment_plan_item_sequence
                                         CHECK (sequence_number >= 0)
);


CREATE INDEX idx_treatment_plan_item_plan
    ON treatment_plan_item (
                            treatment_plan_id,
                            sequence_number
        );

CREATE INDEX idx_treatment_plan_item_procedure
    ON treatment_plan_item (procedure_id);

CREATE INDEX idx_treatment_plan_item_tooth_number
    ON treatment_plan_item (
                            treatment_plan_id,
                            tooth_number
        );

CREATE INDEX idx_treatment_plan_item_status
    ON treatment_plan_item (
                            treatment_plan_id,
                            status
        );


-- =========================================================
-- 6. TREATMENT PLAN STATUS HISTORY
--
-- Append-only lifecycle history.
-- =========================================================

CREATE TABLE treatment_plan_status_history (
                                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                               clinic_id UUID NOT NULL,

                                               treatment_plan_id UUID NOT NULL,

                                               from_status VARCHAR(30),
                                               to_status VARCHAR(30) NOT NULL,

                                               reason TEXT,

                                               changed_by UUID NOT NULL,

                                               changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                               CONSTRAINT fk_treatment_status_clinic
                                                   FOREIGN KEY (clinic_id)
                                                       REFERENCES clinic(id),

                                               CONSTRAINT fk_treatment_status_plan
                                                   FOREIGN KEY (treatment_plan_id)
                                                       REFERENCES treatment_plan(id)
                                                       ON DELETE CASCADE,

                                               CONSTRAINT fk_treatment_status_changed_by
                                                   FOREIGN KEY (changed_by)
                                                       REFERENCES app_user(id),

                                               CONSTRAINT chk_treatment_status_from
                                                   CHECK (
                                                       from_status IS NULL
                                                           OR from_status IN (
                                                                              'DRAFT',
                                                                              'PRESENTED',
                                                                              'APPROVED',
                                                                              'IN_PROGRESS',
                                                                              'COMPLETED',
                                                                              'DECLINED',
                                                                              'CANCELLED'
                                                           )
                                                       ),

                                               CONSTRAINT chk_treatment_status_to
                                                   CHECK (
                                                       to_status IN (
                                                                     'DRAFT',
                                                                     'PRESENTED',
                                                                     'APPROVED',
                                                                     'IN_PROGRESS',
                                                                     'COMPLETED',
                                                                     'DECLINED',
                                                                     'CANCELLED'
                                                           )
                                                       )
);


CREATE INDEX idx_treatment_status_history_plan
    ON treatment_plan_status_history (
                                      treatment_plan_id,
                                      changed_at
        );
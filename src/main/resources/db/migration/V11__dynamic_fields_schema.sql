-- =========================================================
-- DENTAL CLINIC APP
-- V11 - DYNAMIC / CUSTOM FIELDS
-- =========================================================


-- =========================================================
-- 1. CUSTOM FIELD DEFINITION
--
-- Defines a custom field configured by a clinic.
--
-- Examples:
--
-- PATIENT
--   Insurance Provider
--   Referral Source
--   Corporate ID
--
-- APPOINTMENT
--   Referral Campaign
--
-- CASE_SHEET
--   Additional Clinical Observation
-- =========================================================

CREATE TABLE custom_field_definition (
                                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                         clinic_id UUID NOT NULL,

                                         entity_type VARCHAR(30) NOT NULL,

                                         field_code VARCHAR(100) NOT NULL,
                                         field_label VARCHAR(150) NOT NULL,

                                         field_type VARCHAR(30) NOT NULL,

                                         placeholder VARCHAR(255),
                                         help_text TEXT,

                                         default_value TEXT,

                                         is_required BOOLEAN NOT NULL DEFAULT FALSE,
                                         is_active BOOLEAN NOT NULL DEFAULT TRUE,

                                         display_order INTEGER NOT NULL DEFAULT 0,

                                         min_length INTEGER,
                                         max_length INTEGER,

                                         min_value NUMERIC(15,4),
                                         max_value NUMERIC(15,4),

                                         validation_regex TEXT,

                                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                         created_by UUID,
                                         updated_by UUID,

                                         CONSTRAINT fk_custom_field_definition_clinic
                                             FOREIGN KEY (clinic_id)
                                                 REFERENCES clinic(id),

                                         CONSTRAINT fk_custom_field_definition_created_by
                                             FOREIGN KEY (created_by)
                                                 REFERENCES app_user(id),

                                         CONSTRAINT fk_custom_field_definition_updated_by
                                             FOREIGN KEY (updated_by)
                                                 REFERENCES app_user(id),

                                         CONSTRAINT uq_custom_field_definition
                                             UNIQUE (
                                                     clinic_id,
                                                     entity_type,
                                                     field_code
                                                 ),

                                         CONSTRAINT chk_custom_field_entity_type
                                             CHECK (
                                                 entity_type IN (
                                                                 'PATIENT',
                                                                 'APPOINTMENT',
                                                                 'CASE_SHEET'
                                                     )
                                                 ),

                                         CONSTRAINT chk_custom_field_type
                                             CHECK (
                                                 field_type IN (
                                                                'TEXT',
                                                                'TEXTAREA',
                                                                'NUMBER',
                                                                'DATE',
                                                                'DATETIME',
                                                                'BOOLEAN',
                                                                'SELECT',
                                                                'MULTI_SELECT'
                                                     )
                                                 ),

                                         CONSTRAINT chk_custom_field_display_order
                                             CHECK (display_order >= 0),

                                         CONSTRAINT chk_custom_field_min_length
                                             CHECK (
                                                 min_length IS NULL
                                                     OR min_length >= 0
                                                 ),

                                         CONSTRAINT chk_custom_field_max_length
                                             CHECK (
                                                 max_length IS NULL
                                                     OR max_length >= 0
                                                 ),

                                         CONSTRAINT chk_custom_field_length_range
                                             CHECK (
                                                 min_length IS NULL
                                                     OR max_length IS NULL
                                                     OR max_length >= min_length
                                                 ),

                                         CONSTRAINT chk_custom_field_value_range
                                             CHECK (
                                                 min_value IS NULL
                                                     OR max_value IS NULL
                                                     OR max_value >= min_value
                                                 )
);


CREATE INDEX idx_custom_field_definition_clinic
    ON custom_field_definition (
                                clinic_id,
                                entity_type
        );

CREATE INDEX idx_custom_field_definition_active
    ON custom_field_definition (
                                clinic_id,
                                entity_type,
                                is_active,
                                display_order
        );


-- =========================================================
-- 2. CUSTOM FIELD OPTION
--
-- Options for SELECT / MULTI_SELECT fields.
--
-- Example:
--
-- Field:
-- Referral Source
--
-- Options:
-- Google
-- Friend
-- Existing Patient
-- Doctor Referral
-- Walk-in
-- =========================================================

CREATE TABLE custom_field_option (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                     clinic_id UUID NOT NULL,

                                     field_definition_id UUID NOT NULL,

                                     option_value VARCHAR(150) NOT NULL,
                                     option_label VARCHAR(150) NOT NULL,

                                     display_order INTEGER NOT NULL DEFAULT 0,

                                     is_active BOOLEAN NOT NULL DEFAULT TRUE,

                                     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                     created_by UUID,
                                     updated_by UUID,

                                     CONSTRAINT fk_custom_field_option_clinic
                                         FOREIGN KEY (clinic_id)
                                             REFERENCES clinic(id),

                                     CONSTRAINT fk_custom_field_option_definition
                                         FOREIGN KEY (field_definition_id)
                                             REFERENCES custom_field_definition(id)
                                             ON DELETE CASCADE,

                                     CONSTRAINT fk_custom_field_option_created_by
                                         FOREIGN KEY (created_by)
                                             REFERENCES app_user(id),

                                     CONSTRAINT fk_custom_field_option_updated_by
                                         FOREIGN KEY (updated_by)
                                             REFERENCES app_user(id),

                                     CONSTRAINT uq_custom_field_option
                                         UNIQUE (
                                                 field_definition_id,
                                                 option_value
                                             ),

                                     CONSTRAINT chk_custom_field_option_display_order
                                         CHECK (display_order >= 0)
);


CREATE INDEX idx_custom_field_option_definition
    ON custom_field_option (
                            field_definition_id,
                            is_active,
                            display_order
        );


-- =========================================================
-- 3. CUSTOM FIELD VALUE
--
-- Stores the actual value entered against an entity.
--
-- We intentionally keep one generic entity_id rather than
-- separate patient_id / appointment_id / case_sheet_id
-- columns.
--
-- Application validates entity_id according to entity_type.
-- =========================================================

CREATE TABLE custom_field_value (
                                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                    clinic_id UUID NOT NULL,

                                    field_definition_id UUID NOT NULL,

                                    entity_type VARCHAR(30) NOT NULL,
                                    entity_id UUID NOT NULL,

                                    text_value TEXT,
                                    number_value NUMERIC(15,4),
                                    date_value DATE,
                                    datetime_value TIMESTAMP,
                                    boolean_value BOOLEAN,

                                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                    created_by UUID,
                                    updated_by UUID,

                                    CONSTRAINT fk_custom_field_value_clinic
                                        FOREIGN KEY (clinic_id)
                                            REFERENCES clinic(id),

                                    CONSTRAINT fk_custom_field_value_definition
                                        FOREIGN KEY (field_definition_id)
                                            REFERENCES custom_field_definition(id)
                                            ON DELETE CASCADE,

                                    CONSTRAINT fk_custom_field_value_created_by
                                        FOREIGN KEY (created_by)
                                            REFERENCES app_user(id),

                                    CONSTRAINT fk_custom_field_value_updated_by
                                        FOREIGN KEY (updated_by)
                                            REFERENCES app_user(id),

                                    CONSTRAINT uq_custom_field_value
                                        UNIQUE (
                                                field_definition_id,
                                                entity_id
                                            ),

                                    CONSTRAINT chk_custom_field_value_entity_type
                                        CHECK (
                                            entity_type IN (
                                                            'PATIENT',
                                                            'APPOINTMENT',
                                                            'CASE_SHEET'
                                                )
                                            )
);


CREATE INDEX idx_custom_field_value_entity
    ON custom_field_value (
                           clinic_id,
                           entity_type,
                           entity_id
        );

CREATE INDEX idx_custom_field_value_definition
    ON custom_field_value (
                           field_definition_id
        );


-- =========================================================
-- 4. CUSTOM FIELD SELECTED OPTION
--
-- Used primarily for MULTI_SELECT.
--
-- We also allow SELECT to use this table so option values
-- are referenced rather than duplicated as plain text.
-- =========================================================

CREATE TABLE custom_field_selected_option (
                                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                              custom_field_value_id UUID NOT NULL,
                                              option_id UUID NOT NULL,

                                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                              CONSTRAINT fk_custom_field_selected_value
                                                  FOREIGN KEY (custom_field_value_id)
                                                      REFERENCES custom_field_value(id)
                                                      ON DELETE CASCADE,

                                              CONSTRAINT fk_custom_field_selected_option
                                                  FOREIGN KEY (option_id)
                                                      REFERENCES custom_field_option(id),

                                              CONSTRAINT uq_custom_field_selected_option
                                                  UNIQUE (
                                                          custom_field_value_id,
                                                          option_id
                                                      )
);


CREATE INDEX idx_custom_field_selected_value
    ON custom_field_selected_option (
                                     custom_field_value_id
        );

CREATE INDEX idx_custom_field_selected_option
    ON custom_field_selected_option (
                                     option_id
        );
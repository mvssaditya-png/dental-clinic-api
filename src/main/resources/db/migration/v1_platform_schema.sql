-- =========================================================
-- DENTAL CLINIC APP
-- V1 - PLATFORM / MULTI-TENANT FOUNDATION
-- =========================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;


-- =========================================================
-- 1. CLINIC
-- =========================================================

CREATE TABLE clinic (
                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                        clinic_code VARCHAR(50) NOT NULL UNIQUE,
                        clinic_name VARCHAR(150) NOT NULL,

                        phone VARCHAR(20),
                        email VARCHAR(150),

                        address_line1 VARCHAR(255),
                        address_line2 VARCHAR(255),
                        city VARCHAR(100),
                        state VARCHAR(100),
                        country VARCHAR(100) DEFAULT 'India',
                        pincode VARCHAR(20),

                        timezone VARCHAR(100) DEFAULT 'Asia/Kolkata',

                        logo_url TEXT,

                        is_active BOOLEAN NOT NULL DEFAULT TRUE,

                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- =========================================================
-- 2. APP USER
--
-- clinic_id is nullable because SUPER_ADMIN belongs to
-- the platform, not necessarily to one clinic.
-- =========================================================

CREATE TABLE app_user (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                          clinic_id UUID,

                          first_name VARCHAR(100) NOT NULL,
                          last_name VARCHAR(100),

                          phone VARCHAR(20),
                          email VARCHAR(150),

                          password_hash VARCHAR(255),

                          status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

                          last_login_at TIMESTAMP,

                          is_phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
                          is_email_verified BOOLEAN NOT NULL DEFAULT FALSE,

                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          created_by UUID,
                          updated_by UUID,

                          CONSTRAINT fk_app_user_clinic
                              FOREIGN KEY (clinic_id)
                                  REFERENCES clinic(id),

                          CONSTRAINT chk_app_user_status
                              CHECK (status IN (
                                                'ACTIVE',
                                                'INACTIVE',
                                                'BLOCKED'
                                  ))
);


-- Prevent duplicate phone number within same clinic
CREATE UNIQUE INDEX uq_app_user_clinic_phone
    ON app_user (clinic_id, phone)
    WHERE phone IS NOT NULL;


-- Prevent duplicate email within same clinic
CREATE UNIQUE INDEX uq_app_user_clinic_email
    ON app_user (clinic_id, LOWER(email))
    WHERE email IS NOT NULL;


CREATE INDEX idx_app_user_clinic
    ON app_user (clinic_id);


CREATE INDEX idx_app_user_phone
    ON app_user (phone);


CREATE INDEX idx_app_user_email
    ON app_user (LOWER(email));


-- =========================================================
-- 3. ROLE
--
-- System roles:
-- SUPER_ADMIN
-- ADMIN
-- RECEPTIONIST
-- DOCTOR
-- ATTENDER
--
-- Later clinic-specific roles can also be created.
-- =========================================================

CREATE TABLE role (
                      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                      clinic_id UUID,

                      role_code VARCHAR(50) NOT NULL,
                      role_name VARCHAR(100) NOT NULL,

                      description VARCHAR(255),

                      is_system_role BOOLEAN NOT NULL DEFAULT FALSE,
                      is_active BOOLEAN NOT NULL DEFAULT TRUE,

                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                      CONSTRAINT fk_role_clinic
                          FOREIGN KEY (clinic_id)
                              REFERENCES clinic(id)
);


-- Global/system roles cannot duplicate their code
CREATE UNIQUE INDEX uq_global_role_code
    ON role (role_code)
    WHERE clinic_id IS NULL;


-- Clinic-specific roles cannot duplicate within a clinic
CREATE UNIQUE INDEX uq_clinic_role_code
    ON role (clinic_id, role_code)
    WHERE clinic_id IS NOT NULL;


CREATE INDEX idx_role_clinic
    ON role (clinic_id);


-- =========================================================
-- 4. PERMISSION
--
-- Permissions are global capabilities.
--
-- Example:
-- PATIENT_VIEW
-- PATIENT_CREATE
-- APPOINTMENT_CREATE
-- FINANCE_VIEW_PROFIT
-- etc.
-- =========================================================

CREATE TABLE permission (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                            permission_code VARCHAR(100) NOT NULL UNIQUE,
                            permission_name VARCHAR(150) NOT NULL,

                            module VARCHAR(100) NOT NULL,

                            description VARCHAR(255),

                            is_active BOOLEAN NOT NULL DEFAULT TRUE,

                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE INDEX idx_permission_module
    ON permission (module);


-- =========================================================
-- 5. USER ROLE
--
-- A user can have multiple roles if required.
-- =========================================================

CREATE TABLE user_role (
                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                           user_id UUID NOT NULL,
                           role_id UUID NOT NULL,

                           assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           assigned_by UUID,

                           CONSTRAINT fk_user_role_user
                               FOREIGN KEY (user_id)
                                   REFERENCES app_user(id)
                                   ON DELETE CASCADE,

                           CONSTRAINT fk_user_role_role
                               FOREIGN KEY (role_id)
                                   REFERENCES role(id)
                                   ON DELETE CASCADE,

                           CONSTRAINT uq_user_role
                               UNIQUE (user_id, role_id)
);


CREATE INDEX idx_user_role_user
    ON user_role (user_id);


CREATE INDEX idx_user_role_role
    ON user_role (role_id);


-- =========================================================
-- 6. ROLE PERMISSION
-- =========================================================

CREATE TABLE role_permission (
                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                 role_id UUID NOT NULL,
                                 permission_id UUID NOT NULL,

                                 granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 granted_by UUID,

                                 CONSTRAINT fk_role_permission_role
                                     FOREIGN KEY (role_id)
                                         REFERENCES role(id)
                                         ON DELETE CASCADE,

                                 CONSTRAINT fk_role_permission_permission
                                     FOREIGN KEY (permission_id)
                                         REFERENCES permission(id)
                                         ON DELETE CASCADE,

                                 CONSTRAINT uq_role_permission
                                     UNIQUE (role_id, permission_id)
);


CREATE INDEX idx_role_permission_role
    ON role_permission (role_id);


CREATE INDEX idx_role_permission_permission
    ON role_permission (permission_id);


-- =========================================================
-- 7. DEPARTMENT
--
-- Examples:
-- General Dentistry
-- Orthodontics
-- Endodontics
-- Periodontics
-- OMFS
-- Prosthodontics
-- =========================================================

CREATE TABLE department (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                            clinic_id UUID NOT NULL,

                            department_code VARCHAR(50) NOT NULL,
                            department_name VARCHAR(150) NOT NULL,

                            description VARCHAR(255),

                            is_active BOOLEAN NOT NULL DEFAULT TRUE,

                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                            created_by UUID,
                            updated_by UUID,

                            CONSTRAINT fk_department_clinic
                                FOREIGN KEY (clinic_id)
                                    REFERENCES clinic(id),

                            CONSTRAINT uq_department_code
                                UNIQUE (clinic_id, department_code),

                            CONSTRAINT uq_department_name
                                UNIQUE (clinic_id, department_name)
);


CREATE INDEX idx_department_clinic
    ON department (clinic_id);


-- =========================================================
-- 8. CLINIC SETTINGS
--
-- One settings record per clinic.
-- Keep commonly-used settings as columns.
-- Flexible future settings can go into settings_json.
-- =========================================================

CREATE TABLE clinic_settings (
                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                 clinic_id UUID NOT NULL UNIQUE,

                                 currency VARCHAR(10) NOT NULL DEFAULT 'INR',

                                 date_format VARCHAR(30) NOT NULL DEFAULT 'DD-MM-YYYY',
                                 time_format VARCHAR(10) NOT NULL DEFAULT '12_HOUR',

                                 appointment_slot_minutes INTEGER NOT NULL DEFAULT 30,

                                 allow_walk_in BOOLEAN NOT NULL DEFAULT TRUE,
                                 enable_whatsapp BOOLEAN NOT NULL DEFAULT FALSE,
                                 enable_sms BOOLEAN NOT NULL DEFAULT FALSE,
                                 enable_email BOOLEAN NOT NULL DEFAULT FALSE,

                                 enable_ai_assistant BOOLEAN NOT NULL DEFAULT FALSE,

                                 settings_json JSONB,

                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                 CONSTRAINT fk_clinic_settings_clinic
                                     FOREIGN KEY (clinic_id)
                                         REFERENCES clinic(id)
                                         ON DELETE CASCADE,

                                 CONSTRAINT chk_appointment_slot_minutes
                                     CHECK (appointment_slot_minutes > 0),

                                 CONSTRAINT chk_time_format
                                     CHECK (time_format IN (
                                                            '12_HOUR',
                                                            '24_HOUR'
                                         ))
);


-- =========================================================
-- 9. AUDIT LOG
--
-- Generic audit table.
--
-- Examples:
-- PATIENT CREATED
-- APPOINTMENT UPDATED
-- TOOTH 46 CHANGED
-- PAYMENT RECORDED
-- AI SUGGESTION ACCEPTED
-- =========================================================

CREATE TABLE audit_log (
                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                           clinic_id UUID,

                           user_id UUID,

                           module VARCHAR(100) NOT NULL,
                           entity_type VARCHAR(100) NOT NULL,
                           entity_id UUID,

                           action VARCHAR(100) NOT NULL,

                           old_value JSONB,
                           new_value JSONB,

                           description TEXT,

                           ip_address VARCHAR(100),
                           user_agent TEXT,

                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT fk_audit_log_clinic
                               FOREIGN KEY (clinic_id)
                                   REFERENCES clinic(id),

                           CONSTRAINT fk_audit_log_user
                               FOREIGN KEY (user_id)
                                   REFERENCES app_user(id)
);


CREATE INDEX idx_audit_log_clinic
    ON audit_log (clinic_id);


CREATE INDEX idx_audit_log_user
    ON audit_log (user_id);


CREATE INDEX idx_audit_log_entity
    ON audit_log (entity_type, entity_id);


CREATE INDEX idx_audit_log_created_at
    ON audit_log (created_at);


-- =========================================================
-- SELF-REFERENCING AUDIT FOREIGN KEYS FOR APP_USER
-- Added after table creation to avoid circular dependency.
-- =========================================================

ALTER TABLE app_user
    ADD CONSTRAINT fk_app_user_created_by
        FOREIGN KEY (created_by)
            REFERENCES app_user(id);


ALTER TABLE app_user
    ADD CONSTRAINT fk_app_user_updated_by
        FOREIGN KEY (updated_by)
            REFERENCES app_user(id);


ALTER TABLE department
    ADD CONSTRAINT fk_department_created_by
        FOREIGN KEY (created_by)
            REFERENCES app_user(id);


ALTER TABLE department
    ADD CONSTRAINT fk_department_updated_by
        FOREIGN KEY (updated_by)
            REFERENCES app_user(id);
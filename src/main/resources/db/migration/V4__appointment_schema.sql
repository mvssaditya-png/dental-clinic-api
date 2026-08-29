-- =========================================================
-- DENTAL CLINIC APP
-- V4 - DOCTOR & APPOINTMENT MANAGEMENT
-- =========================================================


-- =========================================================
-- 1. DOCTOR PROFILE
-- Professional information for users who are doctors.
-- app_user continues to hold login/account information.
-- =========================================================

CREATE TABLE doctor_profile (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                clinic_id UUID NOT NULL,
                                user_id UUID NOT NULL,

                                registration_number VARCHAR(100),
                                qualification VARCHAR(255),
                                specialization VARCHAR(255),
                                designation VARCHAR(100),

                                experience_years INTEGER,

                                consultation_fee NUMERIC(12,2),

                                bio TEXT,

                                is_active BOOLEAN NOT NULL DEFAULT TRUE,

                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                created_by UUID,
                                updated_by UUID,

                                CONSTRAINT fk_doctor_profile_clinic
                                    FOREIGN KEY (clinic_id)
                                        REFERENCES clinic(id),

                                CONSTRAINT fk_doctor_profile_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES app_user(id),

                                CONSTRAINT fk_doctor_profile_created_by
                                    FOREIGN KEY (created_by)
                                        REFERENCES app_user(id),

                                CONSTRAINT fk_doctor_profile_updated_by
                                    FOREIGN KEY (updated_by)
                                        REFERENCES app_user(id),

                                CONSTRAINT uq_doctor_profile_user
                                    UNIQUE (user_id),

                                CONSTRAINT chk_doctor_experience
                                    CHECK (
                                        experience_years IS NULL
                                            OR experience_years >= 0
                                        ),

                                CONSTRAINT chk_doctor_consultation_fee
                                    CHECK (
                                        consultation_fee IS NULL
                                            OR consultation_fee >= 0
                                        )
);


CREATE INDEX idx_doctor_profile_clinic
    ON doctor_profile (clinic_id);


CREATE INDEX idx_doctor_profile_active
    ON doctor_profile (clinic_id, is_active);


-- Registration number should not duplicate within a clinic
CREATE UNIQUE INDEX uq_doctor_registration_number
    ON doctor_profile (clinic_id, registration_number)
    WHERE registration_number IS NOT NULL;


-- =========================================================
-- 2. DOCTOR DEPARTMENT
--
-- Many-to-many relationship.
--
-- One doctor can belong to multiple departments.
-- One department can contain multiple doctors.
-- =========================================================

CREATE TABLE doctor_department (
                                   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                   clinic_id UUID NOT NULL,

                                   doctor_id UUID NOT NULL,
                                   department_id UUID NOT NULL,

                                   is_primary BOOLEAN NOT NULL DEFAULT FALSE,

                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   created_by UUID,

                                   CONSTRAINT fk_doctor_department_clinic
                                       FOREIGN KEY (clinic_id)
                                           REFERENCES clinic(id),

                                   CONSTRAINT fk_doctor_department_doctor
                                       FOREIGN KEY (doctor_id)
                                           REFERENCES doctor_profile(id)
                                           ON DELETE CASCADE,

                                   CONSTRAINT fk_doctor_department_department
                                       FOREIGN KEY (department_id)
                                           REFERENCES department(id),

                                   CONSTRAINT fk_doctor_department_created_by
                                       FOREIGN KEY (created_by)
                                           REFERENCES app_user(id),

                                   CONSTRAINT uq_doctor_department
                                       UNIQUE (doctor_id, department_id)
);


CREATE INDEX idx_doctor_department_doctor
    ON doctor_department (doctor_id);


CREATE INDEX idx_doctor_department_department
    ON doctor_department (department_id);


-- Only one PRIMARY department per doctor.
CREATE UNIQUE INDEX uq_doctor_primary_department
    ON doctor_department (doctor_id)
    WHERE is_primary = TRUE;


-- =========================================================
-- 3. DENTAL CHAIR
-- =========================================================

CREATE TABLE dental_chair (
                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                              clinic_id UUID NOT NULL,

                              chair_code VARCHAR(50) NOT NULL,
                              chair_name VARCHAR(100) NOT NULL,

                              location VARCHAR(255),
                              description VARCHAR(255),

                              is_active BOOLEAN NOT NULL DEFAULT TRUE,

                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                              created_by UUID,
                              updated_by UUID,

                              CONSTRAINT fk_dental_chair_clinic
                                  FOREIGN KEY (clinic_id)
                                      REFERENCES clinic(id),

                              CONSTRAINT fk_dental_chair_created_by
                                  FOREIGN KEY (created_by)
                                      REFERENCES app_user(id),

                              CONSTRAINT fk_dental_chair_updated_by
                                  FOREIGN KEY (updated_by)
                                      REFERENCES app_user(id),

                              CONSTRAINT uq_dental_chair_code
                                  UNIQUE (clinic_id, chair_code),

                              CONSTRAINT uq_dental_chair_name
                                  UNIQUE (clinic_id, chair_name)
);


CREATE INDEX idx_dental_chair_clinic
    ON dental_chair (clinic_id);


CREATE INDEX idx_dental_chair_active
    ON dental_chair (clinic_id, is_active);


-- =========================================================
-- 4. DOCTOR SCHEDULE
--
-- Recurring weekly schedule.
--
-- Example:
-- MONDAY 09:00 - 13:00
-- MONDAY 14:00 - 18:00
--
-- Multiple rows for the same day are allowed.
-- =========================================================

CREATE TABLE doctor_schedule (
                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                 clinic_id UUID NOT NULL,
                                 doctor_id UUID NOT NULL,

                                 day_of_week VARCHAR(20) NOT NULL,

                                 start_time TIME NOT NULL,
                                 end_time TIME NOT NULL,

                                 slot_duration_minutes INTEGER NOT NULL DEFAULT 30,

                                 is_active BOOLEAN NOT NULL DEFAULT TRUE,

                                 effective_from DATE,
                                 effective_to DATE,

                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                 created_by UUID,
                                 updated_by UUID,

                                 CONSTRAINT fk_doctor_schedule_clinic
                                     FOREIGN KEY (clinic_id)
                                         REFERENCES clinic(id),

                                 CONSTRAINT fk_doctor_schedule_doctor
                                     FOREIGN KEY (doctor_id)
                                         REFERENCES doctor_profile(id)
                                         ON DELETE CASCADE,

                                 CONSTRAINT fk_doctor_schedule_created_by
                                     FOREIGN KEY (created_by)
                                         REFERENCES app_user(id),

                                 CONSTRAINT fk_doctor_schedule_updated_by
                                     FOREIGN KEY (updated_by)
                                         REFERENCES app_user(id),

                                 CONSTRAINT chk_doctor_schedule_day
                                     CHECK (day_of_week IN (
                                                            'MONDAY',
                                                            'TUESDAY',
                                                            'WEDNESDAY',
                                                            'THURSDAY',
                                                            'FRIDAY',
                                                            'SATURDAY',
                                                            'SUNDAY'
                                         )),

                                 CONSTRAINT chk_doctor_schedule_time
                                     CHECK (end_time > start_time),

                                 CONSTRAINT chk_doctor_schedule_slot_duration
                                     CHECK (slot_duration_minutes > 0),

                                 CONSTRAINT chk_doctor_schedule_effective_dates
                                     CHECK (
                                         effective_to IS NULL
                                             OR effective_from IS NULL
                                             OR effective_to >= effective_from
                                         )
);


CREATE INDEX idx_doctor_schedule_doctor
    ON doctor_schedule (doctor_id);


CREATE INDEX idx_doctor_schedule_day
    ON doctor_schedule (
                        doctor_id,
                        day_of_week,
                        is_active
        );


-- =========================================================
-- 5. DOCTOR SCHEDULE BREAK
--
-- Example:
-- 13:00 - 14:00 Lunch
-- =========================================================

CREATE TABLE doctor_schedule_break (
                                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                       clinic_id UUID NOT NULL,
                                       doctor_schedule_id UUID NOT NULL,

                                       start_time TIME NOT NULL,
                                       end_time TIME NOT NULL,

                                       description VARCHAR(255),

                                       is_active BOOLEAN NOT NULL DEFAULT TRUE,

                                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                       created_by UUID,
                                       updated_by UUID,

                                       CONSTRAINT fk_doctor_schedule_break_clinic
                                           FOREIGN KEY (clinic_id)
                                               REFERENCES clinic(id),

                                       CONSTRAINT fk_doctor_schedule_break_schedule
                                           FOREIGN KEY (doctor_schedule_id)
                                               REFERENCES doctor_schedule(id)
                                               ON DELETE CASCADE,

                                       CONSTRAINT fk_doctor_schedule_break_created_by
                                           FOREIGN KEY (created_by)
                                               REFERENCES app_user(id),

                                       CONSTRAINT fk_doctor_schedule_break_updated_by
                                           FOREIGN KEY (updated_by)
                                               REFERENCES app_user(id),

                                       CONSTRAINT chk_doctor_schedule_break_time
                                           CHECK (end_time > start_time)
);


CREATE INDEX idx_doctor_schedule_break_schedule
    ON doctor_schedule_break (doctor_schedule_id);


-- =========================================================
-- 6. DOCTOR AVAILABILITY EXCEPTION
--
-- Handles date-specific changes.
--
-- Examples:
--
-- UNAVAILABLE
-- Doctor on leave 28-Aug-2026
--
-- AVAILABLE
-- Doctor normally doesn't work Sunday,
-- but works 09:00-13:00 on a particular Sunday.
-- =========================================================

CREATE TABLE doctor_availability_exception (
                                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                               clinic_id UUID NOT NULL,
                                               doctor_id UUID NOT NULL,

                                               exception_date DATE NOT NULL,

                                               exception_type VARCHAR(30) NOT NULL,

                                               start_time TIME,
                                               end_time TIME,

                                               reason VARCHAR(255),

                                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                               updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                               created_by UUID,
                                               updated_by UUID,

                                               CONSTRAINT fk_doctor_availability_exception_clinic
                                                   FOREIGN KEY (clinic_id)
                                                       REFERENCES clinic(id),

                                               CONSTRAINT fk_doctor_availability_exception_doctor
                                                   FOREIGN KEY (doctor_id)
                                                       REFERENCES doctor_profile(id)
                                                       ON DELETE CASCADE,

                                               CONSTRAINT fk_doctor_availability_exception_created_by
                                                   FOREIGN KEY (created_by)
                                                       REFERENCES app_user(id),

                                               CONSTRAINT fk_doctor_availability_exception_updated_by
                                                   FOREIGN KEY (updated_by)
                                                       REFERENCES app_user(id),

                                               CONSTRAINT chk_doctor_availability_exception_type
                                                   CHECK (exception_type IN (
                                                                             'AVAILABLE',
                                                                             'UNAVAILABLE'
                                                       )),

                                               CONSTRAINT chk_doctor_availability_exception_time
                                                   CHECK (
                                                       (start_time IS NULL AND end_time IS NULL)
                                                           OR
                                                       (
                                                           start_time IS NOT NULL
                                                               AND end_time IS NOT NULL
                                                               AND end_time > start_time
                                                           )
                                                       )
);


CREATE INDEX idx_doctor_availability_exception_lookup
    ON doctor_availability_exception (
                                      doctor_id,
                                      exception_date
        );


-- =========================================================
-- 7. APPOINTMENT
-- =========================================================

CREATE TABLE appointment (
                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                             clinic_id UUID NOT NULL,

                             appointment_number VARCHAR(50) NOT NULL,

                             patient_id UUID NOT NULL,
                             doctor_id UUID NOT NULL,

                             department_id UUID,

                             chair_id UUID,

                             appointment_type VARCHAR(30) NOT NULL DEFAULT 'NEW',

                             appointment_date DATE NOT NULL,

                             start_time TIME NOT NULL,
                             end_time TIME NOT NULL,

                             estimated_duration_minutes INTEGER,

                             priority VARCHAR(20) NOT NULL DEFAULT 'ROUTINE',

                             reason TEXT,
                             remarks TEXT,

                             status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',

    -- Used when an appointment is rescheduled
    -- or connected to a previous appointment.
                             parent_appointment_id UUID,

                             checked_in_at TIMESTAMP,
                             consultation_started_at TIMESTAMP,
                             completed_at TIMESTAMP,
                             cancelled_at TIMESTAMP,

                             cancellation_reason TEXT,

                             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                             created_by UUID,
                             updated_by UUID,

                             CONSTRAINT fk_appointment_clinic
                                 FOREIGN KEY (clinic_id)
                                     REFERENCES clinic(id),

                             CONSTRAINT fk_appointment_patient
                                 FOREIGN KEY (patient_id)
                                     REFERENCES patient(id),

                             CONSTRAINT fk_appointment_doctor
                                 FOREIGN KEY (doctor_id)
                                     REFERENCES doctor_profile(id),

                             CONSTRAINT fk_appointment_department
                                 FOREIGN KEY (department_id)
                                     REFERENCES department(id),

                             CONSTRAINT fk_appointment_chair
                                 FOREIGN KEY (chair_id)
                                     REFERENCES dental_chair(id),

                             CONSTRAINT fk_appointment_parent
                                 FOREIGN KEY (parent_appointment_id)
                                     REFERENCES appointment(id),

                             CONSTRAINT fk_appointment_created_by
                                 FOREIGN KEY (created_by)
                                     REFERENCES app_user(id),

                             CONSTRAINT fk_appointment_updated_by
                                 FOREIGN KEY (updated_by)
                                     REFERENCES app_user(id),

                             CONSTRAINT uq_appointment_number
                                 UNIQUE (clinic_id, appointment_number),

                             CONSTRAINT chk_appointment_type
                                 CHECK (appointment_type IN (
                                                             'NEW',
                                                             'FOLLOW_UP',
                                                             'EMERGENCY'
                                     )),

                             CONSTRAINT chk_appointment_priority
                                 CHECK (priority IN (
                                                     'ROUTINE',
                                                     'URGENT',
                                                     'EMERGENCY'
                                     )),

                             CONSTRAINT chk_appointment_status
                                 CHECK (status IN (
                                                   'SCHEDULED',
                                                   'CHECKED_IN',
                                                   'WAITING',
                                                   'IN_CONSULTATION',
                                                   'COMPLETED',
                                                   'CANCELLED',
                                                   'NO_SHOW',
                                                   'RESCHEDULED'
                                     )),

                             CONSTRAINT chk_appointment_time
                                 CHECK (end_time > start_time),

                             CONSTRAINT chk_appointment_duration
                                 CHECK (
                                     estimated_duration_minutes IS NULL
                                         OR estimated_duration_minutes > 0
                                     )
);


CREATE INDEX idx_appointment_clinic
    ON appointment (clinic_id);


CREATE INDEX idx_appointment_patient
    ON appointment (patient_id);


CREATE INDEX idx_appointment_doctor
    ON appointment (doctor_id);


CREATE INDEX idx_appointment_date
    ON appointment (
                    clinic_id,
                    appointment_date
        );


CREATE INDEX idx_appointment_doctor_date
    ON appointment (
                    doctor_id,
                    appointment_date,
                    start_time
        );


CREATE INDEX idx_appointment_status
    ON appointment (
                    clinic_id,
                    appointment_date,
                    status
        );


CREATE INDEX idx_appointment_patient_history
    ON appointment (
                    patient_id,
                    appointment_date DESC
        );


CREATE INDEX idx_appointment_chair
    ON appointment (
                    chair_id,
                    appointment_date,
                    start_time
        )
    WHERE chair_id IS NOT NULL;


-- =========================================================
-- 8. APPOINTMENT STATUS HISTORY
--
-- Keeps complete lifecycle history.
--
-- Example:
--
-- SCHEDULED
-- CHECKED_IN
-- WAITING
-- IN_CONSULTATION
-- COMPLETED
-- =========================================================

CREATE TABLE appointment_status_history (
                                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                            clinic_id UUID NOT NULL,
                                            appointment_id UUID NOT NULL,

                                            from_status VARCHAR(30),
                                            to_status VARCHAR(30) NOT NULL,

                                            reason TEXT,

                                            changed_by UUID,

                                            changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                            CONSTRAINT fk_appointment_status_history_clinic
                                                FOREIGN KEY (clinic_id)
                                                    REFERENCES clinic(id),

                                            CONSTRAINT fk_appointment_status_history_appointment
                                                FOREIGN KEY (appointment_id)
                                                    REFERENCES appointment(id)
                                                    ON DELETE CASCADE,

                                            CONSTRAINT fk_appointment_status_history_changed_by
                                                FOREIGN KEY (changed_by)
                                                    REFERENCES app_user(id),

                                            CONSTRAINT chk_appointment_history_from_status
                                                CHECK (
                                                    from_status IS NULL
                                                        OR from_status IN (
                                                                           'SCHEDULED',
                                                                           'CHECKED_IN',
                                                                           'WAITING',
                                                                           'IN_CONSULTATION',
                                                                           'COMPLETED',
                                                                           'CANCELLED',
                                                                           'NO_SHOW',
                                                                           'RESCHEDULED'
                                                        )
                                                    ),

                                            CONSTRAINT chk_appointment_history_to_status
                                                CHECK (
                                                    to_status IN (
                                                                  'SCHEDULED',
                                                                  'CHECKED_IN',
                                                                  'WAITING',
                                                                  'IN_CONSULTATION',
                                                                  'COMPLETED',
                                                                  'CANCELLED',
                                                                  'NO_SHOW',
                                                                  'RESCHEDULED'
                                                        )
                                                    )
);


CREATE INDEX idx_appointment_status_history_appointment
    ON appointment_status_history (
                                   appointment_id,
                                   changed_at
        );


-- =========================================================
-- 9. ADD APPOINTMENT FK TO PATIENT DOCUMENT
--
-- patient_document was created before appointment,
-- so we add the FK now.
-- =========================================================

ALTER TABLE patient_document
    ADD CONSTRAINT fk_patient_document_appointment
        FOREIGN KEY (appointment_id)
            REFERENCES appointment(id);
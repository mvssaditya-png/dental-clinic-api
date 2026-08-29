-- =========================================================
-- DENTAL CLINIC APP
-- V10 - FOLLOW-UPS, NOTIFICATIONS & COMMUNICATION
-- =========================================================


-- =========================================================
-- 1. PATIENT FOLLOW-UP
--
-- Follow-up recommended by doctor after consultation/
-- procedure.
--
-- Example:
-- RCT completed -> follow-up after 7 days
-- =========================================================

CREATE TABLE patient_follow_up (
                                   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                   clinic_id UUID NOT NULL,
                                   patient_id UUID NOT NULL,

                                   appointment_id UUID,
                                   consultation_id UUID,
                                   case_sheet_id UUID,
                                   treatment_plan_id UUID,
                                   treatment_plan_item_id UUID,

                                   assigned_doctor_id UUID,

                                   follow_up_type VARCHAR(30) NOT NULL DEFAULT 'CLINICAL',

                                   scheduled_date DATE NOT NULL,
                                   preferred_time TIME,

                                   reason TEXT,
                                   instructions TEXT,

                                   status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

                                   completed_at TIMESTAMP,
                                   completed_by UUID,

                                   cancelled_at TIMESTAMP,
                                   cancellation_reason TEXT,

                                   next_appointment_id UUID,

                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                   created_by UUID,
                                   updated_by UUID,

                                   CONSTRAINT fk_follow_up_clinic
                                       FOREIGN KEY (clinic_id)
                                           REFERENCES clinic(id),

                                   CONSTRAINT fk_follow_up_patient
                                       FOREIGN KEY (patient_id)
                                           REFERENCES patient(id),

                                   CONSTRAINT fk_follow_up_appointment
                                       FOREIGN KEY (appointment_id)
                                           REFERENCES appointment(id),

                                   CONSTRAINT fk_follow_up_consultation
                                       FOREIGN KEY (consultation_id)
                                           REFERENCES consultation(id),

                                   CONSTRAINT fk_follow_up_case_sheet
                                       FOREIGN KEY (case_sheet_id)
                                           REFERENCES case_sheet(id),

                                   CONSTRAINT fk_follow_up_treatment_plan
                                       FOREIGN KEY (treatment_plan_id)
                                           REFERENCES treatment_plan(id),

                                   CONSTRAINT fk_follow_up_treatment_item
                                       FOREIGN KEY (treatment_plan_item_id)
                                           REFERENCES treatment_plan_item(id),

                                   CONSTRAINT fk_follow_up_doctor
                                       FOREIGN KEY (assigned_doctor_id)
                                           REFERENCES doctor_profile(id),

                                   CONSTRAINT fk_follow_up_completed_by
                                       FOREIGN KEY (completed_by)
                                           REFERENCES app_user(id),

                                   CONSTRAINT fk_follow_up_next_appointment
                                       FOREIGN KEY (next_appointment_id)
                                           REFERENCES appointment(id),

                                   CONSTRAINT fk_follow_up_created_by
                                       FOREIGN KEY (created_by)
                                           REFERENCES app_user(id),

                                   CONSTRAINT fk_follow_up_updated_by
                                       FOREIGN KEY (updated_by)
                                           REFERENCES app_user(id),

                                   CONSTRAINT chk_follow_up_type
                                       CHECK (follow_up_type IN (
                                                                 'CLINICAL',
                                                                 'POST_PROCEDURE',
                                                                 'TREATMENT',
                                                                 'PAYMENT',
                                                                 'REVIEW',
                                                                 'OTHER'
                                           )),

                                   CONSTRAINT chk_follow_up_status
                                       CHECK (status IN (
                                                         'PENDING',
                                                         'CONTACTED',
                                                         'APPOINTMENT_BOOKED',
                                                         'COMPLETED',
                                                         'MISSED',
                                                         'CANCELLED'
                                           ))
);


CREATE INDEX idx_follow_up_patient
    ON patient_follow_up (
                          patient_id,
                          scheduled_date DESC
        );

CREATE INDEX idx_follow_up_clinic_date
    ON patient_follow_up (
                          clinic_id,
                          scheduled_date
        );

CREATE INDEX idx_follow_up_status
    ON patient_follow_up (
                          clinic_id,
                          status,
                          scheduled_date
        );

CREATE INDEX idx_follow_up_doctor
    ON patient_follow_up (
                          assigned_doctor_id,
                          scheduled_date
        );


-- =========================================================
-- 2. NOTIFICATION TEMPLATE
--
-- Clinic-specific communication templates.
--
-- Example:
-- APPOINTMENT_REMINDER
-- PAYMENT_REMINDER
-- FOLLOW_UP_REMINDER
-- =========================================================

CREATE TABLE notification_template (
                                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                       clinic_id UUID NOT NULL,

                                       template_code VARCHAR(100) NOT NULL,
                                       template_name VARCHAR(150) NOT NULL,

                                       channel VARCHAR(30) NOT NULL,

                                       subject_template VARCHAR(255),
                                       body_template TEXT NOT NULL,

                                       provider_template_id VARCHAR(255),

                                       is_active BOOLEAN NOT NULL DEFAULT TRUE,

                                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                       created_by UUID,
                                       updated_by UUID,

                                       CONSTRAINT fk_notification_template_clinic
                                           FOREIGN KEY (clinic_id)
                                               REFERENCES clinic(id),

                                       CONSTRAINT fk_notification_template_created_by
                                           FOREIGN KEY (created_by)
                                               REFERENCES app_user(id),

                                       CONSTRAINT fk_notification_template_updated_by
                                           FOREIGN KEY (updated_by)
                                               REFERENCES app_user(id),

                                       CONSTRAINT uq_notification_template
                                           UNIQUE (
                                                   clinic_id,
                                                   template_code,
                                                   channel
                                               ),

                                       CONSTRAINT chk_notification_template_channel
                                           CHECK (channel IN (
                                                              'SMS',
                                                              'WHATSAPP',
                                                              'EMAIL',
                                                              'PUSH',
                                                              'IN_APP'
                                               ))
);


CREATE INDEX idx_notification_template_clinic
    ON notification_template (
                              clinic_id,
                              is_active
        );


-- =========================================================
-- 3. NOTIFICATION
--
-- Represents an actual communication that needs to be sent
-- or has already been sent.
--
-- We store rendered content here, not only template ID.
-- Therefore changing a template later does not change
-- historical communications.
-- =========================================================

CREATE TABLE notification (
                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                              clinic_id UUID NOT NULL,

                              patient_id UUID,
                              user_id UUID,

                              template_id UUID,

                              notification_type VARCHAR(50) NOT NULL,

                              channel VARCHAR(30) NOT NULL,

                              recipient VARCHAR(255),

                              subject TEXT,
                              message TEXT NOT NULL,

                              status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

                              scheduled_at TIMESTAMP,
                              sent_at TIMESTAMP,
                              delivered_at TIMESTAMP,
                              failed_at TIMESTAMP,

                              provider_message_id VARCHAR(255),

                              retry_count INTEGER NOT NULL DEFAULT 0,

                              failure_reason TEXT,

                              reference_type VARCHAR(50),
                              reference_id UUID,

                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              created_by UUID,

                              CONSTRAINT fk_notification_clinic
                                  FOREIGN KEY (clinic_id)
                                      REFERENCES clinic(id),

                              CONSTRAINT fk_notification_patient
                                  FOREIGN KEY (patient_id)
                                      REFERENCES patient(id),

                              CONSTRAINT fk_notification_user
                                  FOREIGN KEY (user_id)
                                      REFERENCES app_user(id),

                              CONSTRAINT fk_notification_template
                                  FOREIGN KEY (template_id)
                                      REFERENCES notification_template(id),

                              CONSTRAINT fk_notification_created_by
                                  FOREIGN KEY (created_by)
                                      REFERENCES app_user(id),

                              CONSTRAINT chk_notification_type
                                  CHECK (notification_type IN (
                                                               'APPOINTMENT_CONFIRMATION',
                                                               'APPOINTMENT_REMINDER',
                                                               'APPOINTMENT_RESCHEDULED',
                                                               'APPOINTMENT_CANCELLED',
                                                               'FOLLOW_UP_REMINDER',
                                                               'TREATMENT_REMINDER',
                                                               'PAYMENT_REMINDER',
                                                               'PAYMENT_RECEIPT',
                                                               'PRESCRIPTION',
                                                               'GENERAL'
                                      )),

                              CONSTRAINT chk_notification_channel
                                  CHECK (channel IN (
                                                     'SMS',
                                                     'WHATSAPP',
                                                     'EMAIL',
                                                     'PUSH',
                                                     'IN_APP'
                                      )),

                              CONSTRAINT chk_notification_status
                                  CHECK (status IN (
                                                    'PENDING',
                                                    'SCHEDULED',
                                                    'PROCESSING',
                                                    'SENT',
                                                    'DELIVERED',
                                                    'FAILED',
                                                    'CANCELLED'
                                      )),

                              CONSTRAINT chk_notification_retry
                                  CHECK (retry_count >= 0)
);


CREATE INDEX idx_notification_clinic
    ON notification (
                     clinic_id,
                     created_at DESC
        );

CREATE INDEX idx_notification_patient
    ON notification (
                     patient_id,
                     created_at DESC
        );

CREATE INDEX idx_notification_user
    ON notification (
                     user_id,
                     created_at DESC
        );

CREATE INDEX idx_notification_pending
    ON notification (
                     status,
                     scheduled_at
        )
    WHERE status IN ('PENDING', 'SCHEDULED');

CREATE INDEX idx_notification_reference
    ON notification (
                     reference_type,
                     reference_id
        );

CREATE INDEX idx_notification_provider_message
    ON notification (
                     provider_message_id
        )
    WHERE provider_message_id IS NOT NULL;


-- =========================================================
-- 4. NOTIFICATION ATTEMPT
--
-- Every provider/API attempt is recorded separately.
--
-- Example:
-- Attempt 1 -> MSG91 timeout
-- Attempt 2 -> success
-- =========================================================

CREATE TABLE notification_attempt (
                                      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                      notification_id UUID NOT NULL,

                                      attempt_number INTEGER NOT NULL,

                                      provider VARCHAR(100),

                                      provider_message_id VARCHAR(255),

                                      status VARCHAR(30) NOT NULL,

                                      response_code VARCHAR(100),
                                      response_message TEXT,

                                      attempted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                      CONSTRAINT fk_notification_attempt_notification
                                          FOREIGN KEY (notification_id)
                                              REFERENCES notification(id)
                                              ON DELETE CASCADE,

                                      CONSTRAINT uq_notification_attempt
                                          UNIQUE (
                                                  notification_id,
                                                  attempt_number
                                              ),

                                      CONSTRAINT chk_notification_attempt_number
                                          CHECK (attempt_number > 0),

                                      CONSTRAINT chk_notification_attempt_status
                                          CHECK (status IN (
                                                            'SUCCESS',
                                                            'FAILED',
                                                            'TIMEOUT'
                                              ))
);


CREATE INDEX idx_notification_attempt_notification
    ON notification_attempt (
                             notification_id,
                             attempted_at DESC
        );


-- =========================================================
-- 5. PATIENT COMMUNICATION PREFERENCE
--
-- Controls which channels the patient allows/prefers.
-- =========================================================

CREATE TABLE patient_communication_preference (
                                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                                  clinic_id UUID NOT NULL,
                                                  patient_id UUID NOT NULL,

                                                  allow_sms BOOLEAN NOT NULL DEFAULT TRUE,
                                                  allow_whatsapp BOOLEAN NOT NULL DEFAULT TRUE,
                                                  allow_email BOOLEAN NOT NULL DEFAULT TRUE,
                                                  allow_push BOOLEAN NOT NULL DEFAULT TRUE,

                                                  preferred_channel VARCHAR(30),

                                                  appointment_reminders BOOLEAN NOT NULL DEFAULT TRUE,
                                                  follow_up_reminders BOOLEAN NOT NULL DEFAULT TRUE,
                                                  payment_reminders BOOLEAN NOT NULL DEFAULT TRUE,

                                                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                  updated_by UUID,

                                                  CONSTRAINT fk_patient_communication_clinic
                                                      FOREIGN KEY (clinic_id)
                                                          REFERENCES clinic(id),

                                                  CONSTRAINT fk_patient_communication_patient
                                                      FOREIGN KEY (patient_id)
                                                          REFERENCES patient(id)
                                                          ON DELETE CASCADE,

                                                  CONSTRAINT fk_patient_communication_updated_by
                                                      FOREIGN KEY (updated_by)
                                                          REFERENCES app_user(id),

                                                  CONSTRAINT uq_patient_communication_preference
                                                      UNIQUE (patient_id),

                                                  CONSTRAINT chk_patient_preferred_channel
                                                      CHECK (
                                                          preferred_channel IS NULL
                                                              OR preferred_channel IN (
                                                                                       'SMS',
                                                                                       'WHATSAPP',
                                                                                       'EMAIL',
                                                                                       'PUSH'
                                                              )
                                                          )
);


CREATE INDEX idx_patient_communication_clinic
    ON patient_communication_preference (clinic_id);


-- =========================================================
-- 6. FOLLOW-UP ACTIVITY
--
-- History of actions performed for a follow-up.
--
-- Example:
-- CREATED
-- REMINDER_SENT
-- PATIENT_CALLED
-- APPOINTMENT_BOOKED
-- COMPLETED
-- =========================================================

CREATE TABLE follow_up_activity (
                                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                    clinic_id UUID NOT NULL,
                                    follow_up_id UUID NOT NULL,

                                    activity_type VARCHAR(50) NOT NULL,

                                    notes TEXT,

                                    performed_by UUID,

                                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                    CONSTRAINT fk_follow_up_activity_clinic
                                        FOREIGN KEY (clinic_id)
                                            REFERENCES clinic(id),

                                    CONSTRAINT fk_follow_up_activity_follow_up
                                        FOREIGN KEY (follow_up_id)
                                            REFERENCES patient_follow_up(id)
                                            ON DELETE CASCADE,

                                    CONSTRAINT fk_follow_up_activity_performed_by
                                        FOREIGN KEY (performed_by)
                                            REFERENCES app_user(id),

                                    CONSTRAINT chk_follow_up_activity_type
                                        CHECK (activity_type IN (
                                                                 'CREATED',
                                                                 'REMINDER_SENT',
                                                                 'PATIENT_CALLED',
                                                                 'PATIENT_CONTACTED',
                                                                 'APPOINTMENT_BOOKED',
                                                                 'RESCHEDULED',
                                                                 'MISSED',
                                                                 'COMPLETED',
                                                                 'CANCELLED',
                                                                 'NOTE_ADDED'
                                            ))
);


CREATE INDEX idx_follow_up_activity_follow_up
    ON follow_up_activity (
                           follow_up_id,
                           created_at DESC
        );
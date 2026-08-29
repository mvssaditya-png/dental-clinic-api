-- =========================================================
-- DENTAL CLINIC APP
-- V14 - ASSET MANAGEMENT & MAINTENANCE
-- =========================================================


-- =========================================================
-- 1. ASSET CATEGORY
--
-- Examples:
-- Dental Equipment
-- Diagnostic Equipment
-- Sterilization Equipment
-- IT Equipment
-- Furniture
-- Electrical Equipment
-- =========================================================

CREATE TABLE asset_category (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                clinic_id UUID NOT NULL,

                                category_code VARCHAR(50) NOT NULL,
                                category_name VARCHAR(150) NOT NULL,

                                description TEXT,

                                is_active BOOLEAN NOT NULL DEFAULT TRUE,

                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                created_by UUID,
                                updated_by UUID,

                                CONSTRAINT fk_asset_category_clinic
                                    FOREIGN KEY (clinic_id)
                                        REFERENCES clinic(id),

                                CONSTRAINT fk_asset_category_created_by
                                    FOREIGN KEY (created_by)
                                        REFERENCES app_user(id),

                                CONSTRAINT fk_asset_category_updated_by
                                    FOREIGN KEY (updated_by)
                                        REFERENCES app_user(id),

                                CONSTRAINT uq_asset_category_code
                                    UNIQUE (
                                            clinic_id,
                                            category_code
                                        ),

                                CONSTRAINT uq_asset_category_name
                                    UNIQUE (
                                            clinic_id,
                                            category_name
                                        )
);


CREATE INDEX idx_asset_category_clinic
    ON asset_category (clinic_id);

CREATE INDEX idx_asset_category_active
    ON asset_category (
                       clinic_id,
                       is_active
        );


-- =========================================================
-- 2. ASSET
--
-- Main asset registry.
--
-- Examples:
-- Dental Chair #1
-- X-Ray Machine
-- Autoclave
-- Compressor
-- RVG Sensor
--
-- Assets may originate from a vendor / PO / vendor invoice.
-- =========================================================

CREATE TABLE asset (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                       clinic_id UUID NOT NULL,
                       category_id UUID NOT NULL,

                       vendor_id UUID,

                       purchase_order_id UUID,
                       vendor_invoice_id UUID,

                       asset_code VARCHAR(50) NOT NULL,
                       asset_name VARCHAR(200) NOT NULL,

                       description TEXT,

                       manufacturer VARCHAR(150),
                       brand VARCHAR(150),
                       model_number VARCHAR(100),
                       serial_number VARCHAR(150),

                       purchase_date DATE,
                       purchase_price NUMERIC(12,2),

                       installation_date DATE,

                       warranty_start_date DATE,
                       warranty_end_date DATE,

                       location VARCHAR(200),

                       status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

                       condition_status VARCHAR(30) NOT NULL DEFAULT 'GOOD',

                       last_service_date DATE,
                       next_service_date DATE,

                       replacement_due_date DATE,

                       disposed_at TIMESTAMP,
                       disposal_reason TEXT,

                       notes TEXT,

                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                       created_by UUID,
                       updated_by UUID,

                       CONSTRAINT fk_asset_clinic
                           FOREIGN KEY (clinic_id)
                               REFERENCES clinic(id),

                       CONSTRAINT fk_asset_category
                           FOREIGN KEY (category_id)
                               REFERENCES asset_category(id),

                       CONSTRAINT fk_asset_vendor
                           FOREIGN KEY (vendor_id)
                               REFERENCES vendor(id),

                       CONSTRAINT fk_asset_purchase_order
                           FOREIGN KEY (purchase_order_id)
                               REFERENCES purchase_order(id),

                       CONSTRAINT fk_asset_vendor_invoice
                           FOREIGN KEY (vendor_invoice_id)
                               REFERENCES vendor_invoice(id),

                       CONSTRAINT fk_asset_created_by
                           FOREIGN KEY (created_by)
                               REFERENCES app_user(id),

                       CONSTRAINT fk_asset_updated_by
                           FOREIGN KEY (updated_by)
                               REFERENCES app_user(id),

                       CONSTRAINT uq_asset_code
                           UNIQUE (
                                   clinic_id,
                                   asset_code
                               ),

                       CONSTRAINT chk_asset_purchase_price
                           CHECK (
                               purchase_price IS NULL
                                   OR purchase_price >= 0
                               ),

                       CONSTRAINT chk_asset_status
                           CHECK (
                               status IN (
                                          'ACTIVE',
                                          'UNDER_MAINTENANCE',
                                          'OUT_OF_SERVICE',
                                          'RETIRED',
                                          'DISPOSED'
                                   )
                               ),

                       CONSTRAINT chk_asset_condition
                           CHECK (
                               condition_status IN (
                                                    'NEW',
                                                    'GOOD',
                                                    'FAIR',
                                                    'POOR',
                                                    'DAMAGED'
                                   )
                               ),

                       CONSTRAINT chk_asset_warranty_dates
                           CHECK (
                               warranty_start_date IS NULL
                                   OR warranty_end_date IS NULL
                                   OR warranty_end_date >= warranty_start_date
                               ),

                       CONSTRAINT chk_asset_installation_date
                           CHECK (
                               purchase_date IS NULL
                                   OR installation_date IS NULL
                                   OR installation_date >= purchase_date
                               ),

                       CONSTRAINT chk_asset_service_dates
                           CHECK (
                               last_service_date IS NULL
                                   OR next_service_date IS NULL
                                   OR next_service_date >= last_service_date
                               )
);


CREATE INDEX idx_asset_clinic
    ON asset (clinic_id);

CREATE INDEX idx_asset_category
    ON asset (category_id);

CREATE INDEX idx_asset_vendor
    ON asset (vendor_id);

CREATE INDEX idx_asset_status
    ON asset (
              clinic_id,
              status
        );

CREATE INDEX idx_asset_condition
    ON asset (
              clinic_id,
              condition_status
        );

CREATE INDEX idx_asset_next_service
    ON asset (
              clinic_id,
              next_service_date
        )
    WHERE next_service_date IS NOT NULL;

CREATE INDEX idx_asset_warranty
    ON asset (
              clinic_id,
              warranty_end_date
        )
    WHERE warranty_end_date IS NOT NULL;

CREATE INDEX idx_asset_serial_number
    ON asset (
              clinic_id,
              serial_number
        )
    WHERE serial_number IS NOT NULL;


-- =========================================================
-- 3. ASSET DOCUMENT
--
-- Files remain in S3.
--
-- Examples:
-- Purchase Invoice
-- Warranty Certificate
-- AMC Agreement
-- User Manual
-- Installation Report
-- Service Report
-- =========================================================

CREATE TABLE asset_document (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                clinic_id UUID NOT NULL,
                                asset_id UUID NOT NULL,

                                document_type VARCHAR(30) NOT NULL,

                                document_name VARCHAR(255) NOT NULL,

                                file_name VARCHAR(255),
                                file_url TEXT NOT NULL,

                                mime_type VARCHAR(150),
                                file_size BIGINT,

                                document_date DATE,
                                expiry_date DATE,

                                description TEXT,

                                uploaded_by UUID,

                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT fk_asset_document_clinic
                                    FOREIGN KEY (clinic_id)
                                        REFERENCES clinic(id),

                                CONSTRAINT fk_asset_document_asset
                                    FOREIGN KEY (asset_id)
                                        REFERENCES asset(id)
                                        ON DELETE CASCADE,

                                CONSTRAINT fk_asset_document_uploaded_by
                                    FOREIGN KEY (uploaded_by)
                                        REFERENCES app_user(id),

                                CONSTRAINT chk_asset_document_type
                                    CHECK (
                                        document_type IN (
                                                          'PURCHASE_INVOICE',
                                                          'WARRANTY',
                                                          'AMC',
                                                          'MANUAL',
                                                          'INSTALLATION_REPORT',
                                                          'SERVICE_REPORT',
                                                          'CALIBRATION_CERTIFICATE',
                                                          'DISPOSAL_DOCUMENT',
                                                          'OTHER'
                                            )
                                        ),

                                CONSTRAINT chk_asset_document_size
                                    CHECK (
                                        file_size IS NULL
                                            OR file_size >= 0
                                        ),

                                CONSTRAINT chk_asset_document_dates
                                    CHECK (
                                        document_date IS NULL
                                            OR expiry_date IS NULL
                                            OR expiry_date >= document_date
                                        )
);


CREATE INDEX idx_asset_document_asset
    ON asset_document (
                       asset_id,
                       created_at DESC
        );

CREATE INDEX idx_asset_document_expiry
    ON asset_document (
                       clinic_id,
                       expiry_date
        )
    WHERE expiry_date IS NOT NULL;


-- =========================================================
-- 4. ASSET MAINTENANCE
--
-- Represents maintenance/service work that is planned,
-- requested, in progress, or completed.
--
-- Examples:
-- Preventive service every 6 months
-- Breakdown repair
-- Calibration
-- AMC service
-- =========================================================

CREATE TABLE asset_maintenance (
                                   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                   clinic_id UUID NOT NULL,
                                   asset_id UUID NOT NULL,

                                   maintenance_number VARCHAR(50) NOT NULL,

                                   maintenance_type VARCHAR(30) NOT NULL,

                                   scheduled_date DATE,

                                   reported_at TIMESTAMP,

                                   started_at TIMESTAMP,
                                   completed_at TIMESTAMP,

                                   issue_description TEXT,

                                   work_description TEXT,

                                   vendor_id UUID,

                                   technician_name VARCHAR(150),
                                   technician_contact VARCHAR(50),

                                   estimated_cost NUMERIC(12,2),
                                   actual_cost NUMERIC(12,2),

                                   status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',

                                   next_service_date DATE,

                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                   created_by UUID,
                                   updated_by UUID,

                                   CONSTRAINT fk_asset_maintenance_clinic
                                       FOREIGN KEY (clinic_id)
                                           REFERENCES clinic(id),

                                   CONSTRAINT fk_asset_maintenance_asset
                                       FOREIGN KEY (asset_id)
                                           REFERENCES asset(id),

                                   CONSTRAINT fk_asset_maintenance_vendor
                                       FOREIGN KEY (vendor_id)
                                           REFERENCES vendor(id),

                                   CONSTRAINT fk_asset_maintenance_created_by
                                       FOREIGN KEY (created_by)
                                           REFERENCES app_user(id),

                                   CONSTRAINT fk_asset_maintenance_updated_by
                                       FOREIGN KEY (updated_by)
                                           REFERENCES app_user(id),

                                   CONSTRAINT uq_asset_maintenance_number
                                       UNIQUE (
                                               clinic_id,
                                               maintenance_number
                                           ),

                                   CONSTRAINT chk_asset_maintenance_type
                                       CHECK (
                                           maintenance_type IN (
                                                                'PREVENTIVE',
                                                                'CORRECTIVE',
                                                                'BREAKDOWN',
                                                                'CALIBRATION',
                                                                'AMC_SERVICE',
                                                                'INSPECTION',
                                                                'OTHER'
                                               )
                                           ),

                                   CONSTRAINT chk_asset_maintenance_status
                                       CHECK (
                                           status IN (
                                                      'SCHEDULED',
                                                      'REPORTED',
                                                      'IN_PROGRESS',
                                                      'COMPLETED',
                                                      'CANCELLED'
                                               )
                                           ),

                                   CONSTRAINT chk_asset_maintenance_estimated_cost
                                       CHECK (
                                           estimated_cost IS NULL
                                               OR estimated_cost >= 0
                                           ),

                                   CONSTRAINT chk_asset_maintenance_actual_cost
                                       CHECK (
                                           actual_cost IS NULL
                                               OR actual_cost >= 0
                                           ),

                                   CONSTRAINT chk_asset_maintenance_times
                                       CHECK (
                                           started_at IS NULL
                                               OR completed_at IS NULL
                                               OR completed_at >= started_at
                                           )
);


CREATE INDEX idx_asset_maintenance_asset
    ON asset_maintenance (
                          asset_id,
                          scheduled_date DESC
        );

CREATE INDEX idx_asset_maintenance_status
    ON asset_maintenance (
                          clinic_id,
                          status
        );

CREATE INDEX idx_asset_maintenance_schedule
    ON asset_maintenance (
                          clinic_id,
                          scheduled_date
        )
    WHERE status IN (
    'SCHEDULED',
    'REPORTED',
    'IN_PROGRESS'
);

CREATE INDEX idx_asset_maintenance_vendor
    ON asset_maintenance (vendor_id);


-- =========================================================
-- 5. ASSET SERVICE HISTORY
--
-- Immutable history of actual service/maintenance events.
--
-- asset_maintenance = work order / maintenance lifecycle
-- asset_service_history = completed service event
-- =========================================================

CREATE TABLE asset_service_history (
                                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                       clinic_id UUID NOT NULL,
                                       asset_id UUID NOT NULL,

                                       maintenance_id UUID,

                                       service_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                       service_type VARCHAR(30) NOT NULL,

                                       vendor_id UUID,

                                       technician_name VARCHAR(150),

                                       problem_reported TEXT,
                                       work_performed TEXT NOT NULL,

                                       parts_replaced TEXT,

                                       service_cost NUMERIC(12,2) NOT NULL DEFAULT 0,

                                       condition_before VARCHAR(30),
                                       condition_after VARCHAR(30),

                                       next_service_date DATE,

                                       service_report_url TEXT,

                                       performed_by UUID,

                                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                       CONSTRAINT fk_asset_service_history_clinic
                                           FOREIGN KEY (clinic_id)
                                               REFERENCES clinic(id),

                                       CONSTRAINT fk_asset_service_history_asset
                                           FOREIGN KEY (asset_id)
                                               REFERENCES asset(id),

                                       CONSTRAINT fk_asset_service_history_maintenance
                                           FOREIGN KEY (maintenance_id)
                                               REFERENCES asset_maintenance(id),

                                       CONSTRAINT fk_asset_service_history_vendor
                                           FOREIGN KEY (vendor_id)
                                               REFERENCES vendor(id),

                                       CONSTRAINT fk_asset_service_history_performed_by
                                           FOREIGN KEY (performed_by)
                                               REFERENCES app_user(id),

                                       CONSTRAINT chk_asset_service_type
                                           CHECK (
                                               service_type IN (
                                                                'PREVENTIVE',
                                                                'CORRECTIVE',
                                                                'BREAKDOWN',
                                                                'CALIBRATION',
                                                                'AMC_SERVICE',
                                                                'INSPECTION',
                                                                'OTHER'
                                                   )
                                               ),

                                       CONSTRAINT chk_asset_service_cost
                                           CHECK (service_cost >= 0),

                                       CONSTRAINT chk_asset_condition_before
                                           CHECK (
                                               condition_before IS NULL
                                                   OR condition_before IN (
                                                                           'NEW',
                                                                           'GOOD',
                                                                           'FAIR',
                                                                           'POOR',
                                                                           'DAMAGED'
                                                   )
                                               ),

                                       CONSTRAINT chk_asset_condition_after
                                           CHECK (
                                               condition_after IS NULL
                                                   OR condition_after IN (
                                                                          'NEW',
                                                                          'GOOD',
                                                                          'FAIR',
                                                                          'POOR',
                                                                          'DAMAGED'
                                                   )
                                               )
);


CREATE INDEX idx_asset_service_history_asset
    ON asset_service_history (
                              asset_id,
                              service_date DESC
        );

CREATE INDEX idx_asset_service_history_maintenance
    ON asset_service_history (maintenance_id);

CREATE INDEX idx_asset_service_history_vendor
    ON asset_service_history (
                              vendor_id,
                              service_date DESC
        );


-- =========================================================
-- 6. ASSET STATUS HISTORY
--
-- Immutable history of major asset lifecycle/status changes.
--
-- Example:
--
-- ACTIVE
--   ↓
-- UNDER_MAINTENANCE
--   ↓
-- ACTIVE
--   ↓
-- OUT_OF_SERVICE
--   ↓
-- RETIRED
--   ↓
-- DISPOSED
-- =========================================================

CREATE TABLE asset_status_history (
                                      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                      clinic_id UUID NOT NULL,
                                      asset_id UUID NOT NULL,

                                      previous_status VARCHAR(30),
                                      new_status VARCHAR(30) NOT NULL,

                                      previous_condition VARCHAR(30),
                                      new_condition VARCHAR(30),

                                      reason TEXT,

                                      changed_by UUID NOT NULL,
                                      changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                      CONSTRAINT fk_asset_status_history_clinic
                                          FOREIGN KEY (clinic_id)
                                              REFERENCES clinic(id),

                                      CONSTRAINT fk_asset_status_history_asset
                                          FOREIGN KEY (asset_id)
                                              REFERENCES asset(id),

                                      CONSTRAINT fk_asset_status_history_changed_by
                                          FOREIGN KEY (changed_by)
                                              REFERENCES app_user(id),

                                      CONSTRAINT chk_asset_previous_status
                                          CHECK (
                                              previous_status IS NULL
                                                  OR previous_status IN (
                                                                         'ACTIVE',
                                                                         'UNDER_MAINTENANCE',
                                                                         'OUT_OF_SERVICE',
                                                                         'RETIRED',
                                                                         'DISPOSED'
                                                  )
                                              ),

                                      CONSTRAINT chk_asset_new_status
                                          CHECK (
                                              new_status IN (
                                                             'ACTIVE',
                                                             'UNDER_MAINTENANCE',
                                                             'OUT_OF_SERVICE',
                                                             'RETIRED',
                                                             'DISPOSED'
                                                  )
                                              ),

                                      CONSTRAINT chk_asset_previous_condition
                                          CHECK (
                                              previous_condition IS NULL
                                                  OR previous_condition IN (
                                                                            'NEW',
                                                                            'GOOD',
                                                                            'FAIR',
                                                                            'POOR',
                                                                            'DAMAGED'
                                                  )
                                              ),

                                      CONSTRAINT chk_asset_new_condition
                                          CHECK (
                                              new_condition IS NULL
                                                  OR new_condition IN (
                                                                       'NEW',
                                                                       'GOOD',
                                                                       'FAIR',
                                                                       'POOR',
                                                                       'DAMAGED'
                                                  )
                                              )
);


CREATE INDEX idx_asset_status_history_asset
    ON asset_status_history (
                             asset_id,
                             changed_at DESC
        );


-- =========================================================
-- 7. ASSET MAINTENANCE ATTACHMENT
--
-- Additional files attached to a particular maintenance
-- work order/event.
--
-- Examples:
-- Technician quotation
-- Before/after photo
-- Service bill
-- Service report
-- =========================================================

CREATE TABLE asset_maintenance_attachment (
                                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                              clinic_id UUID NOT NULL,

                                              maintenance_id UUID NOT NULL,

                                              attachment_type VARCHAR(30) NOT NULL DEFAULT 'OTHER',

                                              file_name VARCHAR(255) NOT NULL,
                                              file_url TEXT NOT NULL,

                                              mime_type VARCHAR(150),
                                              file_size BIGINT,

                                              description TEXT,

                                              uploaded_by UUID,

                                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                              CONSTRAINT fk_asset_maintenance_attachment_clinic
                                                  FOREIGN KEY (clinic_id)
                                                      REFERENCES clinic(id),

                                              CONSTRAINT fk_asset_maintenance_attachment
                                                  FOREIGN KEY (maintenance_id)
                                                      REFERENCES asset_maintenance(id)
                                                      ON DELETE CASCADE,

                                              CONSTRAINT fk_asset_maintenance_attachment_user
                                                  FOREIGN KEY (uploaded_by)
                                                      REFERENCES app_user(id),

                                              CONSTRAINT chk_asset_maintenance_attachment_type
                                                  CHECK (
                                                      attachment_type IN (
                                                                          'QUOTATION',
                                                                          'SERVICE_REPORT',
                                                                          'BILL',
                                                                          'PHOTO',
                                                                          'CALIBRATION_CERTIFICATE',
                                                                          'OTHER'
                                                          )
                                                      ),

                                              CONSTRAINT chk_asset_maintenance_attachment_size
                                                  CHECK (
                                                      file_size IS NULL
                                                          OR file_size >= 0
                                                      )
);


CREATE INDEX idx_asset_maintenance_attachment
    ON asset_maintenance_attachment (
                                     maintenance_id,
                                     created_at DESC
        );
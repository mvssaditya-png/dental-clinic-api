-- =========================================================
-- DENTAL CLINIC APP
-- V8 - BILLING & PAYMENTS
-- =========================================================


-- =========================================================
-- 1. INVOICE
-- =========================================================

CREATE TABLE invoice (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                         clinic_id UUID NOT NULL,
                         patient_id UUID NOT NULL,

                         treatment_plan_id UUID,
                         appointment_id UUID,

                         invoice_number VARCHAR(50) NOT NULL,

                         invoice_date DATE NOT NULL DEFAULT CURRENT_DATE,
                         due_date DATE,

                         status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',

                         subtotal NUMERIC(12,2) NOT NULL DEFAULT 0,

                         discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
                         tax_amount NUMERIC(12,2) NOT NULL DEFAULT 0,

                         total_amount NUMERIC(12,2) NOT NULL DEFAULT 0,

                         paid_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
                         balance_amount NUMERIC(12,2) NOT NULL DEFAULT 0,

                         notes TEXT,

                         issued_at TIMESTAMP,
                         cancelled_at TIMESTAMP,
                         cancellation_reason TEXT,

                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                         created_by UUID,
                         updated_by UUID,

                         CONSTRAINT fk_invoice_clinic
                             FOREIGN KEY (clinic_id)
                                 REFERENCES clinic(id),

                         CONSTRAINT fk_invoice_patient
                             FOREIGN KEY (patient_id)
                                 REFERENCES patient(id),

                         CONSTRAINT fk_invoice_treatment_plan
                             FOREIGN KEY (treatment_plan_id)
                                 REFERENCES treatment_plan(id),

                         CONSTRAINT fk_invoice_appointment
                             FOREIGN KEY (appointment_id)
                                 REFERENCES appointment(id),

                         CONSTRAINT fk_invoice_created_by
                             FOREIGN KEY (created_by)
                                 REFERENCES app_user(id),

                         CONSTRAINT fk_invoice_updated_by
                             FOREIGN KEY (updated_by)
                                 REFERENCES app_user(id),

                         CONSTRAINT uq_invoice_number
                             UNIQUE (clinic_id, invoice_number),

                         CONSTRAINT chk_invoice_status
                             CHECK (status IN (
                                               'DRAFT',
                                               'ISSUED',
                                               'PARTIALLY_PAID',
                                               'PAID',
                                               'CANCELLED'
                                 )),

                         CONSTRAINT chk_invoice_subtotal
                             CHECK (subtotal >= 0),

                         CONSTRAINT chk_invoice_discount
                             CHECK (discount_amount >= 0),

                         CONSTRAINT chk_invoice_tax
                             CHECK (tax_amount >= 0),

                         CONSTRAINT chk_invoice_total
                             CHECK (total_amount >= 0),

                         CONSTRAINT chk_invoice_paid
                             CHECK (paid_amount >= 0),

                         CONSTRAINT chk_invoice_balance
                             CHECK (balance_amount >= 0),

                         CONSTRAINT chk_invoice_due_date
                             CHECK (
                                 due_date IS NULL
                                     OR due_date >= invoice_date
                                 )
);


CREATE INDEX idx_invoice_clinic
    ON invoice (clinic_id);

CREATE INDEX idx_invoice_patient
    ON invoice (
                patient_id,
                invoice_date DESC
        );

CREATE INDEX idx_invoice_treatment_plan
    ON invoice (treatment_plan_id);

CREATE INDEX idx_invoice_appointment
    ON invoice (appointment_id);

CREATE INDEX idx_invoice_status
    ON invoice (
                clinic_id,
                status
        );

CREATE INDEX idx_invoice_due
    ON invoice (
                clinic_id,
                due_date
        )
    WHERE balance_amount > 0;


-- =========================================================
-- 2. INVOICE ITEM
--
-- Snapshot of the billed procedure/service.
-- Historical invoices must not change when procedure
-- master/pricing changes later.
-- =========================================================

CREATE TABLE invoice_item (
                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                              clinic_id UUID NOT NULL,
                              invoice_id UUID NOT NULL,

                              treatment_plan_item_id UUID,
                              procedure_id UUID,

                              tooth_number VARCHAR(10),

                              item_code_snapshot VARCHAR(50),
                              item_name_snapshot VARCHAR(255) NOT NULL,

                              description TEXT,

                              quantity INTEGER NOT NULL DEFAULT 1,

                              unit_price NUMERIC(12,2) NOT NULL DEFAULT 0,

                              gross_amount NUMERIC(12,2) NOT NULL DEFAULT 0,

                              discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0,

                              taxable_amount NUMERIC(12,2) NOT NULL DEFAULT 0,

                              tax_rate NUMERIC(7,4) NOT NULL DEFAULT 0,
                              tax_amount NUMERIC(12,2) NOT NULL DEFAULT 0,

                              final_amount NUMERIC(12,2) NOT NULL DEFAULT 0,

                              display_order INTEGER NOT NULL DEFAULT 0,

                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                              CONSTRAINT fk_invoice_item_clinic
                                  FOREIGN KEY (clinic_id)
                                      REFERENCES clinic(id),

                              CONSTRAINT fk_invoice_item_invoice
                                  FOREIGN KEY (invoice_id)
                                      REFERENCES invoice(id)
                                      ON DELETE CASCADE,

                              CONSTRAINT fk_invoice_item_treatment_plan_item
                                  FOREIGN KEY (treatment_plan_item_id)
                                      REFERENCES treatment_plan_item(id),

                              CONSTRAINT fk_invoice_item_procedure
                                  FOREIGN KEY (procedure_id)
                                      REFERENCES procedure_master(id),

                              CONSTRAINT chk_invoice_item_quantity
                                  CHECK (quantity > 0),

                              CONSTRAINT chk_invoice_item_unit_price
                                  CHECK (unit_price >= 0),

                              CONSTRAINT chk_invoice_item_gross
                                  CHECK (gross_amount >= 0),

                              CONSTRAINT chk_invoice_item_discount
                                  CHECK (discount_amount >= 0),

                              CONSTRAINT chk_invoice_item_taxable
                                  CHECK (taxable_amount >= 0),

                              CONSTRAINT chk_invoice_item_tax_rate
                                  CHECK (tax_rate >= 0),

                              CONSTRAINT chk_invoice_item_tax
                                  CHECK (tax_amount >= 0),

                              CONSTRAINT chk_invoice_item_final
                                  CHECK (final_amount >= 0),

                              CONSTRAINT chk_invoice_item_display_order
                                  CHECK (display_order >= 0)
);


CREATE INDEX idx_invoice_item_invoice
    ON invoice_item (
                     invoice_id,
                     display_order
        );

CREATE INDEX idx_invoice_item_treatment
    ON invoice_item (treatment_plan_item_id);

CREATE INDEX idx_invoice_item_procedure
    ON invoice_item (procedure_id);


-- =========================================================
-- 3. PAYMENT
--
-- Represents money actually received from a patient.
-- =========================================================

CREATE TABLE payment (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                         clinic_id UUID NOT NULL,
                         patient_id UUID NOT NULL,

                         payment_number VARCHAR(50) NOT NULL,

                         payment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                         amount NUMERIC(12,2) NOT NULL,

                         payment_mode VARCHAR(30) NOT NULL,

                         status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',

                         transaction_reference VARCHAR(255),

                         notes TEXT,

                         received_by UUID NOT NULL,

                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                         created_by UUID,
                         updated_by UUID,

                         CONSTRAINT fk_payment_clinic
                             FOREIGN KEY (clinic_id)
                                 REFERENCES clinic(id),

                         CONSTRAINT fk_payment_patient
                             FOREIGN KEY (patient_id)
                                 REFERENCES patient(id),

                         CONSTRAINT fk_payment_received_by
                             FOREIGN KEY (received_by)
                                 REFERENCES app_user(id),

                         CONSTRAINT fk_payment_created_by
                             FOREIGN KEY (created_by)
                                 REFERENCES app_user(id),

                         CONSTRAINT fk_payment_updated_by
                             FOREIGN KEY (updated_by)
                                 REFERENCES app_user(id),

                         CONSTRAINT uq_payment_number
                             UNIQUE (clinic_id, payment_number),

                         CONSTRAINT chk_payment_amount
                             CHECK (amount > 0),

                         CONSTRAINT chk_payment_mode
                             CHECK (payment_mode IN (
                                                     'CASH',
                                                     'UPI',
                                                     'CARD',
                                                     'BANK_TRANSFER',
                                                     'CHEQUE',
                                                     'ONLINE',
                                                     'OTHER'
                                 )),

                         CONSTRAINT chk_payment_status
                             CHECK (status IN (
                                               'PENDING',
                                               'COMPLETED',
                                               'FAILED',
                                               'CANCELLED',
                                               'PARTIALLY_REFUNDED',
                                               'REFUNDED'
                                 ))
);


CREATE INDEX idx_payment_clinic
    ON payment (clinic_id);

CREATE INDEX idx_payment_patient
    ON payment (
                patient_id,
                payment_date DESC
        );

CREATE INDEX idx_payment_date
    ON payment (
                clinic_id,
                payment_date DESC
        );

CREATE INDEX idx_payment_status
    ON payment (
                clinic_id,
                status
        );

CREATE INDEX idx_payment_transaction_reference
    ON payment (
                clinic_id,
                transaction_reference
        )
    WHERE transaction_reference IS NOT NULL;


-- =========================================================
-- 4. PAYMENT ALLOCATION
--
-- Connects payments to invoices.
--
-- Allows:
-- One invoice -> multiple payments
-- One payment -> multiple invoices
-- =========================================================

CREATE TABLE payment_allocation (
                                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                    clinic_id UUID NOT NULL,

                                    payment_id UUID NOT NULL,
                                    invoice_id UUID NOT NULL,

                                    allocated_amount NUMERIC(12,2) NOT NULL,

                                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    created_by UUID,

                                    CONSTRAINT fk_payment_allocation_clinic
                                        FOREIGN KEY (clinic_id)
                                            REFERENCES clinic(id),

                                    CONSTRAINT fk_payment_allocation_payment
                                        FOREIGN KEY (payment_id)
                                            REFERENCES payment(id),

                                    CONSTRAINT fk_payment_allocation_invoice
                                        FOREIGN KEY (invoice_id)
                                            REFERENCES invoice(id),

                                    CONSTRAINT fk_payment_allocation_created_by
                                        FOREIGN KEY (created_by)
                                            REFERENCES app_user(id),

                                    CONSTRAINT uq_payment_invoice_allocation
                                        UNIQUE (payment_id, invoice_id),

                                    CONSTRAINT chk_payment_allocation_amount
                                        CHECK (allocated_amount > 0)
);


CREATE INDEX idx_payment_allocation_payment
    ON payment_allocation (payment_id);

CREATE INDEX idx_payment_allocation_invoice
    ON payment_allocation (invoice_id);


-- =========================================================
-- 5. RECEIPT
--
-- Patient-facing acknowledgement of a completed payment.
-- PDF URL will eventually point to AWS S3.
-- =========================================================

CREATE TABLE receipt (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                         clinic_id UUID NOT NULL,

                         payment_id UUID NOT NULL,
                         patient_id UUID NOT NULL,

                         receipt_number VARCHAR(50) NOT NULL,

                         amount NUMERIC(12,2) NOT NULL,

                         issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         issued_by UUID NOT NULL,

                         pdf_url TEXT,

                         notes TEXT,

                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                         CONSTRAINT fk_receipt_clinic
                             FOREIGN KEY (clinic_id)
                                 REFERENCES clinic(id),

                         CONSTRAINT fk_receipt_payment
                             FOREIGN KEY (payment_id)
                                 REFERENCES payment(id),

                         CONSTRAINT fk_receipt_patient
                             FOREIGN KEY (patient_id)
                                 REFERENCES patient(id),

                         CONSTRAINT fk_receipt_issued_by
                             FOREIGN KEY (issued_by)
                                 REFERENCES app_user(id),

                         CONSTRAINT uq_receipt_number
                             UNIQUE (clinic_id, receipt_number),

                         CONSTRAINT uq_receipt_payment
                             UNIQUE (payment_id),

                         CONSTRAINT chk_receipt_amount
                             CHECK (amount > 0)
);


CREATE INDEX idx_receipt_patient
    ON receipt (
                patient_id,
                issued_at DESC
        );


-- =========================================================
-- 6. REFUND
--
-- Refunds are separate transactions.
-- Never reduce/change the original payment amount.
-- =========================================================

CREATE TABLE refund (
                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                        clinic_id UUID NOT NULL,

                        payment_id UUID NOT NULL,
                        patient_id UUID NOT NULL,

                        refund_number VARCHAR(50) NOT NULL,

                        amount NUMERIC(12,2) NOT NULL,

                        refund_mode VARCHAR(30),

                        status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',

                        reason TEXT NOT NULL,

                        transaction_reference VARCHAR(255),

                        refunded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        refunded_by UUID NOT NULL,

                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                        CONSTRAINT fk_refund_clinic
                            FOREIGN KEY (clinic_id)
                                REFERENCES clinic(id),

                        CONSTRAINT fk_refund_payment
                            FOREIGN KEY (payment_id)
                                REFERENCES payment(id),

                        CONSTRAINT fk_refund_patient
                            FOREIGN KEY (patient_id)
                                REFERENCES patient(id),

                        CONSTRAINT fk_refund_refunded_by
                            FOREIGN KEY (refunded_by)
                                REFERENCES app_user(id),

                        CONSTRAINT uq_refund_number
                            UNIQUE (clinic_id, refund_number),

                        CONSTRAINT chk_refund_amount
                            CHECK (amount > 0),

                        CONSTRAINT chk_refund_mode
                            CHECK (
                                refund_mode IS NULL
                                    OR refund_mode IN (
                                                       'CASH',
                                                       'UPI',
                                                       'CARD',
                                                       'BANK_TRANSFER',
                                                       'CHEQUE',
                                                       'ONLINE',
                                                       'OTHER'
                                    )
                                ),

                        CONSTRAINT chk_refund_status
                            CHECK (status IN (
                                              'PENDING',
                                              'COMPLETED',
                                              'FAILED',
                                              'CANCELLED'
                                ))
);


CREATE INDEX idx_refund_payment
    ON refund (payment_id);

CREATE INDEX idx_refund_patient
    ON refund (
               patient_id,
               refunded_at DESC
        );

CREATE INDEX idx_refund_clinic
    ON refund (
               clinic_id,
               refunded_at DESC
        );
-- =========================================================
-- DENTAL CLINIC APP
-- V13 - EXPENSES & VENDOR FINANCE
-- =========================================================


-- =========================================================
-- 1. EXPENSE CATEGORY
-- =========================================================

CREATE TABLE expense_category (
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

                                  CONSTRAINT fk_expense_category_clinic
                                      FOREIGN KEY (clinic_id)
                                          REFERENCES clinic(id),

                                  CONSTRAINT fk_expense_category_created_by
                                      FOREIGN KEY (created_by)
                                          REFERENCES app_user(id),

                                  CONSTRAINT fk_expense_category_updated_by
                                      FOREIGN KEY (updated_by)
                                          REFERENCES app_user(id),

                                  CONSTRAINT uq_expense_category_code
                                      UNIQUE (clinic_id, category_code),

                                  CONSTRAINT uq_expense_category_name
                                      UNIQUE (clinic_id, category_name)
);


CREATE INDEX idx_expense_category_clinic
    ON expense_category (clinic_id);

CREATE INDEX idx_expense_category_active
    ON expense_category (
                         clinic_id,
                         is_active
        );


-- =========================================================
-- 2. EXPENSE
--
-- General clinic operational expenses.
--
-- Examples:
-- Rent
-- Electricity
-- Salary
-- Internet
-- Marketing
-- Repairs
-- Travel
-- Miscellaneous
--
-- Procurement/vendor invoices should normally use
-- vendor_invoice rather than being duplicated here.
-- =========================================================

CREATE TABLE expense (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                         clinic_id UUID NOT NULL,
                         category_id UUID NOT NULL,

                         expense_number VARCHAR(50) NOT NULL,

                         expense_date DATE NOT NULL DEFAULT CURRENT_DATE,

                         description VARCHAR(500) NOT NULL,

                         vendor_id UUID,

                         amount NUMERIC(12,2) NOT NULL,

                         tax_amount NUMERIC(12,2) NOT NULL DEFAULT 0,

                         total_amount NUMERIC(12,2) NOT NULL,

                         payment_mode VARCHAR(30),

                         payment_status VARCHAR(30) NOT NULL DEFAULT 'PAID',

                         transaction_reference VARCHAR(255),

                         paid_at TIMESTAMP,

                         notes TEXT,

                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                         created_by UUID,
                         updated_by UUID,

                         CONSTRAINT fk_expense_clinic
                             FOREIGN KEY (clinic_id)
                                 REFERENCES clinic(id),

                         CONSTRAINT fk_expense_category
                             FOREIGN KEY (category_id)
                                 REFERENCES expense_category(id),

                         CONSTRAINT fk_expense_vendor
                             FOREIGN KEY (vendor_id)
                                 REFERENCES vendor(id),

                         CONSTRAINT fk_expense_created_by
                             FOREIGN KEY (created_by)
                                 REFERENCES app_user(id),

                         CONSTRAINT fk_expense_updated_by
                             FOREIGN KEY (updated_by)
                                 REFERENCES app_user(id),

                         CONSTRAINT uq_expense_number
                             UNIQUE (
                                     clinic_id,
                                     expense_number
                                 ),

                         CONSTRAINT chk_expense_amount
                             CHECK (amount >= 0),

                         CONSTRAINT chk_expense_tax
                             CHECK (tax_amount >= 0),

                         CONSTRAINT chk_expense_total
                             CHECK (total_amount >= 0),

                         CONSTRAINT chk_expense_payment_mode
                             CHECK (
                                 payment_mode IS NULL
                                     OR payment_mode IN (
                                                         'CASH',
                                                         'UPI',
                                                         'CARD',
                                                         'BANK_TRANSFER',
                                                         'CHEQUE',
                                                         'ONLINE',
                                                         'OTHER'
                                     )
                                 ),

                         CONSTRAINT chk_expense_payment_status
                             CHECK (
                                 payment_status IN (
                                                    'PENDING',
                                                    'PAID',
                                                    'CANCELLED'
                                     )
                                 )
);


CREATE INDEX idx_expense_clinic
    ON expense (
                clinic_id,
                expense_date DESC
        );

CREATE INDEX idx_expense_category
    ON expense (
                category_id,
                expense_date DESC
        );

CREATE INDEX idx_expense_vendor
    ON expense (
                vendor_id,
                expense_date DESC
        );

CREATE INDEX idx_expense_payment_status
    ON expense (
                clinic_id,
                payment_status
        );


-- =========================================================
-- 3. EXPENSE ATTACHMENT
--
-- Supporting documents:
-- bill, invoice, receipt, voucher etc.
--
-- Files themselves will live in S3.
-- =========================================================

CREATE TABLE expense_attachment (
                                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                    clinic_id UUID NOT NULL,
                                    expense_id UUID NOT NULL,

                                    attachment_type VARCHAR(30) NOT NULL DEFAULT 'OTHER',

                                    file_name VARCHAR(255) NOT NULL,
                                    file_url TEXT NOT NULL,

                                    mime_type VARCHAR(150),
                                    file_size BIGINT,

                                    description TEXT,

                                    uploaded_by UUID,

                                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                    CONSTRAINT fk_expense_attachment_clinic
                                        FOREIGN KEY (clinic_id)
                                            REFERENCES clinic(id),

                                    CONSTRAINT fk_expense_attachment_expense
                                        FOREIGN KEY (expense_id)
                                            REFERENCES expense(id)
                                            ON DELETE CASCADE,

                                    CONSTRAINT fk_expense_attachment_uploaded_by
                                        FOREIGN KEY (uploaded_by)
                                            REFERENCES app_user(id),

                                    CONSTRAINT chk_expense_attachment_type
                                        CHECK (
                                            attachment_type IN (
                                                                'BILL',
                                                                'INVOICE',
                                                                'RECEIPT',
                                                                'VOUCHER',
                                                                'OTHER'
                                                )
                                            ),

                                    CONSTRAINT chk_expense_attachment_size
                                        CHECK (
                                            file_size IS NULL
                                                OR file_size >= 0
                                            )
);


CREATE INDEX idx_expense_attachment_expense
    ON expense_attachment (expense_id);


-- =========================================================
-- 4. VENDOR INVOICE
--
-- Invoice/bill received from a vendor.
--
-- Example:
--
-- Vendor sends invoice:
-- INV/DENTAL/2026/450
--
-- PO = PO-2026-0001
-- Amount = Rs. 50,000
--
-- We maintain paid/balance amounts for fast reporting.
-- =========================================================

CREATE TABLE vendor_invoice (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                clinic_id UUID NOT NULL,
                                vendor_id UUID NOT NULL,

                                purchase_order_id UUID,
                                goods_receipt_id UUID,

                                vendor_invoice_number VARCHAR(100) NOT NULL,

                                invoice_date DATE NOT NULL,
                                due_date DATE,

                                subtotal NUMERIC(12,2) NOT NULL DEFAULT 0,
                                discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
                                tax_amount NUMERIC(12,2) NOT NULL DEFAULT 0,

                                total_amount NUMERIC(12,2) NOT NULL,

                                paid_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
                                balance_amount NUMERIC(12,2) NOT NULL,

                                status VARCHAR(30) NOT NULL DEFAULT 'UNPAID',

                                invoice_document_url TEXT,

                                notes TEXT,

                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                created_by UUID,
                                updated_by UUID,

                                CONSTRAINT fk_vendor_invoice_clinic
                                    FOREIGN KEY (clinic_id)
                                        REFERENCES clinic(id),

                                CONSTRAINT fk_vendor_invoice_vendor
                                    FOREIGN KEY (vendor_id)
                                        REFERENCES vendor(id),

                                CONSTRAINT fk_vendor_invoice_purchase_order
                                    FOREIGN KEY (purchase_order_id)
                                        REFERENCES purchase_order(id),

                                CONSTRAINT fk_vendor_invoice_goods_receipt
                                    FOREIGN KEY (goods_receipt_id)
                                        REFERENCES goods_receipt(id),

                                CONSTRAINT fk_vendor_invoice_created_by
                                    FOREIGN KEY (created_by)
                                        REFERENCES app_user(id),

                                CONSTRAINT fk_vendor_invoice_updated_by
                                    FOREIGN KEY (updated_by)
                                        REFERENCES app_user(id),

                                CONSTRAINT uq_vendor_invoice
                                    UNIQUE (
                                            clinic_id,
                                            vendor_id,
                                            vendor_invoice_number
                                        ),

                                CONSTRAINT chk_vendor_invoice_subtotal
                                    CHECK (subtotal >= 0),

                                CONSTRAINT chk_vendor_invoice_discount
                                    CHECK (discount_amount >= 0),

                                CONSTRAINT chk_vendor_invoice_tax
                                    CHECK (tax_amount >= 0),

                                CONSTRAINT chk_vendor_invoice_total
                                    CHECK (total_amount >= 0),

                                CONSTRAINT chk_vendor_invoice_paid
                                    CHECK (paid_amount >= 0),

                                CONSTRAINT chk_vendor_invoice_balance
                                    CHECK (balance_amount >= 0),

                                CONSTRAINT chk_vendor_invoice_paid_total
                                    CHECK (paid_amount <= total_amount),

                                CONSTRAINT chk_vendor_invoice_balance_total
                                    CHECK (balance_amount <= total_amount),

                                CONSTRAINT chk_vendor_invoice_amount_consistency
                                    CHECK (
                                        paid_amount + balance_amount = total_amount
                                        ),

                                CONSTRAINT chk_vendor_invoice_due_date
                                    CHECK (
                                        due_date IS NULL
                                            OR due_date >= invoice_date
                                        ),

                                CONSTRAINT chk_vendor_invoice_status
                                    CHECK (
                                        status IN (
                                                   'UNPAID',
                                                   'PARTIALLY_PAID',
                                                   'PAID',
                                                   'CANCELLED'
                                            )
                                        )
);


CREATE INDEX idx_vendor_invoice_clinic
    ON vendor_invoice (
                       clinic_id,
                       invoice_date DESC
        );

CREATE INDEX idx_vendor_invoice_vendor
    ON vendor_invoice (
                       vendor_id,
                       invoice_date DESC
        );

CREATE INDEX idx_vendor_invoice_purchase_order
    ON vendor_invoice (purchase_order_id);

CREATE INDEX idx_vendor_invoice_status
    ON vendor_invoice (
                       clinic_id,
                       status
        );

CREATE INDEX idx_vendor_invoice_due
    ON vendor_invoice (
                       clinic_id,
                       due_date
        )
    WHERE balance_amount > 0;


-- =========================================================
-- 5. VENDOR PAYMENT
--
-- Actual payment made TO a vendor.
--
-- One payment can later be allocated across multiple
-- vendor invoices.
-- =========================================================

CREATE TABLE vendor_payment (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                clinic_id UUID NOT NULL,
                                vendor_id UUID NOT NULL,

                                vendor_payment_number VARCHAR(50) NOT NULL,

                                payment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                amount NUMERIC(12,2) NOT NULL,

                                payment_mode VARCHAR(30) NOT NULL,

                                status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',

                                transaction_reference VARCHAR(255),

                                notes TEXT,

                                paid_by UUID NOT NULL,

                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                created_by UUID,
                                updated_by UUID,

                                CONSTRAINT fk_vendor_payment_clinic
                                    FOREIGN KEY (clinic_id)
                                        REFERENCES clinic(id),

                                CONSTRAINT fk_vendor_payment_vendor
                                    FOREIGN KEY (vendor_id)
                                        REFERENCES vendor(id),

                                CONSTRAINT fk_vendor_payment_paid_by
                                    FOREIGN KEY (paid_by)
                                        REFERENCES app_user(id),

                                CONSTRAINT fk_vendor_payment_created_by
                                    FOREIGN KEY (created_by)
                                        REFERENCES app_user(id),

                                CONSTRAINT fk_vendor_payment_updated_by
                                    FOREIGN KEY (updated_by)
                                        REFERENCES app_user(id),

                                CONSTRAINT uq_vendor_payment_number
                                    UNIQUE (
                                            clinic_id,
                                            vendor_payment_number
                                        ),

                                CONSTRAINT chk_vendor_payment_amount
                                    CHECK (amount > 0),

                                CONSTRAINT chk_vendor_payment_mode
                                    CHECK (
                                        payment_mode IN (
                                                         'CASH',
                                                         'UPI',
                                                         'CARD',
                                                         'BANK_TRANSFER',
                                                         'CHEQUE',
                                                         'ONLINE',
                                                         'OTHER'
                                            )
                                        ),

                                CONSTRAINT chk_vendor_payment_status
                                    CHECK (
                                        status IN (
                                                   'PENDING',
                                                   'COMPLETED',
                                                   'FAILED',
                                                   'CANCELLED'
                                            )
                                        )
);


CREATE INDEX idx_vendor_payment_clinic
    ON vendor_payment (
                       clinic_id,
                       payment_date DESC
        );

CREATE INDEX idx_vendor_payment_vendor
    ON vendor_payment (
                       vendor_id,
                       payment_date DESC
        );

CREATE INDEX idx_vendor_payment_status
    ON vendor_payment (
                       clinic_id,
                       status
        );

CREATE INDEX idx_vendor_payment_reference
    ON vendor_payment (
                       clinic_id,
                       transaction_reference
        )
    WHERE transaction_reference IS NOT NULL;


-- =========================================================
-- 6. VENDOR PAYMENT ALLOCATION
--
-- Links vendor payments to vendor invoices.
--
-- Supports:
--
-- One invoice -> multiple payments
-- One payment -> multiple invoices
--
-- Example:
--
-- Vendor payment = Rs. 50,000
--
-- Invoice A -> Rs. 30,000
-- Invoice B -> Rs. 20,000
-- =========================================================

CREATE TABLE vendor_payment_allocation (
                                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                           clinic_id UUID NOT NULL,

                                           vendor_payment_id UUID NOT NULL,
                                           vendor_invoice_id UUID NOT NULL,

                                           allocated_amount NUMERIC(12,2) NOT NULL,

                                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                           created_by UUID,

                                           CONSTRAINT fk_vendor_payment_allocation_clinic
                                               FOREIGN KEY (clinic_id)
                                                   REFERENCES clinic(id),

                                           CONSTRAINT fk_vendor_payment_allocation_payment
                                               FOREIGN KEY (vendor_payment_id)
                                                   REFERENCES vendor_payment(id),

                                           CONSTRAINT fk_vendor_payment_allocation_invoice
                                               FOREIGN KEY (vendor_invoice_id)
                                                   REFERENCES vendor_invoice(id),

                                           CONSTRAINT fk_vendor_payment_allocation_created_by
                                               FOREIGN KEY (created_by)
                                                   REFERENCES app_user(id),

                                           CONSTRAINT uq_vendor_payment_invoice_allocation
                                               UNIQUE (
                                                       vendor_payment_id,
                                                       vendor_invoice_id
                                                   ),

                                           CONSTRAINT chk_vendor_payment_allocation_amount
                                               CHECK (allocated_amount > 0)
);


CREATE INDEX idx_vendor_payment_allocation_payment
    ON vendor_payment_allocation (vendor_payment_id);

CREATE INDEX idx_vendor_payment_allocation_invoice
    ON vendor_payment_allocation (vendor_invoice_id);
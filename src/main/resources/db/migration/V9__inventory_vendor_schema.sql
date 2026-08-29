-- =========================================================
-- DENTAL CLINIC APP
-- V9 - VENDOR, INVENTORY, PROCUREMENT & MATERIAL USAGE
-- =========================================================


-- =========================================================
-- 1. VENDOR
-- =========================================================

CREATE TABLE vendor (
                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                        clinic_id UUID NOT NULL,

                        vendor_code VARCHAR(50) NOT NULL,
                        vendor_name VARCHAR(200) NOT NULL,

                        contact_person VARCHAR(150),
                        phone VARCHAR(20),
                        email VARCHAR(150),

                        gst_number VARCHAR(30),

                        address_line1 VARCHAR(255),
                        address_line2 VARCHAR(255),
                        city VARCHAR(100),
                        state VARCHAR(100),
                        country VARCHAR(100) DEFAULT 'India',
                        pincode VARCHAR(20),

                        payment_terms_days INTEGER,

                        notes TEXT,

                        is_active BOOLEAN NOT NULL DEFAULT TRUE,

                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                        created_by UUID,
                        updated_by UUID,

                        CONSTRAINT fk_vendor_clinic
                            FOREIGN KEY (clinic_id)
                                REFERENCES clinic(id),

                        CONSTRAINT fk_vendor_created_by
                            FOREIGN KEY (created_by)
                                REFERENCES app_user(id),

                        CONSTRAINT fk_vendor_updated_by
                            FOREIGN KEY (updated_by)
                                REFERENCES app_user(id),

                        CONSTRAINT uq_vendor_code
                            UNIQUE (clinic_id, vendor_code),

                        CONSTRAINT chk_vendor_payment_terms
                            CHECK (
                                payment_terms_days IS NULL
                                    OR payment_terms_days >= 0
                                )
);


CREATE INDEX idx_vendor_clinic
    ON vendor (clinic_id);

CREATE INDEX idx_vendor_active
    ON vendor (clinic_id, is_active);

CREATE INDEX idx_vendor_name
    ON vendor (clinic_id, LOWER(vendor_name));


-- =========================================================
-- 2. INVENTORY CATEGORY
-- =========================================================

CREATE TABLE inventory_category (
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

                                    CONSTRAINT fk_inventory_category_clinic
                                        FOREIGN KEY (clinic_id)
                                            REFERENCES clinic(id),

                                    CONSTRAINT fk_inventory_category_created_by
                                        FOREIGN KEY (created_by)
                                            REFERENCES app_user(id),

                                    CONSTRAINT fk_inventory_category_updated_by
                                        FOREIGN KEY (updated_by)
                                            REFERENCES app_user(id),

                                    CONSTRAINT uq_inventory_category_code
                                        UNIQUE (clinic_id, category_code),

                                    CONSTRAINT uq_inventory_category_name
                                        UNIQUE (clinic_id, category_name)
);


CREATE INDEX idx_inventory_category_clinic
    ON inventory_category (clinic_id);

CREATE INDEX idx_inventory_category_active
    ON inventory_category (clinic_id, is_active);


-- =========================================================
-- 3. INVENTORY ITEM
--
-- Master definition of a material/product.
-- Actual physical stock is maintained batch-wise.
-- =========================================================

CREATE TABLE inventory_item (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                clinic_id UUID NOT NULL,
                                category_id UUID,

                                item_code VARCHAR(50) NOT NULL,
                                item_name VARCHAR(200) NOT NULL,

                                description TEXT,

                                unit_of_measure VARCHAR(30) NOT NULL DEFAULT 'PCS',

                                track_batch BOOLEAN NOT NULL DEFAULT TRUE,
                                track_expiry BOOLEAN NOT NULL DEFAULT FALSE,

                                minimum_stock_level NUMERIC(12,3) NOT NULL DEFAULT 0,
                                reorder_level NUMERIC(12,3) NOT NULL DEFAULT 0,

                                default_purchase_price NUMERIC(12,2),

                                is_active BOOLEAN NOT NULL DEFAULT TRUE,

                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                created_by UUID,
                                updated_by UUID,

                                CONSTRAINT fk_inventory_item_clinic
                                    FOREIGN KEY (clinic_id)
                                        REFERENCES clinic(id),

                                CONSTRAINT fk_inventory_item_category
                                    FOREIGN KEY (category_id)
                                        REFERENCES inventory_category(id),

                                CONSTRAINT fk_inventory_item_created_by
                                    FOREIGN KEY (created_by)
                                        REFERENCES app_user(id),

                                CONSTRAINT fk_inventory_item_updated_by
                                    FOREIGN KEY (updated_by)
                                        REFERENCES app_user(id),

                                CONSTRAINT uq_inventory_item_code
                                    UNIQUE (clinic_id, item_code),

                                CONSTRAINT chk_inventory_minimum_stock
                                    CHECK (minimum_stock_level >= 0),

                                CONSTRAINT chk_inventory_reorder_level
                                    CHECK (reorder_level >= 0),

                                CONSTRAINT chk_inventory_default_price
                                    CHECK (
                                        default_purchase_price IS NULL
                                            OR default_purchase_price >= 0
                                        )
);


CREATE INDEX idx_inventory_item_clinic
    ON inventory_item (clinic_id);

CREATE INDEX idx_inventory_item_category
    ON inventory_item (category_id);

CREATE INDEX idx_inventory_item_active
    ON inventory_item (clinic_id, is_active);

CREATE INDEX idx_inventory_item_name
    ON inventory_item (clinic_id, LOWER(item_name));


-- =========================================================
-- 4. PURCHASE ORDER
-- =========================================================

CREATE TABLE purchase_order (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                clinic_id UUID NOT NULL,
                                vendor_id UUID NOT NULL,

                                purchase_order_number VARCHAR(50) NOT NULL,

                                order_date DATE NOT NULL DEFAULT CURRENT_DATE,
                                expected_delivery_date DATE,

                                status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',

                                subtotal NUMERIC(12,2) NOT NULL DEFAULT 0,
                                discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
                                tax_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
                                total_amount NUMERIC(12,2) NOT NULL DEFAULT 0,

                                notes TEXT,

                                placed_at TIMESTAMP,
                                confirmed_at TIMESTAMP,
                                cancelled_at TIMESTAMP,
                                cancellation_reason TEXT,

                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                created_by UUID,
                                updated_by UUID,

                                CONSTRAINT fk_purchase_order_clinic
                                    FOREIGN KEY (clinic_id)
                                        REFERENCES clinic(id),

                                CONSTRAINT fk_purchase_order_vendor
                                    FOREIGN KEY (vendor_id)
                                        REFERENCES vendor(id),

                                CONSTRAINT fk_purchase_order_created_by
                                    FOREIGN KEY (created_by)
                                        REFERENCES app_user(id),

                                CONSTRAINT fk_purchase_order_updated_by
                                    FOREIGN KEY (updated_by)
                                        REFERENCES app_user(id),

                                CONSTRAINT uq_purchase_order_number
                                    UNIQUE (clinic_id, purchase_order_number),

                                CONSTRAINT chk_purchase_order_status
                                    CHECK (status IN (
                                                      'DRAFT',
                                                      'PLACED',
                                                      'CONFIRMED',
                                                      'PARTIALLY_RECEIVED',
                                                      'RECEIVED',
                                                      'CANCELLED'
                                        )),

                                CONSTRAINT chk_purchase_order_subtotal
                                    CHECK (subtotal >= 0),

                                CONSTRAINT chk_purchase_order_discount
                                    CHECK (discount_amount >= 0),

                                CONSTRAINT chk_purchase_order_tax
                                    CHECK (tax_amount >= 0),

                                CONSTRAINT chk_purchase_order_total
                                    CHECK (total_amount >= 0),

                                CONSTRAINT chk_purchase_order_delivery_date
                                    CHECK (
                                        expected_delivery_date IS NULL
                                            OR expected_delivery_date >= order_date
                                        )
);


CREATE INDEX idx_purchase_order_clinic
    ON purchase_order (clinic_id);

CREATE INDEX idx_purchase_order_vendor
    ON purchase_order (vendor_id);

CREATE INDEX idx_purchase_order_status
    ON purchase_order (clinic_id, status);

CREATE INDEX idx_purchase_order_date
    ON purchase_order (clinic_id, order_date DESC);


-- =========================================================
-- 5. PURCHASE ORDER ITEM
-- =========================================================

CREATE TABLE purchase_order_item (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                     clinic_id UUID NOT NULL,

                                     purchase_order_id UUID NOT NULL,
                                     inventory_item_id UUID NOT NULL,

                                     quantity_ordered NUMERIC(12,3) NOT NULL,
                                     quantity_received NUMERIC(12,3) NOT NULL DEFAULT 0,

                                     unit_price NUMERIC(12,2) NOT NULL DEFAULT 0,

                                     gross_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
                                     discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
                                     tax_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
                                     final_amount NUMERIC(12,2) NOT NULL DEFAULT 0,

                                     notes TEXT,

                                     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                     CONSTRAINT fk_purchase_order_item_clinic
                                         FOREIGN KEY (clinic_id)
                                             REFERENCES clinic(id),

                                     CONSTRAINT fk_purchase_order_item_order
                                         FOREIGN KEY (purchase_order_id)
                                             REFERENCES purchase_order(id)
                                             ON DELETE CASCADE,

                                     CONSTRAINT fk_purchase_order_item_inventory
                                         FOREIGN KEY (inventory_item_id)
                                             REFERENCES inventory_item(id),

                                     CONSTRAINT uq_purchase_order_inventory_item
                                         UNIQUE (purchase_order_id, inventory_item_id),

                                     CONSTRAINT chk_po_item_ordered
                                         CHECK (quantity_ordered > 0),

                                     CONSTRAINT chk_po_item_received
                                         CHECK (
                                             quantity_received >= 0
                                                 AND quantity_received <= quantity_ordered
                                             ),

                                     CONSTRAINT chk_po_item_unit_price
                                         CHECK (unit_price >= 0),

                                     CONSTRAINT chk_po_item_gross
                                         CHECK (gross_amount >= 0),

                                     CONSTRAINT chk_po_item_discount
                                         CHECK (discount_amount >= 0),

                                     CONSTRAINT chk_po_item_tax
                                         CHECK (tax_amount >= 0),

                                     CONSTRAINT chk_po_item_final
                                         CHECK (final_amount >= 0)
);


CREATE INDEX idx_purchase_order_item_order
    ON purchase_order_item (purchase_order_id);

CREATE INDEX idx_purchase_order_item_inventory
    ON purchase_order_item (inventory_item_id);


-- =========================================================
-- 6. GOODS RECEIPT
--
-- One PO may be received through multiple deliveries.
-- =========================================================

CREATE TABLE goods_receipt (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                               clinic_id UUID NOT NULL,

                               purchase_order_id UUID NOT NULL,
                               vendor_id UUID NOT NULL,

                               goods_receipt_number VARCHAR(50) NOT NULL,

                               received_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               delivery_reference VARCHAR(100),
                               invoice_reference VARCHAR(100),

                               status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',

                               received_by UUID NOT NULL,

                               notes TEXT,

                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               created_by UUID,
                               updated_by UUID,

                               CONSTRAINT fk_goods_receipt_clinic
                                   FOREIGN KEY (clinic_id)
                                       REFERENCES clinic(id),

                               CONSTRAINT fk_goods_receipt_purchase_order
                                   FOREIGN KEY (purchase_order_id)
                                       REFERENCES purchase_order(id),

                               CONSTRAINT fk_goods_receipt_vendor
                                   FOREIGN KEY (vendor_id)
                                       REFERENCES vendor(id),

                               CONSTRAINT fk_goods_receipt_received_by
                                   FOREIGN KEY (received_by)
                                       REFERENCES app_user(id),

                               CONSTRAINT fk_goods_receipt_created_by
                                   FOREIGN KEY (created_by)
                                       REFERENCES app_user(id),

                               CONSTRAINT fk_goods_receipt_updated_by
                                   FOREIGN KEY (updated_by)
                                       REFERENCES app_user(id),

                               CONSTRAINT uq_goods_receipt_number
                                   UNIQUE (clinic_id, goods_receipt_number),

                               CONSTRAINT chk_goods_receipt_status
                                   CHECK (status IN (
                                                     'DRAFT',
                                                     'RECEIVED',
                                                     'REJECTED',
                                                     'PARTIALLY_ACCEPTED'
                                       ))
);


CREATE INDEX idx_goods_receipt_clinic
    ON goods_receipt (clinic_id);

CREATE INDEX idx_goods_receipt_purchase_order
    ON goods_receipt (purchase_order_id);

CREATE INDEX idx_goods_receipt_vendor
    ON goods_receipt (vendor_id);

CREATE INDEX idx_goods_receipt_date
    ON goods_receipt (clinic_id, received_date DESC);


-- =========================================================
-- 7. GOODS RECEIPT ITEM
-- =========================================================

CREATE TABLE goods_receipt_item (
                                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                    clinic_id UUID NOT NULL,

                                    goods_receipt_id UUID NOT NULL,
                                    purchase_order_item_id UUID NOT NULL,
                                    inventory_item_id UUID NOT NULL,

                                    quantity_received NUMERIC(12,3) NOT NULL,
                                    quantity_accepted NUMERIC(12,3) NOT NULL DEFAULT 0,
                                    quantity_rejected NUMERIC(12,3) NOT NULL DEFAULT 0,

                                    batch_number VARCHAR(100),
                                    expiry_date DATE,

                                    purchase_price NUMERIC(12,2) NOT NULL DEFAULT 0,

                                    rejection_reason TEXT,

                                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                    CONSTRAINT fk_goods_receipt_item_clinic
                                        FOREIGN KEY (clinic_id)
                                            REFERENCES clinic(id),

                                    CONSTRAINT fk_goods_receipt_item_receipt
                                        FOREIGN KEY (goods_receipt_id)
                                            REFERENCES goods_receipt(id)
                                            ON DELETE CASCADE,

                                    CONSTRAINT fk_goods_receipt_item_po_item
                                        FOREIGN KEY (purchase_order_item_id)
                                            REFERENCES purchase_order_item(id),

                                    CONSTRAINT fk_goods_receipt_item_inventory
                                        FOREIGN KEY (inventory_item_id)
                                            REFERENCES inventory_item(id),

                                    CONSTRAINT chk_goods_receipt_item_received
                                        CHECK (quantity_received > 0),

                                    CONSTRAINT chk_goods_receipt_item_accepted
                                        CHECK (quantity_accepted >= 0),

                                    CONSTRAINT chk_goods_receipt_item_rejected
                                        CHECK (quantity_rejected >= 0),

                                    CONSTRAINT chk_goods_receipt_item_quantity_total
                                        CHECK (
                                            quantity_accepted + quantity_rejected
                                                <= quantity_received
                                            ),

                                    CONSTRAINT chk_goods_receipt_item_price
                                        CHECK (purchase_price >= 0)
);


CREATE INDEX idx_goods_receipt_item_receipt
    ON goods_receipt_item (goods_receipt_id);

CREATE INDEX idx_goods_receipt_item_inventory
    ON goods_receipt_item (inventory_item_id);


-- =========================================================
-- 8. INVENTORY BATCH
--
-- Physical usable stock.
-- =========================================================

CREATE TABLE inventory_batch (
                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                 clinic_id UUID NOT NULL,

                                 inventory_item_id UUID NOT NULL,
                                 goods_receipt_item_id UUID,

                                 batch_number VARCHAR(100),
                                 expiry_date DATE,

                                 purchase_price NUMERIC(12,2) NOT NULL DEFAULT 0,

                                 quantity_received NUMERIC(12,3) NOT NULL,
                                 current_quantity NUMERIC(12,3) NOT NULL,

                                 status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                 CONSTRAINT fk_inventory_batch_clinic
                                     FOREIGN KEY (clinic_id)
                                         REFERENCES clinic(id),

                                 CONSTRAINT fk_inventory_batch_item
                                     FOREIGN KEY (inventory_item_id)
                                         REFERENCES inventory_item(id),

                                 CONSTRAINT fk_inventory_batch_goods_receipt
                                     FOREIGN KEY (goods_receipt_item_id)
                                         REFERENCES goods_receipt_item(id),

                                 CONSTRAINT chk_inventory_batch_price
                                     CHECK (purchase_price >= 0),

                                 CONSTRAINT chk_inventory_batch_received
                                     CHECK (quantity_received > 0),

                                 CONSTRAINT chk_inventory_batch_current
                                     CHECK (
                                         current_quantity >= 0
                                             AND current_quantity <= quantity_received
                                         ),

                                 CONSTRAINT chk_inventory_batch_status
                                     CHECK (status IN (
                                                       'ACTIVE',
                                                       'EXPIRED',
                                                       'DEPLETED',
                                                       'QUARANTINED',
                                                       'DISPOSED'
                                         ))
);


CREATE INDEX idx_inventory_batch_item
    ON inventory_batch (inventory_item_id);

CREATE INDEX idx_inventory_batch_status
    ON inventory_batch (
                        clinic_id,
                        status
        );

CREATE INDEX idx_inventory_batch_expiry
    ON inventory_batch (
                        clinic_id,
                        expiry_date
        )
    WHERE expiry_date IS NOT NULL;


-- Batch number only needs to be unique for the same item
-- when a batch number exists.
CREATE UNIQUE INDEX uq_inventory_item_batch_number
    ON inventory_batch (
                        inventory_item_id,
                        batch_number
        )
    WHERE batch_number IS NOT NULL;


-- =========================================================
-- 9. STOCK TRANSACTION
--
-- Append-only inventory ledger.
--
-- Positive quantity = stock IN
-- Negative quantity = stock OUT
-- =========================================================

CREATE TABLE stock_transaction (
                                   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                   clinic_id UUID NOT NULL,

                                   inventory_item_id UUID NOT NULL,
                                   inventory_batch_id UUID,

                                   transaction_type VARCHAR(30) NOT NULL,

                                   quantity NUMERIC(12,3) NOT NULL,

                                   reference_type VARCHAR(50),
                                   reference_id UUID,

                                   notes TEXT,

                                   performed_by UUID NOT NULL,

                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                   CONSTRAINT fk_stock_transaction_clinic
                                       FOREIGN KEY (clinic_id)
                                           REFERENCES clinic(id),

                                   CONSTRAINT fk_stock_transaction_item
                                       FOREIGN KEY (inventory_item_id)
                                           REFERENCES inventory_item(id),

                                   CONSTRAINT fk_stock_transaction_batch
                                       FOREIGN KEY (inventory_batch_id)
                                           REFERENCES inventory_batch(id),

                                   CONSTRAINT fk_stock_transaction_performed_by
                                       FOREIGN KEY (performed_by)
                                           REFERENCES app_user(id),

                                   CONSTRAINT chk_stock_transaction_type
                                       CHECK (transaction_type IN (
                                                                   'PURCHASE',
                                                                   'PATIENT_USAGE',
                                                                   'WASTE',
                                                                   'ADJUSTMENT_IN',
                                                                   'ADJUSTMENT_OUT',
                                                                   'RETURN_TO_VENDOR',
                                                                   'EXPIRED',
                                                                   'DISPOSAL'
                                           )),

                                   CONSTRAINT chk_stock_transaction_quantity
                                       CHECK (quantity <> 0)
);


CREATE INDEX idx_stock_transaction_item
    ON stock_transaction (
                          inventory_item_id,
                          created_at DESC
        );

CREATE INDEX idx_stock_transaction_batch
    ON stock_transaction (
                          inventory_batch_id,
                          created_at DESC
        );

CREATE INDEX idx_stock_transaction_clinic
    ON stock_transaction (
                          clinic_id,
                          created_at DESC
        );

CREATE INDEX idx_stock_transaction_reference
    ON stock_transaction (
                          reference_type,
                          reference_id
        );


-- =========================================================
-- 10. PROCEDURE MATERIAL
--
-- Default/expected material consumption for a procedure.
-- =========================================================

CREATE TABLE procedure_material (
                                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                    clinic_id UUID NOT NULL,

                                    procedure_id UUID NOT NULL,
                                    inventory_item_id UUID NOT NULL,

                                    default_quantity NUMERIC(12,3) NOT NULL,

                                    is_required BOOLEAN NOT NULL DEFAULT TRUE,

                                    notes TEXT,

                                    is_active BOOLEAN NOT NULL DEFAULT TRUE,

                                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                    created_by UUID,
                                    updated_by UUID,

                                    CONSTRAINT fk_procedure_material_clinic
                                        FOREIGN KEY (clinic_id)
                                            REFERENCES clinic(id),

                                    CONSTRAINT fk_procedure_material_procedure
                                        FOREIGN KEY (procedure_id)
                                            REFERENCES procedure_master(id)
                                            ON DELETE CASCADE,

                                    CONSTRAINT fk_procedure_material_inventory
                                        FOREIGN KEY (inventory_item_id)
                                            REFERENCES inventory_item(id),

                                    CONSTRAINT fk_procedure_material_created_by
                                        FOREIGN KEY (created_by)
                                            REFERENCES app_user(id),

                                    CONSTRAINT fk_procedure_material_updated_by
                                        FOREIGN KEY (updated_by)
                                            REFERENCES app_user(id),

                                    CONSTRAINT uq_procedure_material
                                        UNIQUE (procedure_id, inventory_item_id),

                                    CONSTRAINT chk_procedure_material_quantity
                                        CHECK (default_quantity > 0)
);


CREATE INDEX idx_procedure_material_procedure
    ON procedure_material (procedure_id);

CREATE INDEX idx_procedure_material_inventory
    ON procedure_material (inventory_item_id);


-- =========================================================
-- 11. PATIENT MATERIAL USAGE
--
-- Actual materials consumed during patient treatment.
-- =========================================================

CREATE TABLE patient_material_usage (
                                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                        clinic_id UUID NOT NULL,

                                        patient_id UUID NOT NULL,
                                        appointment_id UUID,
                                        case_sheet_id UUID,

                                        treatment_plan_item_id UUID,
                                        procedure_id UUID,

                                        inventory_item_id UUID NOT NULL,
                                        inventory_batch_id UUID NOT NULL,

                                        tooth_number VARCHAR(10),

                                        quantity_used NUMERIC(12,3) NOT NULL,

                                        used_by UUID NOT NULL,
                                        used_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                        notes TEXT,

                                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                        CONSTRAINT fk_patient_material_usage_clinic
                                            FOREIGN KEY (clinic_id)
                                                REFERENCES clinic(id),

                                        CONSTRAINT fk_patient_material_usage_patient
                                            FOREIGN KEY (patient_id)
                                                REFERENCES patient(id),

                                        CONSTRAINT fk_patient_material_usage_appointment
                                            FOREIGN KEY (appointment_id)
                                                REFERENCES appointment(id),

                                        CONSTRAINT fk_patient_material_usage_case_sheet
                                            FOREIGN KEY (case_sheet_id)
                                                REFERENCES case_sheet(id),

                                        CONSTRAINT fk_patient_material_usage_treatment_item
                                            FOREIGN KEY (treatment_plan_item_id)
                                                REFERENCES treatment_plan_item(id),

                                        CONSTRAINT fk_patient_material_usage_procedure
                                            FOREIGN KEY (procedure_id)
                                                REFERENCES procedure_master(id),

                                        CONSTRAINT fk_patient_material_usage_inventory
                                            FOREIGN KEY (inventory_item_id)
                                                REFERENCES inventory_item(id),

                                        CONSTRAINT fk_patient_material_usage_batch
                                            FOREIGN KEY (inventory_batch_id)
                                                REFERENCES inventory_batch(id),

                                        CONSTRAINT fk_patient_material_usage_used_by
                                            FOREIGN KEY (used_by)
                                                REFERENCES app_user(id),

                                        CONSTRAINT chk_patient_material_usage_quantity
                                            CHECK (quantity_used > 0)
);


CREATE INDEX idx_patient_material_usage_patient
    ON patient_material_usage (
                               patient_id,
                               used_at DESC
        );

CREATE INDEX idx_patient_material_usage_case_sheet
    ON patient_material_usage (case_sheet_id);

CREATE INDEX idx_patient_material_usage_treatment
    ON patient_material_usage (treatment_plan_item_id);

CREATE INDEX idx_patient_material_usage_inventory
    ON patient_material_usage (
                               inventory_item_id,
                               used_at DESC
        );

CREATE INDEX idx_patient_material_usage_batch
    ON patient_material_usage (inventory_batch_id);


-- =========================================================
-- 12. WASTE RECORD
--
-- Clinical / expired / damaged material waste.
-- =========================================================

CREATE TABLE waste_record (
                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                              clinic_id UUID NOT NULL,

                              inventory_item_id UUID NOT NULL,
                              inventory_batch_id UUID,

                              patient_id UUID,
                              appointment_id UUID,

                              waste_type VARCHAR(30) NOT NULL,

                              quantity NUMERIC(12,3) NOT NULL,

                              reason TEXT,

                              disposal_status VARCHAR(30) NOT NULL DEFAULT 'GENERATED',

                              waste_bag_reference VARCHAR(100),

                              generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              disposed_at TIMESTAMP,

                              generated_by UUID NOT NULL,
                              disposed_by UUID,

                              notes TEXT,

                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                              CONSTRAINT fk_waste_record_clinic
                                  FOREIGN KEY (clinic_id)
                                      REFERENCES clinic(id),

                              CONSTRAINT fk_waste_record_inventory
                                  FOREIGN KEY (inventory_item_id)
                                      REFERENCES inventory_item(id),

                              CONSTRAINT fk_waste_record_batch
                                  FOREIGN KEY (inventory_batch_id)
                                      REFERENCES inventory_batch(id),

                              CONSTRAINT fk_waste_record_patient
                                  FOREIGN KEY (patient_id)
                                      REFERENCES patient(id),

                              CONSTRAINT fk_waste_record_appointment
                                  FOREIGN KEY (appointment_id)
                                      REFERENCES appointment(id),

                              CONSTRAINT fk_waste_record_generated_by
                                  FOREIGN KEY (generated_by)
                                      REFERENCES app_user(id),

                              CONSTRAINT fk_waste_record_disposed_by
                                  FOREIGN KEY (disposed_by)
                                      REFERENCES app_user(id),

                              CONSTRAINT chk_waste_record_type
                                  CHECK (waste_type IN (
                                                        'CLINICAL',
                                                        'SHARPS',
                                                        'BIOHAZARD',
                                                        'EXPIRED',
                                                        'DAMAGED',
                                                        'GENERAL',
                                                        'OTHER'
                                      )),

                              CONSTRAINT chk_waste_record_quantity
                                  CHECK (quantity > 0),

                              CONSTRAINT chk_waste_disposal_status
                                  CHECK (disposal_status IN (
                                                             'GENERATED',
                                                             'BAGGED',
                                                             'READY_FOR_DISPOSAL',
                                                             'DISPOSED'
                                      )),

                              CONSTRAINT chk_waste_disposed_time
                                  CHECK (
                                      disposed_at IS NULL
                                          OR disposed_at >= generated_at
                                      )
);


CREATE INDEX idx_waste_record_clinic
    ON waste_record (
                     clinic_id,
                     generated_at DESC
        );

CREATE INDEX idx_waste_record_inventory
    ON waste_record (inventory_item_id);

CREATE INDEX idx_waste_record_batch
    ON waste_record (inventory_batch_id);

CREATE INDEX idx_waste_record_status
    ON waste_record (
                     clinic_id,
                     disposal_status
        );
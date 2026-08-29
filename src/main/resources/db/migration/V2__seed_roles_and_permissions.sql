-- =========================================================
-- V2 - SEED SYSTEM ROLES AND PERMISSIONS
-- =========================================================

-- =========================================================
-- SYSTEM ROLES
-- =========================================================

INSERT INTO role (
    role_code,
    role_name,
    description,
    is_system_role,
    is_active
)
VALUES
    ('SUPER_ADMIN', 'Super Admin', 'Platform administrator', TRUE, TRUE),
    ('ADMIN', 'Clinic Admin', 'Clinic owner or senior administrator', TRUE, TRUE),
    ('DOCTOR', 'Doctor', 'Doctor / clinician', TRUE, TRUE),
    ('RECEPTIONIST', 'Receptionist', 'Front desk / reception user', TRUE, TRUE),
    ('ATTENDER', 'Attender', 'Clinical assistant / attender', TRUE, TRUE);


-- =========================================================
-- PATIENT PERMISSIONS
-- =========================================================

INSERT INTO permission (
    permission_code,
    permission_name,
    module,
    description
)
VALUES
    ('PATIENT_VIEW', 'View Patients', 'PATIENT', 'View patient records'),
    ('PATIENT_CREATE', 'Create Patient', 'PATIENT', 'Register new patients'),
    ('PATIENT_EDIT', 'Edit Patient', 'PATIENT', 'Edit patient details'),
    ('PATIENT_DELETE', 'Delete Patient', 'PATIENT', 'Deactivate/delete patient records');


-- =========================================================
-- APPOINTMENT PERMISSIONS
-- =========================================================

INSERT INTO permission (
    permission_code,
    permission_name,
    module,
    description
)
VALUES
    ('APPOINTMENT_VIEW', 'View Appointments', 'APPOINTMENT', 'View appointments'),
    ('APPOINTMENT_CREATE', 'Create Appointment', 'APPOINTMENT', 'Book appointments'),
    ('APPOINTMENT_EDIT', 'Edit Appointment', 'APPOINTMENT', 'Update appointment details'),
    ('APPOINTMENT_CANCEL', 'Cancel Appointment', 'APPOINTMENT', 'Cancel appointments'),
    ('APPOINTMENT_CHECK_IN', 'Check In Patient', 'APPOINTMENT', 'Check in arriving patients');


-- =========================================================
-- CLINICAL PERMISSIONS
-- =========================================================

INSERT INTO permission (
    permission_code,
    permission_name,
    module,
    description
)
VALUES
    ('CASE_SHEET_VIEW', 'View Case Sheet', 'CLINICAL', 'View patient case sheets'),
    ('CASE_SHEET_CREATE', 'Create Case Sheet', 'CLINICAL', 'Create case sheets'),
    ('CASE_SHEET_EDIT', 'Edit Case Sheet', 'CLINICAL', 'Edit case sheets'),
    ('CASE_SHEET_FINALIZE', 'Finalize Case Sheet', 'CLINICAL', 'Finalize consultation case sheets'),
    ('PRESCRIPTION_CREATE', 'Create Prescription', 'CLINICAL', 'Create prescriptions'),
    ('CLINICAL_IMAGE_UPLOAD', 'Upload Clinical Images', 'CLINICAL', 'Upload clinical images and X-rays');


-- =========================================================
-- ODONTOGRAM PERMISSIONS
-- =========================================================

INSERT INTO permission (
    permission_code,
    permission_name,
    module,
    description
)
VALUES
    ('ODONTOGRAM_VIEW', 'View Odontogram', 'ODONTOGRAM', 'View dental chart'),
    ('ODONTOGRAM_EDIT', 'Edit Odontogram', 'ODONTOGRAM', 'Update tooth conditions'),
    ('ODONTOGRAM_CONFIGURE', 'Configure Odontogram', 'ODONTOGRAM', 'Configure colour legend and treatment mappings');


-- =========================================================
-- TREATMENT PERMISSIONS
-- =========================================================

INSERT INTO permission (
    permission_code,
    permission_name,
    module,
    description
)
VALUES
    ('TREATMENT_PLAN_VIEW', 'View Treatment Plan', 'TREATMENT', 'View treatment plans'),
    ('TREATMENT_PLAN_CREATE', 'Create Treatment Plan', 'TREATMENT', 'Create treatment plans'),
    ('TREATMENT_PLAN_EDIT', 'Edit Treatment Plan', 'TREATMENT', 'Edit treatment plans'),
    ('TREATMENT_PLAN_APPROVE', 'Approve Treatment Plan', 'TREATMENT', 'Approve/finalize treatment plans');


-- =========================================================
-- FINANCE PERMISSIONS
-- =========================================================

INSERT INTO permission (
    permission_code,
    permission_name,
    module,
    description
)
VALUES
    ('PAYMENT_VIEW', 'View Payments', 'FINANCE', 'View payment records'),
    ('PAYMENT_RECEIVE', 'Receive Payment', 'FINANCE', 'Record patient payments'),
    ('RECEIPT_GENERATE', 'Generate Receipt', 'FINANCE', 'Generate payment receipts'),
    ('INVOICE_VIEW', 'View Invoice', 'FINANCE', 'View invoices'),
    ('INVOICE_CREATE', 'Create Invoice', 'FINANCE', 'Create invoices'),
    ('EXPENSE_VIEW', 'View Expenses', 'FINANCE', 'View clinic expenses'),
    ('EXPENSE_CREATE', 'Create Expense', 'FINANCE', 'Record clinic expenses'),
    ('PROFIT_VIEW', 'View Profit', 'FINANCE', 'View profit and loss'),
    ('FINANCIAL_REPORT_VIEW', 'View Financial Reports', 'FINANCE', 'View finance reports');


-- =========================================================
-- INVENTORY PERMISSIONS
-- =========================================================

INSERT INTO permission (
    permission_code,
    permission_name,
    module,
    description
)
VALUES
    ('INVENTORY_VIEW', 'View Inventory', 'INVENTORY', 'View inventory'),
    ('INVENTORY_MANAGE', 'Manage Inventory', 'INVENTORY', 'Manage stock and inventory'),
    ('VENDOR_VIEW', 'View Vendors', 'INVENTORY', 'View vendors'),
    ('VENDOR_MANAGE', 'Manage Vendors', 'INVENTORY', 'Create and manage vendors');


-- =========================================================
-- ADMIN PERMISSIONS
-- =========================================================

INSERT INTO permission (
    permission_code,
    permission_name,
    module,
    description
)
VALUES
    ('USER_VIEW', 'View Users', 'ADMIN', 'View clinic users'),
    ('USER_CREATE', 'Create Users', 'ADMIN', 'Create clinic users'),
    ('USER_EDIT', 'Edit Users', 'ADMIN', 'Edit clinic users'),
    ('ROLE_MANAGE', 'Manage Roles', 'ADMIN', 'Manage roles and permissions'),
    ('DEPARTMENT_MANAGE', 'Manage Departments', 'ADMIN', 'Manage clinic departments'),
    ('CLINIC_SETTINGS_MANAGE', 'Manage Clinic Settings', 'ADMIN', 'Manage clinic configuration'),
    ('REPORT_VIEW', 'View Reports', 'REPORTS', 'View clinic reports');


-- =========================================================
-- SUPER ADMIN - ALL PERMISSIONS
-- =========================================================

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
         CROSS JOIN permission p
WHERE r.role_code = 'SUPER_ADMIN';


-- =========================================================
-- ADMIN - ALL CLINIC PERMISSIONS
-- =========================================================

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
         CROSS JOIN permission p
WHERE r.role_code = 'ADMIN';


-- =========================================================
-- DOCTOR PERMISSIONS
-- =========================================================

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
         JOIN permission p
              ON p.permission_code IN (
                                       'PATIENT_VIEW',
                                       'APPOINTMENT_VIEW',
                                       'CASE_SHEET_VIEW',
                                       'CASE_SHEET_CREATE',
                                       'CASE_SHEET_EDIT',
                                       'CASE_SHEET_FINALIZE',
                                       'PRESCRIPTION_CREATE',
                                       'CLINICAL_IMAGE_UPLOAD',
                                       'ODONTOGRAM_VIEW',
                                       'ODONTOGRAM_EDIT',
                                       'TREATMENT_PLAN_VIEW',
                                       'TREATMENT_PLAN_CREATE',
                                       'TREATMENT_PLAN_EDIT',
                                       'PAYMENT_VIEW'
                  )
WHERE r.role_code = 'DOCTOR';


-- =========================================================
-- RECEPTIONIST PERMISSIONS
-- =========================================================

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
         JOIN permission p
              ON p.permission_code IN (
                                       'PATIENT_VIEW',
                                       'PATIENT_CREATE',
                                       'PATIENT_EDIT',
                                       'APPOINTMENT_VIEW',
                                       'APPOINTMENT_CREATE',
                                       'APPOINTMENT_EDIT',
                                       'APPOINTMENT_CANCEL',
                                       'APPOINTMENT_CHECK_IN',
                                       'CASE_SHEET_VIEW',
                                       'PAYMENT_VIEW',
                                       'PAYMENT_RECEIVE',
                                       'RECEIPT_GENERATE',
                                       'INVOICE_VIEW',
                                       'INVOICE_CREATE',
                                       'TREATMENT_PLAN_VIEW'
                  )
WHERE r.role_code = 'RECEPTIONIST';


-- =========================================================
-- ATTENDER PERMISSIONS
-- =========================================================

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
         JOIN permission p
              ON p.permission_code IN (
                                       'PATIENT_VIEW',
                                       'APPOINTMENT_VIEW',
                                       'CASE_SHEET_VIEW',
                                       'CLINICAL_IMAGE_UPLOAD',
                                       'ODONTOGRAM_VIEW',
                                       'ODONTOGRAM_EDIT'
                  )
WHERE r.role_code = 'ATTENDER';
-- Additive authorization extension. V1-V15 remain unchanged.
INSERT INTO permission (permission_code, permission_name, module, description)
VALUES
    ('CONSULTATION_VIEW', 'View Consultations', 'CLINICAL', 'View consultation identity, timing, status and appointment linkage'),
    ('CONSULTATION_MANAGE', 'Manage Consultations', 'CLINICAL', 'Create consultations and perform permitted lifecycle transitions'),
    ('PROCEDURE_VIEW', 'View Procedures', 'TREATMENT', 'View procedures, current prices and condition-linked suggestions'),
    ('PROCEDURE_MANAGE', 'Manage Procedures and Pricing', 'TREATMENT', 'Maintain procedure definitions and pricing and inspect price history'),
    ('INVOICE_ISSUE', 'Issue Invoices', 'FINANCE', 'Issue existing invoices subject to workflow validation'),
    ('INVOICE_CANCEL', 'Cancel Invoices', 'FINANCE', 'Cancel existing invoices subject to workflow validation');

-- Grant only the existing global system roles seeded by V2, not tenant-specific roles.
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM (VALUES
    ('SUPER_ADMIN', 'CONSULTATION_VIEW'),
    ('SUPER_ADMIN', 'CONSULTATION_MANAGE'),
    ('SUPER_ADMIN', 'PROCEDURE_VIEW'),
    ('SUPER_ADMIN', 'PROCEDURE_MANAGE'),
    ('SUPER_ADMIN', 'INVOICE_ISSUE'),
    ('SUPER_ADMIN', 'INVOICE_CANCEL'),
    ('ADMIN', 'CONSULTATION_VIEW'),
    ('ADMIN', 'CONSULTATION_MANAGE'),
    ('ADMIN', 'PROCEDURE_VIEW'),
    ('ADMIN', 'PROCEDURE_MANAGE'),
    ('ADMIN', 'INVOICE_ISSUE'),
    ('ADMIN', 'INVOICE_CANCEL'),
    ('DOCTOR', 'CONSULTATION_VIEW'),
    ('DOCTOR', 'CONSULTATION_MANAGE'),
    ('DOCTOR', 'PROCEDURE_VIEW'),
    ('RECEPTIONIST', 'CONSULTATION_VIEW'),
    ('RECEPTIONIST', 'PROCEDURE_VIEW'),
    ('RECEPTIONIST', 'INVOICE_ISSUE'),
    ('ATTENDER', 'CONSULTATION_VIEW')
) AS grants(role_code, permission_code)
JOIN role r ON r.role_code = grants.role_code AND r.clinic_id IS NULL
JOIN permission p ON p.permission_code = grants.permission_code;

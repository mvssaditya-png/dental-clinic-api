-- Additive platform clinic-management permissions; V1-V16 are frozen.
DO $$ BEGIN
    IF (SELECT count(*) FROM role WHERE role_code = 'SUPER_ADMIN'
        AND clinic_id IS NULL AND is_system_role = TRUE) <> 1 THEN
        RAISE EXCEPTION 'Expected exactly one global system SUPER_ADMIN role';
    END IF;
END $$;
INSERT INTO permission (permission_code, permission_name, module, description) VALUES
('CLINIC_VIEW', 'View Clinics', 'ADMIN', 'List and view active or inactive clinics'),
('CLINIC_CREATE', 'Create Clinics', 'ADMIN', 'Create clinics and default settings'),
('CLINIC_EDIT', 'Edit Clinics', 'ADMIN', 'Update clinic details'),
('CLINIC_STATUS_MANAGE', 'Manage Clinic Status', 'ADMIN', 'Activate and deactivate clinics');
DO $$ DECLARE inserted_count integer; BEGIN
    INSERT INTO role_permission (role_id, permission_id)
    SELECT r.id, p.id FROM role r CROSS JOIN permission p
    WHERE r.role_code = 'SUPER_ADMIN' AND r.clinic_id IS NULL AND r.is_system_role = TRUE
      AND p.permission_code IN ('CLINIC_VIEW', 'CLINIC_CREATE', 'CLINIC_EDIT', 'CLINIC_STATUS_MANAGE');
    GET DIAGNOSTICS inserted_count = ROW_COUNT;
    IF inserted_count <> 4 THEN RAISE EXCEPTION 'Expected exactly four clinic permission grants'; END IF;
END $$;


-- Existing entries remain active; no historical events are fabricated.
ALTER TABLE case_sheet_diagnosis
 ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
 ADD COLUMN updated_at TIMESTAMP,
 ADD COLUMN updated_by UUID REFERENCES app_user(id),
 ADD COLUMN removed_at TIMESTAMP,
 ADD COLUMN removed_by UUID REFERENCES app_user(id);
ALTER TABLE case_sheet_clinical_finding
 ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
 ADD COLUMN updated_at TIMESTAMP,
 ADD COLUMN updated_by UUID REFERENCES app_user(id),
 ADD COLUMN removed_at TIMESTAMP,
 ADD COLUMN removed_by UUID REFERENCES app_user(id);
CREATE INDEX idx_diagnosis_active ON case_sheet_diagnosis(clinic_id,case_sheet_id,created_at,id) WHERE removed_at IS NULL;
CREATE INDEX idx_finding_active ON case_sheet_clinical_finding(clinic_id,case_sheet_id,created_at,id) WHERE removed_at IS NULL;
CREATE INDEX idx_clinical_entry_audit ON audit_log(clinic_id, (new_value->>'caseSheetId'), created_at DESC, id DESC) WHERE module='CASE_SHEET_ENTRIES';
INSERT INTO permission(permission_code,permission_name,module,description) VALUES
 ('CASE_SHEET_HISTORY_VIEW','View Case Sheet History','CLINICAL','View structured clinical entry audit history');
DO $$ DECLARE grants integer; BEGIN
 INSERT INTO role_permission(role_id,permission_id)
 SELECT r.id,p.id FROM role r CROSS JOIN permission p
 WHERE r.role_code IN ('SUPER_ADMIN','ADMIN','DOCTOR') AND r.clinic_id IS NULL AND r.is_system_role=TRUE
 AND p.permission_code='CASE_SHEET_HISTORY_VIEW';
 GET DIAGNOSTICS grants=ROW_COUNT;
 IF grants<>3 THEN RAISE EXCEPTION 'Expected three clinical history grants'; END IF;
END $$;
-- Guard clinical audit records against updates/deletes, including cascade attempts.
CREATE FUNCTION protect_clinical_entry_audit() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF OLD.module='CASE_SHEET_ENTRIES' THEN RAISE EXCEPTION 'Clinical entry audit history is append-only'; END IF;
 IF TG_OP='DELETE' THEN RETURN OLD; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER clinical_entry_audit_immutable BEFORE UPDATE OR DELETE ON audit_log FOR EACH ROW EXECUTE FUNCTION protect_clinical_entry_audit();
CREATE FUNCTION protect_clinical_entry_truncate() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 RAISE EXCEPTION 'Clinical history tables cannot be truncated';
END $$;
CREATE TRIGGER audit_no_truncate BEFORE TRUNCATE ON audit_log FOR EACH STATEMENT EXECUTE FUNCTION protect_clinical_entry_truncate();
-- Existing parent FKs cascade; reject physical child deletion to retain clinical records.
CREATE FUNCTION protect_clinical_entry_delete() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 RAISE EXCEPTION 'Clinical entries must be soft removed';
END $$;
CREATE TRIGGER diagnosis_no_delete BEFORE DELETE ON case_sheet_diagnosis FOR EACH ROW EXECUTE FUNCTION protect_clinical_entry_delete();
CREATE TRIGGER finding_no_delete BEFORE DELETE ON case_sheet_clinical_finding FOR EACH ROW EXECUTE FUNCTION protect_clinical_entry_delete();
CREATE TRIGGER diagnosis_no_truncate BEFORE TRUNCATE ON case_sheet_diagnosis FOR EACH STATEMENT EXECUTE FUNCTION protect_clinical_entry_truncate();
CREATE TRIGGER finding_no_truncate BEFORE TRUNCATE ON case_sheet_clinical_finding FOR EACH STATEMENT EXECUTE FUNCTION protect_clinical_entry_truncate();

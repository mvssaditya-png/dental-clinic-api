ALTER TABLE odontogram DROP CONSTRAINT uq_odontogram_patient;
CREATE UNIQUE INDEX uq_odontogram_active_patient ON odontogram(patient_id) WHERE status='ACTIVE';
ALTER TABLE odontogram ADD COLUMN version BIGINT NOT NULL DEFAULT 0 CHECK(version>=0),
 ADD COLUMN archived_at TIMESTAMP, ADD COLUMN archived_by UUID REFERENCES app_user(id), ADD COLUMN archive_reason TEXT;
-- Legacy archived records retain unknown metadata; no historical events are fabricated.
CREATE TABLE odontogram_archive_audit (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), clinic_id UUID NOT NULL REFERENCES clinic(id),
 odontogram_id UUID NOT NULL UNIQUE REFERENCES odontogram(id), patient_id UUID NOT NULL REFERENCES patient(id),
 actor_id UUID NOT NULL REFERENCES app_user(id), archived_at TIMESTAMP NOT NULL,
 reason TEXT NOT NULL CHECK(char_length(reason) BETWEEN 1 AND 1000),
 previous_version BIGINT NOT NULL, archived_version BIGINT NOT NULL CHECK(archived_version=previous_version+1)
);
CREATE FUNCTION protect_odontogram_archive() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 RAISE EXCEPTION 'Odontogram archive history is immutable'; END $$;
CREATE TRIGGER odontogram_archive_audit_immutable BEFORE UPDATE OR DELETE ON odontogram_archive_audit FOR EACH ROW EXECUTE FUNCTION protect_odontogram_archive();
CREATE TRIGGER odontogram_archive_audit_no_truncate BEFORE TRUNCATE ON odontogram_archive_audit FOR EACH STATEMENT EXECUTE FUNCTION protect_odontogram_archive();
CREATE FUNCTION protect_archived_chart() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF TG_OP='DELETE' OR OLD.status='ARCHIVED' THEN RAISE EXCEPTION 'Archived charts are immutable; charts cannot be deleted'; END IF;
 RETURN NEW; END $$;
CREATE TRIGGER odontogram_archive_protection BEFORE UPDATE OR DELETE ON odontogram FOR EACH ROW EXECUTE FUNCTION protect_archived_chart();
CREATE TRIGGER odontogram_no_truncate BEFORE TRUNCATE ON odontogram FOR EACH STATEMENT EXECUTE FUNCTION protect_odontogram_archive();
CREATE INDEX idx_archived_odontogram_patient ON odontogram(clinic_id,patient_id,archived_at DESC,id DESC) WHERE status='ARCHIVED';
INSERT INTO permission(permission_code,permission_name,module,description) VALUES
 ('ODONTOGRAM_ARCHIVE','Archive Odontogram','ODONTOGRAM','Archive an active chart while preserving clinical history');
DO $$ DECLARE grants integer; BEGIN
 INSERT INTO role_permission(role_id,permission_id) SELECT r.id,p.id FROM role r CROSS JOIN permission p
 WHERE r.role_code IN ('SUPER_ADMIN','ADMIN','DOCTOR') AND r.clinic_id IS NULL AND r.is_system_role=TRUE AND p.permission_code='ODONTOGRAM_ARCHIVE';
 GET DIAGNOSTICS grants=ROW_COUNT;
 IF grants<>3 THEN RAISE EXCEPTION 'Expected three odontogram archive grants'; END IF;
END $$;

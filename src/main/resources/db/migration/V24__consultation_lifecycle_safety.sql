ALTER TABLE case_sheet ADD COLUMN version BIGINT NOT NULL DEFAULT 0
 CONSTRAINT chk_case_sheet_version CHECK (version >= 0);

CREATE TABLE consultation_status_audit (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 consultation_id UUID NOT NULL REFERENCES consultation(id) ON DELETE RESTRICT,
 clinic_id UUID NOT NULL REFERENCES clinic(id) ON DELETE RESTRICT,
 from_status VARCHAR(30) NOT NULL,
 to_status VARCHAR(30) NOT NULL,
 actor_id UUID NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
 changed_at TIMESTAMP NOT NULL,
 CONSTRAINT chk_consultation_audit_transition CHECK (
  (from_status='STARTED' AND to_status IN ('IN_PROGRESS','CANCELLED')) OR
  (from_status='IN_PROGRESS' AND to_status IN ('COMPLETED','CANCELLED')))
);
CREATE INDEX idx_consultation_status_audit_order ON consultation_status_audit(clinic_id,consultation_id,changed_at DESC,id DESC);
CREATE FUNCTION protect_consultation_status_audit() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 RAISE EXCEPTION 'Consultation status audit is append-only';
END $$;
CREATE TRIGGER consultation_status_audit_immutable BEFORE UPDATE OR DELETE ON consultation_status_audit
 FOR EACH ROW EXECUTE FUNCTION protect_consultation_status_audit();
CREATE TRIGGER consultation_status_audit_no_truncate BEFORE TRUNCATE ON consultation_status_audit
 FOR EACH STATEMENT EXECUTE FUNCTION protect_consultation_status_audit();

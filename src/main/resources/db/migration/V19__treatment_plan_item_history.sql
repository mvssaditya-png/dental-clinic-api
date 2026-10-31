-- Existing rows stay active; historical events are not fabricated.
ALTER TABLE treatment_plan_item ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
 ADD COLUMN removed_at TIMESTAMP, ADD COLUMN removed_by UUID REFERENCES app_user(id);
CREATE INDEX idx_plan_item_active ON treatment_plan_item(clinic_id,treatment_plan_id,sequence_number,created_at,id) WHERE removed_at IS NULL;
CREATE TABLE treatment_plan_item_audit (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), clinic_id UUID NOT NULL REFERENCES clinic(id),
 treatment_plan_id UUID NOT NULL REFERENCES treatment_plan(id), item_id UUID NOT NULL REFERENCES treatment_plan_item(id),
 operation VARCHAR(20) NOT NULL CHECK(operation IN ('CREATE','UPDATE','REMOVE','STATUS')),
 actor_id UUID NOT NULL REFERENCES app_user(id), created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 before_snapshot JSONB, after_snapshot JSONB NOT NULL, removal_reason TEXT
);
CREATE INDEX idx_plan_item_audit_page ON treatment_plan_item_audit(clinic_id,treatment_plan_id,created_at DESC,id DESC);
CREATE FUNCTION protect_plan_item_audit() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 RAISE EXCEPTION 'Treatment plan item audit history is append-only'; END $$;
CREATE TRIGGER plan_item_audit_immutable BEFORE UPDATE OR DELETE ON treatment_plan_item_audit FOR EACH ROW EXECUTE FUNCTION protect_plan_item_audit();
CREATE TRIGGER plan_item_audit_no_truncate BEFORE TRUNCATE ON treatment_plan_item_audit FOR EACH STATEMENT EXECUTE FUNCTION protect_plan_item_audit();
CREATE FUNCTION protect_plan_item_delete() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 RAISE EXCEPTION 'Treatment plan items must be soft removed'; END $$;
CREATE TRIGGER plan_item_no_delete BEFORE DELETE ON treatment_plan_item FOR EACH ROW EXECUTE FUNCTION protect_plan_item_delete();
CREATE TRIGGER plan_item_no_truncate BEFORE TRUNCATE ON treatment_plan_item FOR EACH STATEMENT EXECUTE FUNCTION protect_plan_item_delete();
INSERT INTO permission(permission_code,permission_name,module,description) VALUES
 ('TREATMENT_PLAN_HISTORY_VIEW','View Treatment Plan History','TREATMENT','View treatment plan item audit history');
DO $$ DECLARE grants integer; BEGIN
 INSERT INTO role_permission(role_id,permission_id)
 SELECT r.id,p.id FROM role r CROSS JOIN permission p
 WHERE r.role_code IN ('SUPER_ADMIN','ADMIN','DOCTOR') AND r.clinic_id IS NULL AND r.is_system_role=TRUE
 AND p.permission_code='TREATMENT_PLAN_HISTORY_VIEW';
 GET DIAGNOSTICS grants=ROW_COUNT;
 IF grants<>3 THEN RAISE EXCEPTION 'Expected three treatment history grants'; END IF;
END $$;

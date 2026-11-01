-- Foundation only: existing plans remain active and no historical events are fabricated.
ALTER TABLE treatment_plan
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    ADD COLUMN removed_at TIMESTAMP,
    ADD COLUMN removed_by UUID REFERENCES app_user(id),
    ADD CONSTRAINT chk_plan_removal_metadata CHECK (
        (removed_at IS NULL AND removed_by IS NULL) OR
        (removed_at IS NOT NULL AND removed_by IS NOT NULL)
    ),
    ADD CONSTRAINT uq_plan_clinic_identity UNIQUE (id, clinic_id);

CREATE INDEX idx_plan_active_clinic ON treatment_plan(clinic_id, created_at DESC, id DESC)
    WHERE removed_at IS NULL;
CREATE INDEX idx_plan_active_patient ON treatment_plan(clinic_id, patient_id, created_at DESC, id DESC)
    WHERE removed_at IS NULL;
CREATE INDEX idx_plan_active_case_sheet ON treatment_plan(clinic_id, case_sheet_id, created_at DESC, id DESC)
    WHERE removed_at IS NULL;

CREATE TABLE treatment_plan_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id UUID NOT NULL REFERENCES clinic(id),
    treatment_plan_id UUID NOT NULL,
    operation VARCHAR(20) NOT NULL CHECK (operation IN ('CREATE', 'UPDATE', 'REMOVE', 'STATUS')),
    actor_id UUID NOT NULL REFERENCES app_user(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    removal_reason TEXT,
    plan_snapshot JSONB NOT NULL CHECK (jsonb_typeof(plan_snapshot) = 'object'),
    retained_items JSONB NOT NULL CHECK (jsonb_typeof(retained_items) = 'array'),
    CONSTRAINT fk_plan_audit_clinic FOREIGN KEY (treatment_plan_id, clinic_id)
        REFERENCES treatment_plan(id, clinic_id),
    CONSTRAINT chk_plan_audit_removal_reason CHECK (
        operation <> 'REMOVE' OR
        (removal_reason IS NOT NULL AND removal_reason ~ '[^[:space:]]'
         AND char_length(removal_reason) <= 1000)
    )
);
CREATE INDEX idx_plan_audit_page
    ON treatment_plan_audit(clinic_id, treatment_plan_id, created_at DESC, id DESC);

CREATE FUNCTION protect_plan_audit() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
    RAISE EXCEPTION 'Treatment plan audit history is append-only';
END $$;
CREATE TRIGGER plan_audit_immutable BEFORE UPDATE OR DELETE ON treatment_plan_audit
    FOR EACH ROW EXECUTE FUNCTION protect_plan_audit();
CREATE TRIGGER plan_audit_no_truncate BEFORE TRUNCATE ON treatment_plan_audit
    FOR EACH STATEMENT EXECUTE FUNCTION protect_plan_audit();

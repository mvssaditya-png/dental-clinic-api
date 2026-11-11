ALTER TABLE case_sheet ADD COLUMN treatment_plan_requirement VARCHAR(20),
 ADD COLUMN treatment_decision_reason TEXT,
 ADD CONSTRAINT ck_case_sheet_treatment_requirement CHECK(treatment_plan_requirement IN ('REQUIRED','NOT_REQUIRED','DEFERRED'));
-- Null legacy decisions remain unknown. No finalized clinical records are backfilled.
CREATE TABLE case_sheet_treatment_finalization (
 case_sheet_id UUID PRIMARY KEY REFERENCES case_sheet(id),
 clinic_id UUID NOT NULL REFERENCES clinic(id),
 consultation_id UUID NOT NULL REFERENCES consultation(id),
 patient_id UUID NOT NULL REFERENCES patient(id),
 requirement VARCHAR(20) NOT NULL CHECK(requirement IN ('REQUIRED','NOT_REQUIRED','DEFERRED')),
 reason TEXT, actor_id UUID NOT NULL REFERENCES app_user(id), validated_at TIMESTAMP NOT NULL,
 treatment_plan_id UUID REFERENCES treatment_plan(id), treatment_plan_number VARCHAR(50),
 treatment_plan_status VARCHAR(30), treatment_plan_version BIGINT,
 CHECK(requirement <> 'REQUIRED' OR (treatment_plan_id IS NOT NULL AND treatment_plan_number IS NOT NULL AND treatment_plan_status IS NOT NULL AND treatment_plan_version IS NOT NULL)),
 CHECK(requirement <> 'DEFERRED' OR (reason IS NOT NULL AND length(btrim(reason)) > 0))
);
CREATE FUNCTION protect_case_sheet_treatment_evidence() RETURNS trigger LANGUAGE plpgsql AS $$
 BEGIN RAISE EXCEPTION 'Case Sheet treatment finalization evidence is immutable'; END $$;
CREATE TRIGGER immutable_case_sheet_treatment_evidence BEFORE UPDATE OR DELETE ON case_sheet_treatment_finalization FOR EACH ROW EXECUTE FUNCTION protect_case_sheet_treatment_evidence();
CREATE TRIGGER no_truncate_case_sheet_treatment_evidence BEFORE TRUNCATE ON case_sheet_treatment_finalization FOR EACH STATEMENT EXECUTE FUNCTION protect_case_sheet_treatment_evidence();

-- Existing clinical events are never updated. Labels available at migration time
-- are explicitly a BASELINE, not reconstructed event-time facts.
ALTER TABLE odontogram_condition ADD COLUMN version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0);
CREATE UNIQUE INDEX uq_odontogram_active_name ON odontogram_condition(clinic_id,lower(btrim(condition_name))) WHERE is_active;
ALTER TABLE odontogram_condition ADD CONSTRAINT chk_odontogram_name CHECK (length(btrim(condition_name)) > 0);
CREATE TABLE odontogram_history_label (
 history_id UUID PRIMARY KEY REFERENCES odontogram_tooth_history(id) ON DELETE RESTRICT,
 previous_name VARCHAR(150), previous_code VARCHAR(100), new_name VARCHAR(150), new_code VARCHAR(100),
 source VARCHAR(20) NOT NULL CHECK (source IN ('BASELINE','EVENT_TIME')),
 captured_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO odontogram_history_label(history_id,previous_name,previous_code,new_name,new_code,source)
 SELECT h.id,p.condition_name,p.condition_code,n.condition_name,n.condition_code,'BASELINE'
 FROM odontogram_tooth_history h LEFT JOIN odontogram_condition p ON p.id=h.previous_condition_id
 LEFT JOIN odontogram_condition n ON n.id=h.new_condition_id;
CREATE FUNCTION capture_odontogram_history_label() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 -- Serialize label capture with configuration edits, in stable UUID order.
 PERFORM id FROM odontogram_condition WHERE id IN (NEW.previous_condition_id,NEW.new_condition_id) ORDER BY id FOR SHARE;
 INSERT INTO odontogram_history_label(history_id,previous_name,previous_code,new_name,new_code,source)
 SELECT NEW.id,p.condition_name,p.condition_code,n.condition_name,n.condition_code,'EVENT_TIME'
 FROM (SELECT 1) dummy LEFT JOIN odontogram_condition p ON p.id=NEW.previous_condition_id
 LEFT JOIN odontogram_condition n ON n.id=NEW.new_condition_id;
 RETURN NEW;
END $$;
CREATE TRIGGER odontogram_capture_label AFTER INSERT ON odontogram_tooth_history FOR EACH ROW EXECUTE FUNCTION capture_odontogram_history_label();
CREATE TRIGGER odontogram_label_immutable BEFORE UPDATE OR DELETE ON odontogram_history_label FOR EACH ROW EXECUTE FUNCTION protect_odontogram_tooth_history();
CREATE TRIGGER odontogram_label_no_truncate BEFORE TRUNCATE ON odontogram_history_label FOR EACH STATEMENT EXECUTE FUNCTION protect_odontogram_tooth_history();

-- Configuration audit is separate from clinical history. It does not alter observations.
CREATE TABLE odontogram_configuration_audit (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), clinic_id UUID NOT NULL REFERENCES clinic(id),
 condition_id UUID NOT NULL REFERENCES odontogram_condition(id), actor_id UUID REFERENCES app_user(id),
 operation VARCHAR(30) NOT NULL, before_value JSONB, after_value JSONB, changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE FUNCTION audit_odontogram_configuration() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 INSERT INTO odontogram_configuration_audit(clinic_id,condition_id,actor_id,operation,before_value,after_value)
 VALUES(NEW.clinic_id,COALESCE((to_jsonb(NEW)->>'condition_id')::uuid,NEW.id),
 NEW.updated_by,TG_TABLE_NAME||':'||TG_OP,CASE WHEN TG_OP='INSERT' THEN NULL ELSE to_jsonb(OLD) END,to_jsonb(NEW));
 RETURN NEW;
END $$;
-- Table names plus operation need more than thirty characters.
ALTER TABLE odontogram_configuration_audit ALTER COLUMN operation TYPE VARCHAR(60);
CREATE TRIGGER odontogram_condition_audit AFTER INSERT OR UPDATE ON odontogram_condition FOR EACH ROW EXECUTE FUNCTION audit_odontogram_configuration();
CREATE TRIGGER odontogram_colour_audit AFTER INSERT OR UPDATE ON odontogram_colour_config FOR EACH ROW EXECUTE FUNCTION audit_odontogram_configuration();
CREATE TRIGGER odontogram_config_audit_immutable BEFORE UPDATE OR DELETE ON odontogram_configuration_audit FOR EACH ROW EXECUTE FUNCTION protect_odontogram_tooth_history();
CREATE TRIGGER odontogram_config_audit_no_truncate BEFORE TRUNCATE ON odontogram_configuration_audit FOR EACH STATEMENT EXECUTE FUNCTION protect_odontogram_tooth_history();

CREATE FUNCTION initialize_odontogram_conditions(target UUID) RETURNS void LANGUAGE plpgsql AS $$
DECLARE d RECORD; condition UUID;
BEGIN
 FOR d IN SELECT * FROM (VALUES
 ('CARIES','Dental Caries','#E53935'),('HEALTHY','Healthy','#43A047'),('FILLING','Filling','#1E88E5'),
 ('ROOT_CANAL','Root Canal Treatment','#FB8C00'),('CROWN','Crown','#8E24AA'),('MISSING','Missing Tooth','#757575'),
 ('IMPLANT','Implant','#00897B'),('FRACTURED','Fractured Tooth','#F9A825'),('EXTRACTION_INDICATED','Extraction Indicated','#B71C1C')
 ) AS defaults(code,name,colour) LOOP
  condition := NULL;
  -- Respect existing codes/names, including inactive entries; never reactivate or rename.
  SELECT id INTO condition FROM odontogram_condition WHERE clinic_id=target AND
   (condition_code=d.code OR lower(btrim(condition_name))=lower(d.name)) ORDER BY created_at,id LIMIT 1;
  IF condition IS NULL THEN
   INSERT INTO odontogram_condition(clinic_id,condition_code,condition_name) VALUES(target,d.code,d.name) RETURNING id INTO condition;
  END IF;
  -- Only exact-name matches inherit a missing suggested colour. Existing colours always win.
  INSERT INTO odontogram_colour_config(clinic_id,condition_id,colour_hex)
   SELECT target,condition,d.colour WHERE EXISTS (SELECT 1 FROM odontogram_condition WHERE id=condition AND lower(btrim(condition_name))=lower(d.name))
   ON CONFLICT(condition_id) DO NOTHING;
 END LOOP;
END $$;
DO $$ DECLARE c RECORD; BEGIN FOR c IN SELECT id FROM clinic LOOP PERFORM initialize_odontogram_conditions(c.id); END LOOP; END $$;
CREATE FUNCTION initialize_clinic_odontogram() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN PERFORM initialize_odontogram_conditions(NEW.id); RETURN NEW; END $$;
CREATE TRIGGER clinic_odontogram_defaults AFTER INSERT ON clinic FOR EACH ROW EXECUTE FUNCTION initialize_clinic_odontogram();

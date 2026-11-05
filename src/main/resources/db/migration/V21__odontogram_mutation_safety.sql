-- Preserve legacy tooth identifiers and all historical observations.
ALTER TABLE odontogram_tooth ADD COLUMN version BIGINT NOT NULL DEFAULT 0
    CONSTRAINT chk_odontogram_tooth_version CHECK (version >= 0);

ALTER TABLE odontogram_tooth_history
    DROP CONSTRAINT fk_odontogram_history_odontogram,
    DROP CONSTRAINT fk_odontogram_history_tooth,
    ADD CONSTRAINT fk_odontogram_history_odontogram FOREIGN KEY (odontogram_id)
        REFERENCES odontogram(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_odontogram_history_tooth FOREIGN KEY (odontogram_tooth_id)
        REFERENCES odontogram_tooth(id) ON DELETE RESTRICT;

CREATE FUNCTION protect_odontogram_tooth_history() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
    RAISE EXCEPTION 'Odontogram tooth history is append-only';
END $$;
CREATE TRIGGER odontogram_history_immutable BEFORE UPDATE OR DELETE ON odontogram_tooth_history
    FOR EACH ROW EXECUTE FUNCTION protect_odontogram_tooth_history();
CREATE TRIGGER odontogram_history_no_truncate BEFORE TRUNCATE ON odontogram_tooth_history
    FOR EACH STATEMENT EXECUTE FUNCTION protect_odontogram_tooth_history();
CREATE INDEX idx_odontogram_history_stable_order
    ON odontogram_tooth_history(odontogram_id, tooth_number, changed_at DESC, id DESC);

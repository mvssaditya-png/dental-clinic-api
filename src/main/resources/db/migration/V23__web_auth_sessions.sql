CREATE TABLE web_auth_session (
 id UUID PRIMARY KEY, token_hash BYTEA NOT NULL UNIQUE CHECK (octet_length(token_hash)=32),
 user_id UUID NOT NULL REFERENCES app_user(id), created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 last_seen_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, expires_at TIMESTAMPTZ NOT NULL,
 revoked_at TIMESTAMPTZ, revocation_reason VARCHAR(40), csrf_key_version SMALLINT NOT NULL,
 CHECK (expires_at>created_at), CHECK(last_seen_at>=created_at),
 CHECK ((revoked_at IS NULL)=(revocation_reason IS NULL))
);
CREATE INDEX ix_web_auth_session_user ON web_auth_session(user_id);
CREATE INDEX ix_web_auth_session_expiry ON web_auth_session(expires_at);
CREATE TABLE web_auth_session_event (
 id UUID PRIMARY KEY, session_id UUID REFERENCES web_auth_session(id) ON DELETE SET NULL,
 user_id UUID NOT NULL REFERENCES app_user(id), event_type VARCHAR(20) NOT NULL CHECK(event_type IN ('CREATED','REVOKED','EXPIRED')),
 occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, outcome_code VARCHAR(40) NOT NULL,
 UNIQUE(session_id,event_type)
);
CREATE INDEX ix_web_auth_session_event_time ON web_auth_session_event(occurred_at);

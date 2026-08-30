-- =========================================================
-- DENTAL CLINIC APP
-- V15 - OTP AUTHENTICATION
-- =========================================================

CREATE TABLE otp_verification (
                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                  phone VARCHAR(20) NOT NULL,

    -- Never store the OTP itself.
    -- Application stores a hash of the generated OTP.
                                  otp_hash VARCHAR(255) NOT NULL,

                                  purpose VARCHAR(30) NOT NULL DEFAULT 'LOGIN',

                                  expires_at TIMESTAMP NOT NULL,

                                  verified_at TIMESTAMP,

                                  attempt_count INTEGER NOT NULL DEFAULT 0,

                                  is_used BOOLEAN NOT NULL DEFAULT FALSE,

                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  CONSTRAINT chk_otp_purpose
                                      CHECK (purpose IN (
                                          'LOGIN'
                                          )),

                                  CONSTRAINT chk_otp_attempt_count
                                      CHECK (attempt_count >= 0)
);


CREATE INDEX idx_otp_verification_phone
    ON otp_verification (phone);


CREATE INDEX idx_otp_verification_phone_purpose
    ON otp_verification (phone, purpose);


CREATE INDEX idx_otp_verification_expires_at
    ON otp_verification (expires_at);


CREATE INDEX idx_otp_verification_lookup
    ON otp_verification (phone, purpose, created_at DESC)
    WHERE is_used = FALSE;
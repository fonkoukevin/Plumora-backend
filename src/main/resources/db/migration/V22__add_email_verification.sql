-- Existing accounts predate this feature and never went through an email confirmation step, so
-- they must not be locked out retroactively: backfill them to true, then flip the column's
-- steady-state default to false so every new registration starts unverified.
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE users ALTER COLUMN email_verified SET DEFAULT false;

CREATE TABLE email_verification_tokens (
    id_email_verification_token UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id_user) ON DELETE CASCADE,
    token VARCHAR(100) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_email_verification_tokens_user_id ON email_verification_tokens(user_id);

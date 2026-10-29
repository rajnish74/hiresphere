ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE users SET email_verified = TRUE;

CREATE TABLE auth_tokens (
                             id         BIGSERIAL PRIMARY KEY,
                             user_id    BIGINT       NOT NULL REFERENCES users(id),
                             type       VARCHAR(30)  NOT NULL,
                             token_hash VARCHAR(64)  NOT NULL UNIQUE,
                             expires_at TIMESTAMPTZ  NOT NULL,
                             used_at    TIMESTAMPTZ,
                             created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_auth_tokens_user_type ON auth_tokens (user_id, type);
CREATE TABLE refresh_tokens (
                                id         BIGSERIAL PRIMARY KEY,
                                user_id    BIGINT       NOT NULL REFERENCES users(id),
                                token_hash VARCHAR(64)  NOT NULL UNIQUE,
                                family_id  VARCHAR(36)  NOT NULL,
                                expires_at TIMESTAMPTZ  NOT NULL,
                                revoked_at TIMESTAMPTZ,
                                created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_user   ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens(family_id);
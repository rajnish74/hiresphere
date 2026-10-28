-- Timestamps
ALTER TABLE users ALTER COLUMN created_at TYPE TIMESTAMPTZ;
ALTER TABLE users ALTER COLUMN updated_at TYPE TIMESTAMPTZ;

-- Optimistic locking
ALTER TABLE users ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Soft delete
ALTER TABLE users ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN deleted_at TIMESTAMPTZ;

-- Soft delete and email unique
ALTER TABLE users DROP CONSTRAINT users_email_key;
CREATE UNIQUE INDEX uk_users_email_active ON users (email) WHERE deleted = FALSE;
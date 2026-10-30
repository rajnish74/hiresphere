ALTER TABLE users ADD COLUMN token_version INT NOT NULL DEFAULT 0;

ALTER TABLE jobs ADD COLUMN hidden BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE jobs ADD COLUMN moderation_reason VARCHAR(500);

CREATE TABLE audit_logs (
                            id          BIGSERIAL PRIMARY KEY,
                            actor_id    BIGINT       NOT NULL REFERENCES users(id),
                            action      VARCHAR(50)  NOT NULL,
                            target_type VARCHAR(30)  NOT NULL,
                            target_id   BIGINT       NOT NULL,
                            details     VARCHAR(1000),
                            created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_created ON audit_logs (created_at DESC, id DESC);
CREATE INDEX idx_audit_actor   ON audit_logs (actor_id, created_at DESC);
CREATE INDEX idx_audit_target  ON audit_logs (target_type, target_id);
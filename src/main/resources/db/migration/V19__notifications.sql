CREATE TABLE notifications (
                               id          BIGSERIAL PRIMARY KEY,
                               user_id     BIGINT       NOT NULL REFERENCES users(id),
                               type        VARCHAR(40)  NOT NULL,
                               subject     VARCHAR(200) NOT NULL,
                               detail      VARCHAR(200),
                               note        VARCHAR(500),
                               target_type VARCHAR(20),
                               target_id   BIGINT,
                               read_at     TIMESTAMPTZ,
                               created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               CONSTRAINT chk_notifications_target CHECK ((target_type IS NULL) = (target_id IS NULL))
);

CREATE INDEX idx_notifications_user ON notifications (user_id, created_at DESC, id DESC);

CREATE INDEX idx_notifications_unread ON notifications (user_id) WHERE read_at IS NULL;
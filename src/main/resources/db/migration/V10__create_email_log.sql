CREATE TABLE email_log (
                           id         BIGSERIAL PRIMARY KEY,
                           recipient  VARCHAR(150) NOT NULL,
                           subject    VARCHAR(200) NOT NULL,
                           template   VARCHAR(60)  NOT NULL,
                           status     VARCHAR(10)  NOT NULL,
                           attempts   INT          NOT NULL,
                           error      VARCHAR(500),
                           created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_email_log_created ON email_log (created_at DESC);
CREATE TABLE ai_usage (
                          id            BIGSERIAL PRIMARY KEY,
                          user_id       BIGINT,
                          feature       VARCHAR(40)  NOT NULL,
                          model         VARCHAR(100),
                          status        VARCHAR(10)  NOT NULL,
                          error_kind    VARCHAR(30),
                          prompt_tokens INT          NOT NULL DEFAULT 0,
                          output_tokens INT          NOT NULL DEFAULT 0,
                          total_tokens  INT          NOT NULL DEFAULT 0,
                          latency_ms    BIGINT       NOT NULL DEFAULT 0,
                          created_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          CONSTRAINT chk_ai_usage_status CHECK (status IN ('OK', 'FAILED', 'CACHED')),
                          CONSTRAINT chk_ai_usage_tokens CHECK (prompt_tokens >= 0 AND output_tokens >= 0 AND total_tokens >= 0)
);

CREATE INDEX idx_ai_usage_user_day ON ai_usage (user_id, created_at DESC);
CREATE INDEX idx_ai_usage_created ON ai_usage (created_at DESC);
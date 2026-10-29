CREATE TABLE applications (
                              id                   BIGSERIAL PRIMARY KEY,
                              job_id               BIGINT       NOT NULL REFERENCES jobs(id),
                              company_id           BIGINT       NOT NULL REFERENCES companies(id),
                              candidate_user_id    BIGINT       NOT NULL REFERENCES users(id),
                              status               VARCHAR(20)  NOT NULL DEFAULT 'APPLIED',
                              cover_letter         TEXT,
                              job_title            VARCHAR(150) NOT NULL,
                              company_name         VARCHAR(150) NOT NULL,
                              candidate_name       VARCHAR(100) NOT NULL,
                              candidate_headline   VARCHAR(150),
                              resume_storage_key   VARCHAR(100) NOT NULL,
                              resume_original_name VARCHAR(255) NOT NULL,
                              created_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              version              BIGINT       NOT NULL DEFAULT 0,
                              deleted              BOOLEAN      NOT NULL DEFAULT FALSE,
                              deleted_at           TIMESTAMPTZ
);

CREATE UNIQUE INDEX uk_applications_job_candidate ON applications (job_id, candidate_user_id);
CREATE INDEX idx_applications_candidate ON applications (candidate_user_id, created_at DESC);
CREATE INDEX idx_applications_job_status ON applications (job_id, status, created_at DESC);
CREATE INDEX idx_applications_company ON applications (company_id);

CREATE TABLE application_status_history (
                                            id             BIGSERIAL PRIMARY KEY,
                                            application_id BIGINT      NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
                                            from_status    VARCHAR(20),
                                            to_status      VARCHAR(20) NOT NULL,
                                            changed_by     BIGINT      NOT NULL REFERENCES users(id),
                                            note           VARCHAR(500),
                                            created_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_app_history_application ON application_status_history (application_id, created_at);
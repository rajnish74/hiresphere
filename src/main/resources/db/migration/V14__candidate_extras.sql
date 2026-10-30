CREATE TABLE saved_jobs (
                            id                BIGSERIAL PRIMARY KEY,
                            candidate_user_id BIGINT       NOT NULL REFERENCES users(id),
                            job_id            BIGINT       NOT NULL REFERENCES jobs(id),
                            job_title         VARCHAR(150) NOT NULL,
                            company_name      VARCHAR(150) NOT NULL,
                            created_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            CONSTRAINT uk_saved_jobs UNIQUE (candidate_user_id, job_id)
);
CREATE INDEX idx_saved_jobs_candidate ON saved_jobs (candidate_user_id, created_at DESC, id DESC);

CREATE TABLE job_reports (
                             id               BIGSERIAL PRIMARY KEY,
                             job_id           BIGINT       NOT NULL REFERENCES jobs(id),
                             reporter_user_id BIGINT       NOT NULL REFERENCES users(id),
                             reason           VARCHAR(30)  NOT NULL,
                             details          VARCHAR(1000),
                             job_title        VARCHAR(150) NOT NULL,
                             status           VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
                             resolved_by      BIGINT REFERENCES users(id),
                             resolution_note  VARCHAR(500),
                             resolved_at      TIMESTAMPTZ,
                             created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             version          BIGINT       NOT NULL DEFAULT 0,
                             CONSTRAINT uk_job_reports_reporter_job UNIQUE (reporter_user_id, job_id)
);
CREATE INDEX idx_job_reports_status ON job_reports (status, created_at DESC);
CREATE INDEX idx_job_reports_job ON job_reports (job_id);

CREATE TABLE notification_opt_outs (
                                       id         BIGSERIAL PRIMARY KEY,
                                       user_id    BIGINT      NOT NULL REFERENCES users(id),
                                       category   VARCHAR(30) NOT NULL,
                                       created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       CONSTRAINT uk_opt_out UNIQUE (user_id, category)
);

CREATE INDEX idx_applications_company_candidate ON applications (company_id, candidate_user_id);
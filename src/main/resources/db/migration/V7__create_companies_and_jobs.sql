CREATE TABLE companies (
                           id                  BIGSERIAL PRIMARY KEY,
                           owner_id            BIGINT       NOT NULL REFERENCES users(id),
                           name                VARCHAR(150) NOT NULL,
                           website             VARCHAR(255),
                           industry            VARCHAR(100),
                           description         TEXT,
                           location            VARCHAR(100),
                           verification_status VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
                           rejection_reason    VARCHAR(500),
                           verified_at         TIMESTAMPTZ,
                           verified_by         BIGINT REFERENCES users(id),
                           created_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           version             BIGINT       NOT NULL DEFAULT 0,
                           deleted             BOOLEAN      NOT NULL DEFAULT FALSE,
                           deleted_at          TIMESTAMPTZ
);

CREATE UNIQUE INDEX uk_companies_owner_active ON companies(owner_id) WHERE deleted = FALSE;
CREATE INDEX idx_companies_status ON companies(verification_status);

CREATE TABLE jobs (
                      id                   BIGSERIAL PRIMARY KEY,
                      company_id           BIGINT        NOT NULL REFERENCES companies(id),
                      title                VARCHAR(150)  NOT NULL,
                      description          TEXT          NOT NULL,
                      location             VARCHAR(100),
                      job_type             VARCHAR(20)   NOT NULL,
                      work_mode            VARCHAR(20)   NOT NULL,
                      min_experience_years INT           NOT NULL DEFAULT 0,
                      max_experience_years INT,
                      salary_min           NUMERIC(12,2),
                      salary_max           NUMERIC(12,2),
                      currency             VARCHAR(3)    NOT NULL DEFAULT 'INR',
                      openings             INT           NOT NULL DEFAULT 1,
                      status               VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
                      published_at         TIMESTAMPTZ,
                      closed_at            TIMESTAMPTZ,
                      expires_at           TIMESTAMPTZ,
                      created_at           TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      updated_at           TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      version              BIGINT        NOT NULL DEFAULT 0,
                      deleted              BOOLEAN       NOT NULL DEFAULT FALSE,
                      deleted_at           TIMESTAMPTZ,
                      CONSTRAINT chk_jobs_salary CHECK (salary_max IS NULL OR salary_min IS NULL OR salary_max >= salary_min),
                      CONSTRAINT chk_jobs_openings CHECK (openings > 0)
);

CREATE INDEX idx_jobs_company ON jobs(company_id);
CREATE INDEX idx_jobs_status ON jobs(status);

CREATE TABLE job_skills (
                            job_id   BIGINT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
                            skill_id BIGINT NOT NULL REFERENCES skills(id),
                            PRIMARY KEY (job_id, skill_id)
);
CREATE INDEX idx_job_skills_skill ON job_skills(skill_id);
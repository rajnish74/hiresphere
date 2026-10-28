CREATE INDEX idx_jobs_public_feed
    ON jobs (published_at DESC, id DESC)
    WHERE status = 'PUBLISHED' AND deleted = FALSE;

CREATE INDEX idx_jobs_public_salary
    ON jobs (salary_max DESC NULLS LAST, id DESC)
    WHERE status = 'PUBLISHED' AND deleted = FALSE;
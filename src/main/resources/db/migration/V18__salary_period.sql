ALTER TABLE jobs ADD COLUMN salary_period VARCHAR(10) NOT NULL DEFAULT 'YEAR';
ALTER TABLE jobs ADD CONSTRAINT chk_jobs_salary_period CHECK (salary_period IN ('YEAR', 'MONTH', 'HOUR'));

UPDATE jobs SET salary_period = 'MONTH'
WHERE job_type = 'INTERNSHIP' AND salary_max IS NOT NULL AND salary_max < 200000;

ALTER TABLE jobs ADD COLUMN salary_max_annual NUMERIC(18,2) GENERATED ALWAYS AS (
    salary_max * CASE salary_period WHEN 'MONTH' THEN 12 WHEN 'HOUR' THEN 2000 ELSE 1 END
    ) STORED;

DROP INDEX IF EXISTS idx_jobs_public_salary;
CREATE INDEX idx_jobs_public_salary ON jobs (salary_max_annual DESC NULLS LAST, id DESC)
    WHERE status = 'PUBLISHED' AND deleted = FALSE;
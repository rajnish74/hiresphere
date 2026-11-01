ALTER TABLE jobs ADD COLUMN applications_count INT NOT NULL DEFAULT 0;
ALTER TABLE jobs ADD CONSTRAINT chk_jobs_applications_count CHECK (applications_count >= 0);

UPDATE jobs j
SET applications_count = (SELECT count(*) FROM applications a
                          WHERE a.job_id = j.id AND a.status <> 'WITHDRAWN' AND a.deleted = FALSE);
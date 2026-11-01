ALTER TABLE applications ADD COLUMN phone            VARCHAR(15);
ALTER TABLE applications ADD COLUMN current_location VARCHAR(100);
ALTER TABLE applications ADD COLUMN expected_salary  NUMERIC(12,2);
ALTER TABLE applications ADD COLUMN available_from   DATE;
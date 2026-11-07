ALTER TABLE notification_opt_outs ADD COLUMN channel VARCHAR(10) NOT NULL DEFAULT 'EMAIL';
ALTER TABLE notification_opt_outs ALTER COLUMN channel DROP DEFAULT;   -- ab insert mein channel dena zaroori
ALTER TABLE notification_opt_outs DROP CONSTRAINT uk_opt_out;
ALTER TABLE notification_opt_outs ADD CONSTRAINT uk_opt_out UNIQUE (user_id, category, channel);
ALTER TABLE notification_opt_outs ADD CONSTRAINT chk_opt_out_channel CHECK (channel IN ('EMAIL', 'IN_APP'));
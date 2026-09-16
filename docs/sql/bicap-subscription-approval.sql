-- Add manual-admin approval state to existing subscription requests.
-- Run once before deploying with DDL_AUTO=validate.
ALTER TABLE subscriptions
    ADD COLUMN request_status VARCHAR(20) NULL AFTER status;

UPDATE subscriptions
SET request_status = CASE
    WHEN status = 'ACTIVE' THEN 'APPROVED'
    ELSE 'PENDING'
END
WHERE request_status IS NULL;
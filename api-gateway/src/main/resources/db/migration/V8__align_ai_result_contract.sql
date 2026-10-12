-- Keep the existing Java/API enum contract while migrating old SQL spellings.
ALTER TABLE tickets DROP CONSTRAINT tickets_priority_check;
UPDATE tickets SET priority='NORMAL' WHERE priority='MEDIUM';
ALTER TABLE tickets ADD CONSTRAINT tickets_priority_check CHECK (priority IN ('LOW','NORMAL','HIGH','CRITICAL'));
ALTER TABLE tickets DROP CONSTRAINT tickets_status_check;
UPDATE tickets SET status='ANSWED' WHERE status='ANSWERED';
UPDATE tickets SET status='WAITIND_OPERATOR' WHERE status='WAITING_OPERATOR';
UPDATE ticket_status_history SET from_status='ANSWED' WHERE from_status='ANSWERED';
UPDATE ticket_status_history SET to_status='ANSWED' WHERE to_status='ANSWERED';
UPDATE ticket_status_history SET from_status='WAITIND_OPERATOR' WHERE from_status='WAITING_OPERATOR';
UPDATE ticket_status_history SET to_status='WAITIND_OPERATOR' WHERE to_status='WAITING_OPERATOR';
ALTER TABLE tickets ADD CONSTRAINT tickets_status_check CHECK (status IN ('NEW','PROCESSING','CLASSIFIED','ANSWED','WAITIND_OPERATOR','CLOSED','REOPENED','FAILED'));
ALTER TABLE tickets ALTER COLUMN answer TYPE TEXT;

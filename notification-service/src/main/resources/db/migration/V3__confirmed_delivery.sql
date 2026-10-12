ALTER TABLE notification ADD COLUMN delivered_at TIMESTAMPTZ;
CREATE INDEX idx_notification_delivery ON notification(user_id, created_at, id)
    WHERE delivered_at IS NULL AND NOT is_read;

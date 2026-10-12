ALTER TABLE notification ADD COLUMN ws_published BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_notification_ws_pending ON notification(created_at) WHERE NOT ws_published;

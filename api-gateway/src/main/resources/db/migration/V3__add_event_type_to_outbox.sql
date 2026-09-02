ALTER TABLE outbox_event
    ADD COLUMN event_type VARCHAR(64) NOT NULL;
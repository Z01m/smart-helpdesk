CREATE TABLE notification (
                              id UUID PRIMARY KEY,
                              user_id UUID NOT NULL,
                              ticket_id UUID NOT NULL,
                              type VARCHAR(64) NOT NULL,
                              content TEXT NOT NULL,
                              is_read BOOLEAN NOT NULL DEFAULT FALSE,
                              created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notification_user_id
    ON notification(user_id);

CREATE INDEX idx_notification_ticket_id
    ON notification(ticket_id);

CREATE INDEX idx_notification_user_unread
    ON notification(user_id, is_read);


CREATE TABLE processed_event (
                                 event_id UUID PRIMARY KEY,
                                 processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE result_outbox (
 id UUID PRIMARY KEY,
 ticket_id UUID NOT NULL,
 payload TEXT NOT NULL,
 correlation_id VARCHAR(128) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 published_at TIMESTAMPTZ
);
CREATE INDEX idx_result_outbox_pending ON result_outbox(created_at,id) WHERE published_at IS NULL;

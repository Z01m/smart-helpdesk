CREATE TABLE processed_event (
                                 event_id UUID PRIMARY KEY,
                                 processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ai_processing_result (
                                      id UUID PRIMARY KEY,
                                      ticket_id UUID NOT NULL,

                                      category VARCHAR(64),
                                      priority VARCHAR(16),
                                      sentiment VARCHAR(16),

                                      answer TEXT,

                                      processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_processing_result_ticket_id
    ON ai_processing_result(ticket_id);

CREATE INDEX idx_ai_processing_result_processed_at
    ON ai_processing_result(processed_at DESC);
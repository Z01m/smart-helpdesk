CREATE TABLE ticket_results (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                ticket_id UUID NOT NULL UNIQUE,
                                generated_answer TEXT NOT NULL,
                                category VARCHAR(64) NOT NULL,
                                priority VARCHAR(16) NOT NULL,
                                sentiment VARCHAR(16) NOT NULL,
                                confidence NUMERIC(4,3),
                                context_sources JSONB,
                                created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ticket_results_ticket_id
    ON ticket_results(ticket_id);
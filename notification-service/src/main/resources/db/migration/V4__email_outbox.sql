CREATE TABLE email_subscription (
    user_id UUID PRIMARY KEY,
    email_ciphertext TEXT NOT NULL,
    challenge_hash VARCHAR(64),
    challenge_expires_at TIMESTAMPTZ,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    verified_at TIMESTAMPTZ,
    enabled BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE TABLE email_outbox (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES email_subscription(user_id),
    notification_id UUID UNIQUE REFERENCES notification(id) ON DELETE CASCADE,
    kind VARCHAR(16) NOT NULL CHECK (kind IN ('VERIFY','NOTIFICATION')),
    ciphertext TEXT,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','SENDING','SENT','FAILED','CANCELLED')),
    attempts INT NOT NULL DEFAULT 0,
    available_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ,
    lease_id UUID,
    lease_until TIMESTAMPTZ,
    last_error VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_email_pending ON email_outbox(available_at) WHERE status IN ('PENDING','SENDING');

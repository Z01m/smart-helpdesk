CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
                       id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       email           VARCHAR(255) NOT NULL UNIQUE,
                       password_hash   VARCHAR(255) NOT NULL,
                       full_name       VARCHAR(255) NOT NULL,
                       role            VARCHAR(32)  NOT NULL DEFAULT 'CUSTOMER'
                           CHECK (role IN ('CUSTOMER','OPERATOR','ADMIN')),
                       customer_tier   VARCHAR(32)  NOT NULL DEFAULT 'STANDARD'
                           CHECK (customer_tier IN ('STANDARD','PREMIUM','VIP')),
                       created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                       updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_users_email ON users(email);

CREATE TABLE tickets (
                         id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                         user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                         operator_id     UUID REFERENCES users(id) ON DELETE SET NULL,
                         message         TEXT NOT NULL,
                         status          VARCHAR(32) NOT NULL DEFAULT 'NEW'
                             CHECK (status IN ('NEW','PROCESSING','CLASSIFIED','ANSWERED',
                                               'WAITING_OPERATOR','CLOSED','REOPENED','FAILED')),
                         category        VARCHAR(64),
                         priority        VARCHAR(16) CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL')),
                         sentiment       VARCHAR(16) CHECK (sentiment IN ('POSITIVE','NEUTRAL','NEGATIVE','ANGRY')),
                         version         INT NOT NULL DEFAULT 0,
                         created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                         updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_tickets_user_id ON tickets(user_id);
CREATE INDEX idx_tickets_operator_id ON tickets(operator_id);
CREATE INDEX idx_tickets_status ON tickets(status);
CREATE INDEX idx_tickets_created_at ON tickets(created_at DESC);

CREATE TABLE ticket_status_history (
                                       id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                       ticket_id       UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
                                       from_status     VARCHAR(32),
                                       to_status       VARCHAR(32) NOT NULL,
                                       changed_by      UUID,
                                       changed_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_tsh_ticket_id ON ticket_status_history(ticket_id);
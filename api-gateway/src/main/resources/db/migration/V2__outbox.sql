CREATE TABLE outbox_event (
    id UUID PRIMARY KEY,
    aggregate_type varchar(64) not null,
    aggregate_id UUID not null ,
    payload JSONB not null ,
    published BOOLEAN not null default false,
    created_at TIMESTAMPTZ not null default now(),
    published_at TIMESTAMPTZ
);
CREATE INDEX idx_outbox_unpublished
    ON outbox_event (published, created_at)
    WHERE published = FALSE;
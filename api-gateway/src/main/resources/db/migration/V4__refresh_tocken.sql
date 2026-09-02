CREATE TABLE refresh_token(
    id UUID primary key,
    user_id UUID not null,
    token_hash VARCHAR(255) not null unique,
    expires_at TIMESTAMPTZ not null,
    revoked BOOLEAN not null default false,
    created_at TIMESTAMPTZ not null,

    CONSTRAINT fk_refresh_token_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_refresh_token_user_id
    ON refresh_token(user_id);
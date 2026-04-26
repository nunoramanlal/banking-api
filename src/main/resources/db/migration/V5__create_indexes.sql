DROP TABLE IF EXISTS refresh_tokens;

CREATE TABLE refresh_tokens (
    token_hash  VARCHAR(64)     NOT NULL,
    user_id     VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (token_hash),
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_user_revoked ON refresh_tokens (user_id, revoked);
CREATE INDEX idx_refresh_tokens_expires_at   ON refresh_tokens (expires_at);
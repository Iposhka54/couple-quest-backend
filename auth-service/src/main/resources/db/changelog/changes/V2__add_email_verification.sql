--liquibase formatted sql

--changeset auth-service:add-email-verification-columns-to-users
ALTER TABLE users
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN email_verified_at TIMESTAMP NULL;

--changeset auth-service:create-email-verification-table
CREATE TABLE email_verification
(
    id                  UUID PRIMARY KEY,
    user_id             BIGINT       NOT NULL REFERENCES users (id),
    code_hash           VARCHAR(255) NOT NULL,
    expires_at          TIMESTAMP    NOT NULL,
    resend_available_at TIMESTAMP    NOT NULL,
    attempts_count      INTEGER      NOT NULL DEFAULT 0,
    max_attempts        INTEGER      NOT NULL,
    used_at             TIMESTAMP NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT now()
);

--changeset auth-service:create-email-verification-indexes
CREATE INDEX idx_email_verification_user_id_created_at
    ON email_verification (user_id, created_at DESC);

CREATE UNIQUE INDEX uk_email_verification_active_user
    ON email_verification (user_id)
    WHERE used_at IS NULL;
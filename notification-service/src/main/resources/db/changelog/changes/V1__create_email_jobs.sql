CREATE TABLE email_jobs (
    event_id UUID PRIMARY KEY,
    code_id UUID NOT NULL,
    user_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    next_attempt_at TIMESTAMP,
    sent_at TIMESTAMP,
    error_message TEXT
);

CREATE INDEX idx_email_jobs_status_next_attempt_created
    ON email_jobs (status, next_attempt_at, created_at);

CREATE INDEX idx_email_jobs_processing_updated
    ON email_jobs (status, updated_at);
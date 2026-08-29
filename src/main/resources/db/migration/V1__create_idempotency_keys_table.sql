CREATE TABLE idempotency_keys (
    key VARCHAR(128) PRIMARY KEY,
    payload_hash VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL, -- PROCESSING, COMPLETED, FAILED
    response_code INT,
    response_body TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    locked_until TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_idempotency_keys_status_locked ON idempotency_keys(status, locked_until);

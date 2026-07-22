ALTER TABLE document_processed_inbox
    ADD COLUMN schema_version INTEGER,
    ADD COLUMN occurred_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN error_code VARCHAR(100),
    ADD COLUMN error_retryable BOOLEAN;

ALTER TABLE document_processed_inbox
    ADD CONSTRAINT ck_processed_inbox_schema_version
        CHECK (
            schema_version IS NULL
            OR schema_version = 1
        ),
    ADD CONSTRAINT ck_processed_inbox_v1_occurred_at
        CHECK (
            schema_version IS NULL
            OR occurred_at IS NOT NULL
        ),
    ADD CONSTRAINT ck_processed_inbox_v1_error
        CHECK (
            schema_version IS NULL
            OR (
                result_status = 'PROCESSED'
                AND error_code IS NULL
                AND error_retryable IS NULL
            )
            OR (
                result_status = 'FAILED'
                AND error_code IS NOT NULL
                AND error_retryable IS NOT NULL
            )
        );

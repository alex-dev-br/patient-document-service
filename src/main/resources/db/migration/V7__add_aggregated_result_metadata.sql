ALTER TABLE document_processed_inbox
    ADD COLUMN correlation_id UUID,
    ADD COLUMN document_date DATE;

CREATE INDEX idx_processed_inbox_correlation_id
    ON document_processed_inbox (correlation_id);

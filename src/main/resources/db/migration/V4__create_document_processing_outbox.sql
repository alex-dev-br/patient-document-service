CREATE TABLE document_processing_outbox (
                                            event_id UUID PRIMARY KEY,
                                            document_id UUID NOT NULL,
                                            patient_id UUID NOT NULL,
                                            status VARCHAR(30) NOT NULL,
                                            attempt_count INTEGER NOT NULL DEFAULT 0,
                                            error_detail VARCHAR(2000),
                                            created_at TIMESTAMP NOT NULL,
                                            published_at TIMESTAMP,

                                            CONSTRAINT fk_processing_outbox_document
                                                FOREIGN KEY (document_id)
                                                    REFERENCES health_documents (id),

                                            CONSTRAINT fk_processing_outbox_patient
                                                FOREIGN KEY (patient_id)
                                                    REFERENCES patients (id),

                                            CONSTRAINT ck_processing_outbox_status
                                                CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),

                                            CONSTRAINT ck_processing_outbox_attempt_count
                                                CHECK (attempt_count >= 0)
);

CREATE INDEX idx_processing_outbox_status_created_at
    ON document_processing_outbox (status, created_at);

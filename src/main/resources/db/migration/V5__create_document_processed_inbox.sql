CREATE TABLE document_processed_inbox (
                                          id UUID PRIMARY KEY,
                                          event_id UUID NOT NULL,
                                          document_id UUID NOT NULL,
                                          patient_id UUID NOT NULL,
                                          external_result_id VARCHAR(64) NOT NULL,
                                          external_document_type VARCHAR(100),
                                          result_status VARCHAR(30) NOT NULL,
                                          payload JSONB,
                                          error_detail VARCHAR(2000),
                                          received_at TIMESTAMP NOT NULL,

                                          CONSTRAINT fk_processed_inbox_document
                                              FOREIGN KEY (document_id)
                                                  REFERENCES health_documents (id),

                                          CONSTRAINT fk_processed_inbox_patient
                                              FOREIGN KEY (patient_id)
                                                  REFERENCES patients (id),

                                          CONSTRAINT uk_processed_inbox_event_result
                                              UNIQUE (event_id, external_result_id),

                                          CONSTRAINT ck_processed_inbox_status
                                              CHECK (result_status IN ('PROCESSED', 'FAILED'))
);

CREATE INDEX idx_processed_inbox_document_received_at
    ON document_processed_inbox (document_id, received_at);

CREATE INDEX idx_processed_inbox_event_id
    ON document_processed_inbox (event_id);

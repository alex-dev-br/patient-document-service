CREATE TABLE health_documents (
                                  id UUID PRIMARY KEY,
                                  patient_id UUID NOT NULL,
                                  original_file_name VARCHAR(255) NOT NULL,
                                  stored_file_name VARCHAR(255) NOT NULL,
                                  storage_path TEXT NOT NULL,
                                  content_type VARCHAR(100),
                                  file_size BIGINT,
                                  document_type VARCHAR(80),
                                  specialty VARCHAR(80),
                                  document_date DATE,
                                  summary TEXT,
                                  confidence NUMERIC(5, 4),
                                  processing_status VARCHAR(50) NOT NULL,
                                  created_at TIMESTAMP NOT NULL,
                                  processed_at TIMESTAMP,

                                  CONSTRAINT fk_health_documents_patient
                                      FOREIGN KEY (patient_id)
                                          REFERENCES patients(id)
);

CREATE INDEX idx_health_documents_patient_id
    ON health_documents(patient_id);

CREATE INDEX idx_health_documents_document_date
    ON health_documents(document_date);

CREATE TABLE document_keywords (
                                   id UUID PRIMARY KEY,
                                   document_id UUID NOT NULL,
                                   keyword VARCHAR(100) NOT NULL,

                                   CONSTRAINT fk_document_keywords_document
                                       FOREIGN KEY (document_id)
                                           REFERENCES health_documents(id)
);

CREATE INDEX idx_document_keywords_document_id
    ON document_keywords(document_id);

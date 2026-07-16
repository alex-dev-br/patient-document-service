package br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.outbox;

public enum DocumentProcessingOutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}

package br.com.fiap.techchallenge.patientdocument.application.document.event;

import java.util.Objects;
import java.util.UUID;

public record DocumentProcessingRequestedEvent(
        UUID eventId,
        UUID documentId,
        UUID patientId
) {

    public DocumentProcessingRequestedEvent {
        Objects.requireNonNull(eventId, "O eventId é obrigatório.");
        Objects.requireNonNull(documentId, "O documentId é obrigatório.");
        Objects.requireNonNull(patientId, "O patientId é obrigatório.");
    }
}

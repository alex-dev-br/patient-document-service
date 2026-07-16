package br.com.fiap.techchallenge.patientdocument.application.document.gateway;

import br.com.fiap.techchallenge.patientdocument.application.document.event.DocumentProcessingRequestedEvent;

import java.util.UUID;

public interface DocumentProcessingEventGateway {

    void enqueue(DocumentProcessingRequestedEvent event);

    boolean existsByEventIdAndDocumentIdAndPatientId(
            UUID eventId,
            UUID documentId,
            UUID patientId
    );
}

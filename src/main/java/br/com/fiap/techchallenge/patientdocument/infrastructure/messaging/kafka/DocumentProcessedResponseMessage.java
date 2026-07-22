package br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DocumentProcessedResponseMessage(
        Integer schemaVersion,
        String eventType,
        Instant occurredAt,
        UUID eventId,
        UUID documentId,
        UUID patientId,
        String status,
        Map<String, Object> document,
        DocumentProcessingErrorMessage error,
        String errorDetail
) {

    public DocumentProcessedResponseMessage {
        document = document == null
                ? null
                : Collections.unmodifiableMap(
                        new LinkedHashMap<>(document)
                );
    }

    /*
     * Construtor legado temporário.
     * Deve ser removido após todos os produtores emitirem o contrato v1.
     */
    public DocumentProcessedResponseMessage(
            UUID eventId,
            UUID documentId,
            UUID patientId,
            String status,
            Map<String, Object> document,
            String errorDetail
    ) {
        this(
                null,
                null,
                null,
                eventId,
                documentId,
                patientId,
                status,
                document,
                null,
                errorDetail
        );
    }
}

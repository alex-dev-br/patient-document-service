package br.com.fiap.techchallenge.patientdocument.application.document.command;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record ProcessDocumentProcessedResponseCommand(
        Integer schemaVersion,
        Instant occurredAt,
        UUID eventId,
        UUID documentId,
        UUID patientId,
        String status,
        Map<String, Object> document,
        String errorCode,
        String errorDetail,
        Boolean errorRetryable
) {

    public ProcessDocumentProcessedResponseCommand {
        document = document == null
                ? null
                : Collections.unmodifiableMap(
                        new LinkedHashMap<>(document)
                );
    }

    /*
     * Construtor legado temporário.
     */
    public ProcessDocumentProcessedResponseCommand(
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
                eventId,
                documentId,
                patientId,
                status,
                document,
                null,
                errorDetail,
                null
        );
    }
}

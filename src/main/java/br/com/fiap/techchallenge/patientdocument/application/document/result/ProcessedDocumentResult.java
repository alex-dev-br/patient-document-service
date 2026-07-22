package br.com.fiap.techchallenge.patientdocument.application.document.result;

import br.com.fiap.techchallenge.patientdocument.domain.document.DocumentProcessingStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record ProcessedDocumentResult(
        UUID id,
        Integer schemaVersion,
        Instant occurredAt,
        UUID eventId,
        UUID documentId,
        UUID patientId,
        String externalResultId,
        String externalDocumentType,
        DocumentProcessingStatus status,
        Map<String, Object> payload,
        String errorCode,
        String errorDetail,
        Boolean errorRetryable,
        LocalDateTime receivedAt
) {

    public ProcessedDocumentResult {
        Objects.requireNonNull(id, "O id do resultado é obrigatório.");
        Objects.requireNonNull(eventId, "O eventId é obrigatório.");
        Objects.requireNonNull(documentId, "O documentId é obrigatório.");
        Objects.requireNonNull(patientId, "O patientId é obrigatório.");

        Objects.requireNonNull(
                externalResultId,
                "O identificador externo do resultado é obrigatório."
        );

        Objects.requireNonNull(
                status,
                "O status do resultado é obrigatório."
        );

        Objects.requireNonNull(
                receivedAt,
                "A data de recebimento é obrigatória."
        );

        if (status != DocumentProcessingStatus.PROCESSED
                && status != DocumentProcessingStatus.FAILED) {
            throw new IllegalArgumentException(
                    "O resultado recebido deve estar como "
                            + "PROCESSED ou FAILED."
            );
        }

        payload = payload == null
                ? null
                : Collections.unmodifiableMap(
                        new LinkedHashMap<>(payload)
                );
    }

    /*
     * Construtor legado temporário.
     */
    public ProcessedDocumentResult(
            UUID id,
            UUID eventId,
            UUID documentId,
            UUID patientId,
            String externalResultId,
            String externalDocumentType,
            DocumentProcessingStatus status,
            Map<String, Object> payload,
            String errorDetail,
            LocalDateTime receivedAt
    ) {
        this(
                id,
                null,
                null,
                eventId,
                documentId,
                patientId,
                externalResultId,
                externalDocumentType,
                status,
                payload,
                null,
                errorDetail,
                null,
                receivedAt
        );
    }
}

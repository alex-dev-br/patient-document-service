package br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record DocumentProcessingRequestedMessage(
        int schemaVersion,
        String eventType,
        Instant occurredAt,
        UUID eventId,
        UUID documentId,
        UUID patientId,
        String fileUrl,
        String contentType
) {

    public static final int SCHEMA_VERSION = 1;

    public static final String EVENT_TYPE =
            "DOCUMENT_PROCESSING_REQUESTED";

    private static final int CONTENT_TYPE_MAX_LENGTH = 100;

    public DocumentProcessingRequestedMessage {
        if (schemaVersion != SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "A schemaVersion deve ser igual a 1."
            );
        }

        if (!EVENT_TYPE.equals(eventType)) {
            throw new IllegalArgumentException(
                    "O eventType deve ser "
                            + "DOCUMENT_PROCESSING_REQUESTED."
            );
        }

        Objects.requireNonNull(
                occurredAt,
                "O occurredAt é obrigatório."
        );

        Objects.requireNonNull(
                eventId,
                "O eventId é obrigatório."
        );

        Objects.requireNonNull(
                documentId,
                "O documentId é obrigatório."
        );

        Objects.requireNonNull(
                patientId,
                "O patientId é obrigatório."
        );

        if (fileUrl == null || fileUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "O fileUrl é obrigatório."
            );
        }

        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException(
                    "O contentType é obrigatório."
            );
        }

        if (contentType.length() > CONTENT_TYPE_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "O contentType deve possuir no máximo "
                            + CONTENT_TYPE_MAX_LENGTH
                            + " caracteres."
            );
        }
    }

    public static DocumentProcessingRequestedMessage versionOne(
            Instant occurredAt,
            UUID eventId,
            UUID documentId,
            UUID patientId,
            String fileUrl,
            String contentType
    ) {
        return new DocumentProcessingRequestedMessage(
                SCHEMA_VERSION,
                EVENT_TYPE,
                occurredAt,
                eventId,
                documentId,
                patientId,
                fileUrl,
                contentType
        );
    }
}
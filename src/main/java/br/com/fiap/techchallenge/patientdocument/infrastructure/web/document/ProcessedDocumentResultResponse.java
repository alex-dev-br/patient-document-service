package br.com.fiap.techchallenge.patientdocument.infrastructure.web.document;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record ProcessedDocumentResultResponse(
        Integer schemaVersion,
        Instant occurredAt,
        UUID eventId,
        UUID documentId,
        UUID patientId,
        String resultId,
        String documentType,
        String status,
        Map<String, Object> document,
        String errorCode,
        String errorDetail,
        Boolean errorRetryable,
        LocalDateTime receivedAt
) {
}

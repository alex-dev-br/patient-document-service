package br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka;

import java.util.Map;
import java.util.UUID;

public record DocumentProcessedResponseMessage(
        UUID eventId,
        UUID documentId,
        UUID patientId,
        String status,
        Map<String, Object> document,
        String errorDetail
) {
}

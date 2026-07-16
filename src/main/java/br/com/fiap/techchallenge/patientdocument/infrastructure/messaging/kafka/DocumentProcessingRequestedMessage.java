package br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka;

import java.util.UUID;

public record DocumentProcessingRequestedMessage(
        UUID eventId,
        UUID documentId,
        UUID patientId,
        String fileUrl
) {
}

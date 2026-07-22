package br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DocumentProcessingErrorMessage(
        String code,
        String message,
        Boolean retryable
) {
}

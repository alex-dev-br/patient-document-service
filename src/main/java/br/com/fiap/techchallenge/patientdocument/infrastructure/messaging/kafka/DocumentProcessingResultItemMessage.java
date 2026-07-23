package br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record DocumentProcessingResultItemMessage(
        String resultId,
        String documentType,
        LocalDate documentDate,
        Map<String, Object> data
) {

    public DocumentProcessingResultItemMessage {
        data = data == null
                ? null
                : Collections.unmodifiableMap(
                        new LinkedHashMap<>(data)
                );
    }
}

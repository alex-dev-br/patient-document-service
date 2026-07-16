package br.com.fiap.techchallenge.patientdocument.application.document.command;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record ProcessDocumentProcessedResponseCommand(
        UUID eventId,
        UUID documentId,
        UUID patientId,
        String status,
        Map<String, Object> document,
        String errorDetail
) {

    public ProcessDocumentProcessedResponseCommand {
        document = document == null
                ? null
                : Collections.unmodifiableMap(
                new LinkedHashMap<>(document)
        );
    }
}

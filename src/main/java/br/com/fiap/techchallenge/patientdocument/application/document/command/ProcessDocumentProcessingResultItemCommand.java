package br.com.fiap.techchallenge.patientdocument.application.document.command;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record ProcessDocumentProcessingResultItemCommand(
        String resultId,
        String documentType,
        LocalDate documentDate,
        Map<String, Object> data
) {

    public ProcessDocumentProcessingResultItemCommand {
        data = data == null
                ? null
                : Collections.unmodifiableMap(
                        new LinkedHashMap<>(data)
                );
    }
}

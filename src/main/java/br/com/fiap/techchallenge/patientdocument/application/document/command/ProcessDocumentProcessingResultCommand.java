package br.com.fiap.techchallenge.patientdocument.application.document.command;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record ProcessDocumentProcessingResultCommand(
        Integer schemaVersion,
        String eventType,
        UUID eventId,
        UUID correlationId,
        Instant occurredAt,
        UUID documentId,
        UUID patientId,
        String summary,
        String primaryDocumentType,
        String specialty,
        LocalDate documentDate,
        BigDecimal confidence,
        List<ProcessDocumentProcessingResultItemCommand> results,
        String errorCode,
        String errorDetail,
        Boolean errorRetryable
) {

    public ProcessDocumentProcessingResultCommand {
        results = results == null
                ? null
                : Collections.unmodifiableList(
                        new ArrayList<>(results)
                );
    }
}

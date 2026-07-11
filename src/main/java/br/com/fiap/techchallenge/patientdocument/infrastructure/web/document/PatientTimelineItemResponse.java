package br.com.fiap.techchallenge.patientdocument.infrastructure.web.document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PatientTimelineItemResponse(
        UUID documentId,
        String originalFileName,
        String documentType,
        String specialty,
        LocalDate documentDate,
        LocalDateTime createdAt,
        String summary,
        String processingStatus,
        List<String> keywords
) {
}

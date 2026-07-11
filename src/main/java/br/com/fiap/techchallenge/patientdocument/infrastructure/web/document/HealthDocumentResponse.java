package br.com.fiap.techchallenge.patientdocument.infrastructure.web.document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record HealthDocumentResponse(
        UUID id,
        UUID patientId,
        String originalFileName,
        String contentType,
        Long fileSize,
        String documentType,
        String specialty,
        LocalDate documentDate,
        String summary,
        BigDecimal confidence,
        String processingStatus,
        LocalDateTime createdAt,
        LocalDateTime processedAt,
        List<String> keywords
) {
}

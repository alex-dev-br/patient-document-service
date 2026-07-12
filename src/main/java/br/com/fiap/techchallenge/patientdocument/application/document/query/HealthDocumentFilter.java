package br.com.fiap.techchallenge.patientdocument.application.document.query;

import br.com.fiap.techchallenge.patientdocument.domain.document.DocumentProcessingStatus;
import br.com.fiap.techchallenge.patientdocument.domain.document.DocumentType;
import br.com.fiap.techchallenge.patientdocument.domain.document.MedicalSpecialty;

import java.time.LocalDate;

public record HealthDocumentFilter(
        DocumentType documentType,
        MedicalSpecialty specialty,
        DocumentProcessingStatus status,
        String keyword,
        LocalDate startDate,
        LocalDate endDate
) {

    public HealthDocumentFilter {
        keyword = keyword == null || keyword.isBlank()
                ? null
                : keyword.trim();
    }

    public static HealthDocumentFilter empty() {
        return new HealthDocumentFilter(
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public boolean hasInvalidDateRange() {
        return startDate != null
                && endDate != null
                && startDate.isAfter(endDate);
    }
}

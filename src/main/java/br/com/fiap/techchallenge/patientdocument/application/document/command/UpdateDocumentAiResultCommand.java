package br.com.fiap.techchallenge.patientdocument.application.document.command;

import br.com.fiap.techchallenge.patientdocument.domain.document.DocumentProcessingStatus;
import br.com.fiap.techchallenge.patientdocument.domain.document.DocumentType;
import br.com.fiap.techchallenge.patientdocument.domain.document.MedicalSpecialty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UpdateDocumentAiResultCommand(
        UUID documentId,
        DocumentType documentType,
        MedicalSpecialty specialty,
        LocalDate documentDate,
        String summary,
        List<String> keywords,
        BigDecimal confidence,
        DocumentProcessingStatus status
) {
}

package br.com.fiap.techchallenge.patientdocument.infrastructure.web.document;

import br.com.fiap.techchallenge.patientdocument.application.document.command.UpdateDocumentAiResultCommand;
import br.com.fiap.techchallenge.patientdocument.domain.document.DocumentProcessingStatus;
import br.com.fiap.techchallenge.patientdocument.domain.document.DocumentType;
import br.com.fiap.techchallenge.patientdocument.domain.document.HealthDocument;
import br.com.fiap.techchallenge.patientdocument.domain.document.MedicalSpecialty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class HealthDocumentWebMapper {

    public HealthDocumentResponse toResponse(HealthDocument document) {
        return new HealthDocumentResponse(
                document.getId(),
                document.getPatientId(),
                document.getOriginalFileName(),
                document.getContentType(),
                document.getFileSize(),
                document.getDocumentType() == null ? null : document.getDocumentType().name(),
                document.getSpecialty() == null ? null : document.getSpecialty().name(),
                document.getDocumentDate(),
                document.getSummary(),
                document.getConfidence(),
                document.getProcessingStatus().name(),
                document.getCreatedAt(),
                document.getProcessedAt(),
                document.getKeywords()
        );
    }

    public PatientTimelineItemResponse toTimelineItemResponse(HealthDocument document) {
        return new PatientTimelineItemResponse(
                document.getId(),
                document.getOriginalFileName(),
                document.getDocumentType() == null ? null : document.getDocumentType().name(),
                document.getSpecialty() == null ? null : document.getSpecialty().name(),
                document.getDocumentDate(),
                document.getCreatedAt(),
                document.getSummary(),
                document.getProcessingStatus().name(),
                document.getKeywords()
        );
    }

    public UpdateDocumentAiResultCommand toCommand(UUID documentId, AiResultRequest request) {
        return new UpdateDocumentAiResultCommand(
                documentId,
                toDocumentType(request.documentType()),
                toMedicalSpecialty(request.specialty()),
                request.documentDate(),
                request.summary(),
                request.keywords(),
                request.confidence(),
                toProcessingStatus(request.status())
        );
    }

    private DocumentType toDocumentType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return DocumentType.valueOf(normalizeEnumValue(value));
    }

    private MedicalSpecialty toMedicalSpecialty(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return MedicalSpecialty.valueOf(normalizeEnumValue(value));
    }

    private DocumentProcessingStatus toProcessingStatus(String value) {
        if (value == null || value.isBlank()) {
            return DocumentProcessingStatus.PROCESSED;
        }

        return DocumentProcessingStatus.valueOf(normalizeEnumValue(value));
    }

    private String normalizeEnumValue(String value) {
        return value.trim().toUpperCase();
    }
}

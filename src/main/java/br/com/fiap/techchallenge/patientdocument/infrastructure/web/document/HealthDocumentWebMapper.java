package br.com.fiap.techchallenge.patientdocument.infrastructure.web.document;

import br.com.fiap.techchallenge.patientdocument.domain.document.HealthDocument;
import org.springframework.stereotype.Component;

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
}

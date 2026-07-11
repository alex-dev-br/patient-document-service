package br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.document;

import br.com.fiap.techchallenge.patientdocument.domain.document.DocumentProcessingStatus;
import br.com.fiap.techchallenge.patientdocument.domain.document.DocumentType;
import br.com.fiap.techchallenge.patientdocument.domain.document.HealthDocument;
import br.com.fiap.techchallenge.patientdocument.domain.document.MedicalSpecialty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HealthDocumentPersistenceMapper {

    public HealthDocumentJpaEntity toEntity(HealthDocument document) {
        return HealthDocumentJpaEntity.builder()
                .id(document.getId())
                .patientId(document.getPatientId())
                .originalFileName(document.getOriginalFileName())
                .storedFileName(document.getStoredFileName())
                .storagePath(document.getStoragePath())
                .contentType(document.getContentType())
                .fileSize(document.getFileSize())
                .documentType(document.getDocumentType() == null ? null : document.getDocumentType().name())
                .specialty(document.getSpecialty() == null ? null : document.getSpecialty().name())
                .documentDate(document.getDocumentDate())
                .summary(document.getSummary())
                .confidence(document.getConfidence())
                .processingStatus(document.getProcessingStatus().name())
                .createdAt(document.getCreatedAt())
                .processedAt(document.getProcessedAt())
                .build();
    }

    public HealthDocument toDomain(HealthDocumentJpaEntity entity, List<String> keywords) {
        return HealthDocument.restore(
                entity.getId(),
                entity.getPatientId(),
                entity.getOriginalFileName(),
                entity.getStoredFileName(),
                entity.getStoragePath(),
                entity.getContentType(),
                entity.getFileSize(),
                toDocumentType(entity.getDocumentType()),
                toMedicalSpecialty(entity.getSpecialty()),
                entity.getDocumentDate(),
                entity.getSummary(),
                entity.getConfidence(),
                DocumentProcessingStatus.valueOf(entity.getProcessingStatus()),
                entity.getCreatedAt(),
                entity.getProcessedAt(),
                keywords
        );
    }

    private DocumentType toDocumentType(String value) {
        return value == null ? null : DocumentType.valueOf(value);
    }

    private MedicalSpecialty toMedicalSpecialty(String value) {
        return value == null ? null : MedicalSpecialty.valueOf(value);
    }
}

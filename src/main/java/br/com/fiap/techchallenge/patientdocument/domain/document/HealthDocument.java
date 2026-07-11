package br.com.fiap.techchallenge.patientdocument.domain.document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class HealthDocument {

    private final UUID id;
    private final UUID patientId;
    private final String originalFileName;
    private final String storedFileName;
    private final String storagePath;
    private final String contentType;
    private final Long fileSize;
    private final DocumentType documentType;
    private final MedicalSpecialty specialty;
    private final LocalDate documentDate;
    private final String summary;
    private final BigDecimal confidence;
    private final DocumentProcessingStatus processingStatus;
    private final LocalDateTime createdAt;
    private final LocalDateTime processedAt;
    private final List<String> keywords;

    private HealthDocument(
            UUID id,
            UUID patientId,
            String originalFileName,
            String storedFileName,
            String storagePath,
            String contentType,
            Long fileSize,
            DocumentType documentType,
            MedicalSpecialty specialty,
            LocalDate documentDate,
            String summary,
            BigDecimal confidence,
            DocumentProcessingStatus processingStatus,
            LocalDateTime createdAt,
            LocalDateTime processedAt,
            List<String> keywords
    ) {
        this.id = id;
        this.patientId = patientId;
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.storagePath = storagePath;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.documentType = documentType;
        this.specialty = specialty;
        this.documentDate = documentDate;
        this.summary = summary;
        this.confidence = confidence;
        this.processingStatus = processingStatus;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
        this.keywords = keywords == null ? List.of() : List.copyOf(keywords);
    }

    public static HealthDocument createPending(
            UUID patientId,
            String originalFileName,
            String storedFileName,
            String storagePath,
            String contentType,
            Long fileSize
    ) {
        return new HealthDocument(
                UUID.randomUUID(),
                patientId,
                originalFileName,
                storedFileName,
                storagePath,
                contentType,
                fileSize,
                null,
                null,
                null,
                null,
                null,
                DocumentProcessingStatus.PENDING_PROCESSING,
                LocalDateTime.now(),
                null,
                List.of()
        );
    }

    public static HealthDocument restore(
            UUID id,
            UUID patientId,
            String originalFileName,
            String storedFileName,
            String storagePath,
            String contentType,
            Long fileSize,
            DocumentType documentType,
            MedicalSpecialty specialty,
            LocalDate documentDate,
            String summary,
            BigDecimal confidence,
            DocumentProcessingStatus processingStatus,
            LocalDateTime createdAt,
            LocalDateTime processedAt,
            List<String> keywords
    ) {
        return new HealthDocument(
                id,
                patientId,
                originalFileName,
                storedFileName,
                storagePath,
                contentType,
                fileSize,
                documentType,
                specialty,
                documentDate,
                summary,
                confidence,
                processingStatus,
                createdAt,
                processedAt,
                keywords
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public MedicalSpecialty getSpecialty() {
        return specialty;
    }

    public LocalDate getDocumentDate() {
        return documentDate;
    }

    public String getSummary() {
        return summary;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public DocumentProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public List<String> getKeywords() {
        return keywords;
    }
}

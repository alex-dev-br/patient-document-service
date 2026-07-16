package br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_processing_outbox")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DocumentProcessingOutboxJpaEntity {

    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DocumentProcessingOutboxStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "error_detail", length = 2000)
    private String errorDetail;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    public void registerAttempt() {
        this.attemptCount++;
    }

    public void markPublished() {
        this.status = DocumentProcessingOutboxStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
        this.errorDetail = null;
    }

    public void markFailed(String errorDetail) {
        this.status = DocumentProcessingOutboxStatus.FAILED;
        this.errorDetail = truncate(errorDetail);
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }

        return value.substring(0, Math.min(value.length(), 2000));
    }
}

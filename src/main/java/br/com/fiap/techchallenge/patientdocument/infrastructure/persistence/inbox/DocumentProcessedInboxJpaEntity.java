package br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.inbox;

import br.com.fiap.techchallenge.patientdocument.domain.document.DocumentProcessingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "document_processed_inbox",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_processed_inbox_event_result",
                        columnNames = {
                                "event_id",
                                "external_result_id"
                        }
                )
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DocumentProcessedInboxJpaEntity {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(
            name = "external_result_id",
            nullable = false,
            length = 64
    )
    private String externalResultId;

    @Column(name = "external_document_type", length = 100)
    private String externalDocumentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false, length = 30)
    private DocumentProcessingStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "error_detail", length = 2000)
    private String errorDetail;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;
}

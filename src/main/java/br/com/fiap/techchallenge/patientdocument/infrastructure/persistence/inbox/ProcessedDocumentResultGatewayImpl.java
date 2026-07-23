package br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.inbox;

import br.com.fiap.techchallenge.patientdocument.application.document.gateway.ProcessedDocumentResultGateway;
import br.com.fiap.techchallenge.patientdocument.application.document.result.ProcessedDocumentResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProcessedDocumentResultGatewayImpl
        implements ProcessedDocumentResultGateway {

    private final DocumentProcessedInboxJpaRepository repository;

    @Override
    public boolean existsByEventIdAndExternalResultId(
            UUID eventId,
            String externalResultId
    ) {
        return repository.existsByEventIdAndExternalResultId(
                eventId,
                externalResultId
        );
    }

    @Override
    public ProcessedDocumentResult save(
            ProcessedDocumentResult result
    ) {
        DocumentProcessedInboxJpaEntity entity =
                DocumentProcessedInboxJpaEntity.builder()
                        .id(result.id())
                        .schemaVersion(result.schemaVersion())
                        .occurredAt(result.occurredAt())
                        .eventId(result.eventId())
                        .correlationId(result.correlationId())
                        .documentId(result.documentId())
                        .patientId(result.patientId())
                        .externalResultId(
                                result.externalResultId()
                        )
                        .externalDocumentType(
                                result.externalDocumentType()
                        )
                        .documentDate(result.documentDate())
                        .status(result.status())
                        .payload(result.payload())
                        .errorCode(result.errorCode())
                        .errorDetail(result.errorDetail())
                        .errorRetryable(
                                result.errorRetryable()
                        )
                        .receivedAt(result.receivedAt())
                        .build();

        return toDomain(repository.save(entity));
    }

    @Override
    public List<ProcessedDocumentResult> findByDocumentId(
            UUID documentId
    ) {
        return repository
                .findByDocumentIdOrderByReceivedAtAsc(
                        documentId
                )
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private ProcessedDocumentResult toDomain(
            DocumentProcessedInboxJpaEntity entity
    ) {
        return new ProcessedDocumentResult(
                entity.getId(),
                entity.getSchemaVersion(),
                entity.getOccurredAt(),
                entity.getEventId(),
                entity.getCorrelationId(),
                entity.getDocumentId(),
                entity.getPatientId(),
                entity.getExternalResultId(),
                entity.getExternalDocumentType(),
                entity.getDocumentDate(),
                entity.getStatus(),
                entity.getPayload(),
                entity.getErrorCode(),
                entity.getErrorDetail(),
                entity.getErrorRetryable(),
                entity.getReceivedAt()
        );
    }
}

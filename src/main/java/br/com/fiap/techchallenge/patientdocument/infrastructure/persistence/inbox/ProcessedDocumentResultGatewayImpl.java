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
                        .eventId(result.eventId())
                        .documentId(result.documentId())
                        .patientId(result.patientId())
                        .externalResultId(result.externalResultId())
                        .externalDocumentType(
                                result.externalDocumentType()
                        )
                        .status(result.status())
                        .payload(result.payload())
                        .errorDetail(result.errorDetail())
                        .receivedAt(result.receivedAt())
                        .build();

        return toDomain(repository.save(entity));
    }

    @Override
    public List<ProcessedDocumentResult> findByDocumentId(
            UUID documentId
    ) {
        return repository
                .findByDocumentIdOrderByReceivedAtAsc(documentId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private ProcessedDocumentResult toDomain(
            DocumentProcessedInboxJpaEntity entity
    ) {
        return new ProcessedDocumentResult(
                entity.getId(),
                entity.getEventId(),
                entity.getDocumentId(),
                entity.getPatientId(),
                entity.getExternalResultId(),
                entity.getExternalDocumentType(),
                entity.getStatus(),
                entity.getPayload(),
                entity.getErrorDetail(),
                entity.getReceivedAt()
        );
    }
}

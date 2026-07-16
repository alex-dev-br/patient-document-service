package br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.outbox;

import br.com.fiap.techchallenge.patientdocument.application.document.event.DocumentProcessingRequestedEvent;
import br.com.fiap.techchallenge.patientdocument.application.document.gateway.DocumentProcessingEventGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DocumentProcessingOutboxGatewayImpl
        implements DocumentProcessingEventGateway {

    private final DocumentProcessingOutboxJpaRepository repository;

    @Override
    public void enqueue(DocumentProcessingRequestedEvent event) {
        repository.save(
                DocumentProcessingOutboxJpaEntity.builder()
                        .eventId(event.eventId())
                        .documentId(event.documentId())
                        .patientId(event.patientId())
                        .status(DocumentProcessingOutboxStatus.PENDING)
                        .attemptCount(0)
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }

    @Override
    public boolean existsByEventIdAndDocumentIdAndPatientId(
            UUID eventId,
            UUID documentId,
            UUID patientId
    ) {
        return repository.existsByEventIdAndDocumentIdAndPatientId(
                eventId,
                documentId,
                patientId
        );
    }
}

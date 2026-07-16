package br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.inbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentProcessedInboxJpaRepository
        extends JpaRepository<DocumentProcessedInboxJpaEntity, UUID> {

    boolean existsByEventIdAndExternalResultId(
            UUID eventId,
            String externalResultId
    );

    List<DocumentProcessedInboxJpaEntity>
    findByDocumentIdOrderByReceivedAtAsc(UUID documentId);
}

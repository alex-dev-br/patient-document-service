package br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DocumentProcessingOutboxJpaRepository
        extends JpaRepository<DocumentProcessingOutboxJpaEntity, UUID> {

    List<DocumentProcessingOutboxJpaEntity>
    findTop20ByStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(
            Collection<DocumentProcessingOutboxStatus> statuses,
            int maxAttempts
    );

    boolean existsByEventIdAndDocumentIdAndPatientId(
            UUID eventId,
            UUID documentId,
            UUID patientId
    );
}

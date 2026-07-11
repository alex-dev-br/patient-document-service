package br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DocumentKeywordJpaRepository extends JpaRepository<DocumentKeywordJpaEntity, UUID> {

    List<DocumentKeywordJpaEntity> findByDocumentId(UUID documentId);

    List<DocumentKeywordJpaEntity> findByDocumentIdIn(Collection<UUID> documentIds);

    void deleteByDocumentId(UUID documentId);
}

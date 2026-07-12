package br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface HealthDocumentJpaRepository extends
        JpaRepository<HealthDocumentJpaEntity, UUID>,
        JpaSpecificationExecutor<HealthDocumentJpaEntity> {

    List<HealthDocumentJpaEntity> findByPatientIdOrderByCreatedAtDesc(UUID patientId);
}

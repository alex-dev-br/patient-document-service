package br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HealthDocumentJpaRepository extends JpaRepository<HealthDocumentJpaEntity, UUID> {

    List<HealthDocumentJpaEntity> findByPatientIdOrderByCreatedAtDesc(UUID patientId);
}

package br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccessAuditJpaRepository
        extends JpaRepository<AccessAuditJpaEntity, UUID> {
}
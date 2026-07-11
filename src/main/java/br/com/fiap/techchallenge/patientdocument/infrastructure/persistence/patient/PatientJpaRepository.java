package br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.patient;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PatientJpaRepository extends JpaRepository<PatientJpaEntity, UUID> {
}

package br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.patient;

import br.com.fiap.techchallenge.patientdocument.application.patient.gateway.PatientGateway;
import br.com.fiap.techchallenge.patientdocument.domain.patient.Patient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PatientGatewayImpl implements PatientGateway {

    private final PatientJpaRepository patientJpaRepository;
    private final PatientPersistenceMapper patientPersistenceMapper;

    @Override
    public Patient save(Patient patient) {
        PatientJpaEntity entity = patientPersistenceMapper.toEntity(patient);
        PatientJpaEntity savedEntity = patientJpaRepository.save(entity);
        return patientPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Patient> findById(UUID id) {
        return patientJpaRepository.findById(id)
                .map(patientPersistenceMapper::toDomain);
    }

    @Override
    public List<Patient> findAll() {
        return patientJpaRepository.findAll()
                .stream()
                .map(patientPersistenceMapper::toDomain)
                .toList();
    }
}

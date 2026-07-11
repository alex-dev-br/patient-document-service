package br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.patient;

import br.com.fiap.techchallenge.patientdocument.domain.patient.Patient;
import org.springframework.stereotype.Component;

@Component
public class PatientPersistenceMapper {

    public PatientJpaEntity toEntity(Patient patient) {
        return PatientJpaEntity.builder()
                .id(patient.getId())
                .name(patient.getName())
                .birthDate(patient.getBirthDate())
                .cpf(patient.getCpf())
                .email(patient.getEmail())
                .phone(patient.getPhone())
                .createdAt(patient.getCreatedAt())
                .updatedAt(patient.getUpdatedAt())
                .build();
    }

    public Patient toDomain(PatientJpaEntity entity) {
        return Patient.restore(
                entity.getId(),
                entity.getName(),
                entity.getBirthDate(),
                entity.getCpf(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}

package br.com.fiap.techchallenge.patientdocument.infrastructure.web.patient;

import java.time.LocalDate;
import java.util.UUID;

public record PatientResponse(
        UUID id,
        String name,
        LocalDate birthDate,
        String cpf,
        String email,
        String phone
) {
}

package br.com.fiap.techchallenge.patientdocument.application.patient.command;

import java.time.LocalDate;

public record CreatePatientCommand(
        String name,
        LocalDate birthDate,
        String cpf,
        String email,
        String phone
) {
}

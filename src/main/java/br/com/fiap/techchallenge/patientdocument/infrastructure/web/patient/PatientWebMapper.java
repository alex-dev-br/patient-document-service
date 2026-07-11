package br.com.fiap.techchallenge.patientdocument.infrastructure.web.patient;

import br.com.fiap.techchallenge.patientdocument.application.patient.command.CreatePatientCommand;
import br.com.fiap.techchallenge.patientdocument.domain.patient.Patient;
import org.springframework.stereotype.Component;

@Component
public class PatientWebMapper {

    public CreatePatientCommand toCommand(CreatePatientRequest request) {
        return new CreatePatientCommand(
                request.name(),
                request.birthDate(),
                request.cpf(),
                request.email(),
                request.phone()
        );
    }

    public PatientResponse toResponse(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getName(),
                patient.getBirthDate(),
                patient.getCpf(),
                patient.getEmail(),
                patient.getPhone()
        );
    }
}

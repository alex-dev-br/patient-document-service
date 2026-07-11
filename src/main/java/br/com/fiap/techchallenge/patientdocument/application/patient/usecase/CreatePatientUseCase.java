package br.com.fiap.techchallenge.patientdocument.application.patient.usecase;

import br.com.fiap.techchallenge.patientdocument.application.patient.command.CreatePatientCommand;
import br.com.fiap.techchallenge.patientdocument.application.patient.gateway.PatientGateway;
import br.com.fiap.techchallenge.patientdocument.domain.patient.Patient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreatePatientUseCase {

    private final PatientGateway patientGateway;

    @Transactional
    public Patient execute(CreatePatientCommand command) {
        Patient patient = Patient.create(
                command.name(),
                command.birthDate(),
                command.cpf(),
                command.email(),
                command.phone()
        );

        return patientGateway.save(patient);
    }
}

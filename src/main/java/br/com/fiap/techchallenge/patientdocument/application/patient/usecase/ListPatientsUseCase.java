package br.com.fiap.techchallenge.patientdocument.application.patient.usecase;

import br.com.fiap.techchallenge.patientdocument.application.patient.gateway.PatientGateway;
import br.com.fiap.techchallenge.patientdocument.domain.patient.Patient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListPatientsUseCase {

    private final PatientGateway patientGateway;

    @Transactional(readOnly = true)
    public List<Patient> execute() {
        return patientGateway.findAll();
    }
}

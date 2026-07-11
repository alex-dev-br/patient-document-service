package br.com.fiap.techchallenge.patientdocument.application.patient.usecase;

import br.com.fiap.techchallenge.patientdocument.application.exception.ResourceNotFoundException;
import br.com.fiap.techchallenge.patientdocument.application.patient.gateway.PatientGateway;
import br.com.fiap.techchallenge.patientdocument.domain.patient.Patient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FindPatientByIdUseCase {

    private final PatientGateway patientGateway;

    @Transactional(readOnly = true)
    public Patient execute(UUID id) {
        return patientGateway.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado: " + id));
    }
}

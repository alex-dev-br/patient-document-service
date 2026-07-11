package br.com.fiap.techchallenge.patientdocument.application.document.usecase;

import br.com.fiap.techchallenge.patientdocument.application.document.gateway.HealthDocumentGateway;
import br.com.fiap.techchallenge.patientdocument.application.exception.ResourceNotFoundException;
import br.com.fiap.techchallenge.patientdocument.application.patient.gateway.PatientGateway;
import br.com.fiap.techchallenge.patientdocument.domain.document.HealthDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListPatientDocumentsUseCase {

    private final PatientGateway patientGateway;
    private final HealthDocumentGateway healthDocumentGateway;

    @Transactional(readOnly = true)
    public List<HealthDocument> execute(UUID patientId) {
        patientGateway.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado: " + patientId));

        return healthDocumentGateway.findByPatientId(patientId);
    }
}

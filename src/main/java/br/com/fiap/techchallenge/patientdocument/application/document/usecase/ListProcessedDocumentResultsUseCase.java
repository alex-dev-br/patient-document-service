package br.com.fiap.techchallenge.patientdocument.application.document.usecase;

import br.com.fiap.techchallenge.patientdocument.application.document.gateway.HealthDocumentGateway;
import br.com.fiap.techchallenge.patientdocument.application.document.gateway.ProcessedDocumentResultGateway;
import br.com.fiap.techchallenge.patientdocument.application.document.result.ProcessedDocumentResult;
import br.com.fiap.techchallenge.patientdocument.application.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListProcessedDocumentResultsUseCase {

    private final HealthDocumentGateway healthDocumentGateway;
    private final ProcessedDocumentResultGateway processedDocumentResultGateway;

    @Transactional(readOnly = true)
    public List<ProcessedDocumentResult> execute(UUID documentId) {
        Objects.requireNonNull(
                documentId,
                "O identificador do documento é obrigatório."
        );

        if (healthDocumentGateway.findById(documentId).isEmpty()) {
            throw new ResourceNotFoundException(
                    "Documento não encontrado: " + documentId
            );
        }

        return processedDocumentResultGateway.findByDocumentId(documentId);
    }
}

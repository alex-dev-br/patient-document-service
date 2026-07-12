package br.com.fiap.techchallenge.patientdocument.application.document.usecase;

import br.com.fiap.techchallenge.patientdocument.application.document.command.UpdateDocumentAiResultCommand;
import br.com.fiap.techchallenge.patientdocument.application.document.gateway.HealthDocumentGateway;
import br.com.fiap.techchallenge.patientdocument.application.exception.ResourceNotFoundException;
import br.com.fiap.techchallenge.patientdocument.domain.document.DocumentProcessingStatus;
import br.com.fiap.techchallenge.patientdocument.domain.document.HealthDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class UpdateDocumentAiResultUseCase {

    private final HealthDocumentGateway healthDocumentGateway;

    @Transactional
    public HealthDocument execute(UpdateDocumentAiResultCommand command) {
        HealthDocument document = healthDocumentGateway.findById(command.documentId())
                .orElseThrow(() -> new ResourceNotFoundException("Documento não encontrado: " + command.documentId()));

        validate(command);

        HealthDocument updatedDocument = document.applyAiResult(
                command.documentType(),
                command.specialty(),
                command.documentDate(),
                command.summary(),
                command.keywords(),
                command.confidence(),
                command.status()
        );

        return healthDocumentGateway.save(updatedDocument);
    }

    private void validate(UpdateDocumentAiResultCommand command) {
        if (command.status() == DocumentProcessingStatus.PROCESSED && command.summary() == null) {
            throw new IllegalArgumentException("O resumo é obrigatório para documentos processados.");
        }

        if (command.confidence() != null
                && (command.confidence().compareTo(BigDecimal.ZERO) < 0
                || command.confidence().compareTo(BigDecimal.ONE) > 0)) {
            throw new IllegalArgumentException("A confiança deve estar entre 0 e 1.");
        }
    }
}

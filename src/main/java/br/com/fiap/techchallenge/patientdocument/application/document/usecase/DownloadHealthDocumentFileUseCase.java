package br.com.fiap.techchallenge.patientdocument.application.document.usecase;

import br.com.fiap.techchallenge.patientdocument.application.document.gateway.HealthDocumentGateway;
import br.com.fiap.techchallenge.patientdocument.application.document.result.HealthDocumentFile;
import br.com.fiap.techchallenge.patientdocument.application.exception.ResourceNotFoundException;
import br.com.fiap.techchallenge.patientdocument.application.storage.gateway.StorageGateway;
import br.com.fiap.techchallenge.patientdocument.application.storage.result.StoredFileContent;
import br.com.fiap.techchallenge.patientdocument.domain.document.HealthDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DownloadHealthDocumentFileUseCase {

    private final HealthDocumentGateway healthDocumentGateway;
    private final StorageGateway storageGateway;

    @Transactional(readOnly = true)
    public HealthDocumentFile execute(UUID documentId) {
        HealthDocument document = healthDocumentGateway.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento não encontrado: " + documentId));

        StoredFileContent storedFileContent = storageGateway.load(document.getStoragePath());

        return new HealthDocumentFile(
                document.getOriginalFileName(),
                document.getContentType(),
                storedFileContent.size(),
                storedFileContent.content()
        );
    }
}

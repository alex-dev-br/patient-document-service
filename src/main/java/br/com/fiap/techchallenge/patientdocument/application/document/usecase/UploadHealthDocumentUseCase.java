package br.com.fiap.techchallenge.patientdocument.application.document.usecase;

import br.com.fiap.techchallenge.patientdocument.application.document.command.UploadHealthDocumentCommand;
import br.com.fiap.techchallenge.patientdocument.application.document.gateway.HealthDocumentGateway;
import br.com.fiap.techchallenge.patientdocument.application.exception.ResourceNotFoundException;
import br.com.fiap.techchallenge.patientdocument.application.patient.gateway.PatientGateway;
import br.com.fiap.techchallenge.patientdocument.application.storage.command.StoreFileCommand;
import br.com.fiap.techchallenge.patientdocument.application.storage.gateway.StorageGateway;
import br.com.fiap.techchallenge.patientdocument.application.storage.result.StoredFile;
import br.com.fiap.techchallenge.patientdocument.domain.document.HealthDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UploadHealthDocumentUseCase {

    private final PatientGateway patientGateway;
    private final StorageGateway storageGateway;
    private final HealthDocumentGateway healthDocumentGateway;

    @Transactional
    public HealthDocument execute(UploadHealthDocumentCommand command) {
        patientGateway.findById(command.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado: " + command.patientId()));

        if (command.fileSize() == null || command.fileSize() <= 0) {
            throw new IllegalArgumentException("O arquivo enviado está vazio.");
        }

        StoredFile storedFile = storageGateway.store(new StoreFileCommand(
                command.originalFileName(),
                command.contentType(),
                command.fileSize(),
                command.inputStream()
        ));

        HealthDocument document = HealthDocument.createPending(
                command.patientId(),
                storedFile.originalFileName(),
                storedFile.storedFileName(),
                storedFile.storagePath(),
                storedFile.contentType(),
                storedFile.fileSize()
        );

        return healthDocumentGateway.save(document);
    }
}

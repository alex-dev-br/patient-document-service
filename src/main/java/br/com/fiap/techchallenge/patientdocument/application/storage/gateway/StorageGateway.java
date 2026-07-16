package br.com.fiap.techchallenge.patientdocument.application.storage.gateway;

import br.com.fiap.techchallenge.patientdocument.application.storage.command.StoreFileCommand;
import br.com.fiap.techchallenge.patientdocument.application.storage.result.StoredFile;
import br.com.fiap.techchallenge.patientdocument.application.storage.result.StoredFileContent;

public interface StorageGateway {

    StoredFile store(StoreFileCommand command);

    StoredFileContent load(String storagePath);

    void delete(String storagePath);
}

package br.com.fiap.techchallenge.patientdocument.application.document.gateway;

import br.com.fiap.techchallenge.patientdocument.application.document.result.ProcessedDocumentResult;

import java.util.List;
import java.util.UUID;

public interface ProcessedDocumentResultGateway {

    boolean existsByEventIdAndExternalResultId(
            UUID eventId,
            String externalResultId
    );

    ProcessedDocumentResult save(ProcessedDocumentResult result);

    List<ProcessedDocumentResult> findByDocumentId(UUID documentId);
}

package br.com.fiap.techchallenge.patientdocument.infrastructure.web.document;

import br.com.fiap.techchallenge.patientdocument.application.document.result.ProcessedDocumentResult;
import org.springframework.stereotype.Component;

@Component
public class ProcessedDocumentResultWebMapper {

    public ProcessedDocumentResultResponse toResponse(
            ProcessedDocumentResult result
    ) {
        return new ProcessedDocumentResultResponse(
                result.eventId(),
                result.documentId(),
                result.patientId(),
                result.externalResultId(),
                result.externalDocumentType(),
                result.status().name(),
                result.payload(),
                result.errorDetail(),
                result.receivedAt()
        );
    }
}

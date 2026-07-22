package br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka;

import br.com.fiap.techchallenge.patientdocument.application.document.command.ProcessDocumentProcessedResponseCommand;
import br.com.fiap.techchallenge.patientdocument.application.document.usecase.ProcessDocumentProcessedResponseUseCase;
import br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka.validation.DocumentProcessedResponseValidator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.messaging.kafka.enabled",
        havingValue = "true"
)
public class DocumentProcessedResponseListener {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    DocumentProcessedResponseListener.class
            );

    private final ProcessDocumentProcessedResponseUseCase useCase;
    private final DocumentProcessedResponseValidator validator;

    @KafkaListener(
            topics = "${app.messaging.kafka.topics.processed-response}"
    )
    public void consume(DocumentProcessedResponseMessage message) {
        validator.validate(message);

        boolean legacy = validator.isLegacy(message);

        LOGGER.info(
                "action=documentProcessedResponseReceived, "
                        + "eventId={}, documentId={}, "
                        + "schemaVersion={}, legacy={}, status={}",
                message.eventId(),
                message.documentId(),
                message.schemaVersion(),
                legacy,
                message.status()
        );

        useCase.execute(
                new ProcessDocumentProcessedResponseCommand(
                        message.schemaVersion(),
                        message.occurredAt(),
                        message.eventId(),
                        message.documentId(),
                        message.patientId(),
                        message.status(),
                        message.document(),
                        resolveErrorCode(message),
                        resolveErrorDetail(message),
                        resolveErrorRetryable(message)
                )
        );
    }

    private String resolveErrorCode(
            DocumentProcessedResponseMessage message
    ) {
        DocumentProcessingErrorMessage error = message.error();
        return error == null ? null : error.code();
    }

    private String resolveErrorDetail(
            DocumentProcessedResponseMessage message
    ) {
        DocumentProcessingErrorMessage error = message.error();

        return error == null
                ? message.errorDetail()
                : error.message();
    }

    private Boolean resolveErrorRetryable(
            DocumentProcessedResponseMessage message
    ) {
        DocumentProcessingErrorMessage error = message.error();
        return error == null ? null : error.retryable();
    }
}

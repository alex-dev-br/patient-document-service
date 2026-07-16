package br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka;

import br.com.fiap.techchallenge.patientdocument.application.document.command.ProcessDocumentProcessedResponseCommand;
import br.com.fiap.techchallenge.patientdocument.application.document.usecase.ProcessDocumentProcessedResponseUseCase;
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

    @KafkaListener(
            topics = "${app.messaging.kafka.topics.processed-response}"
    )
    public void consume(DocumentProcessedResponseMessage message) {
        LOGGER.info(
                "Recebendo resultado de processamento. eventId={}, documentId={}",
                message.eventId(),
                message.documentId()
        );

        useCase.execute(
                new ProcessDocumentProcessedResponseCommand(
                        message.eventId(),
                        message.documentId(),
                        message.patientId(),
                        message.status(),
                        message.document(),
                        message.errorDetail()
                )
        );
    }
}

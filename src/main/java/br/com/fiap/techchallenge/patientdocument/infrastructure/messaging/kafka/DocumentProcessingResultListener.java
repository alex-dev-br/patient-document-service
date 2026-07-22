package br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka;

import br.com.fiap.techchallenge.patientdocument.application.document.command.ProcessDocumentProcessingResultCommand;
import br.com.fiap.techchallenge.patientdocument.application.document.command.ProcessDocumentProcessingResultItemCommand;
import br.com.fiap.techchallenge.patientdocument.application.document.usecase.ProcessDocumentProcessingResultUseCase;
import br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka.validation.DocumentProcessingResultValidator;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.messaging.kafka.enabled",
        havingValue = "true"
)
public class DocumentProcessingResultListener {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    DocumentProcessingResultListener.class
            );

    private static final JsonMapper STRICT_JSON_MAPPER =
            JsonMapper
                    .builder()
                    .findAndAddModules()
                    .enable(
                            DeserializationFeature
                                    .FAIL_ON_UNKNOWN_PROPERTIES
                    )
                    .build();

    private final ProcessDocumentProcessingResultUseCase useCase;
    private final DocumentProcessingResultValidator validator;

    @KafkaListener(
            topics = "${app.messaging.kafka.topics.processing-result}",
            properties = {
                    "value.deserializer="
                            + "org.apache.kafka.common.serialization."
                            + "StringDeserializer"
            }
    )
    public void consume(
            ConsumerRecord<String, String> record
    ) {
        DocumentProcessingResultMessage message =
                deserialize(record.value());

        validator.validate(message);
        validateKafkaKey(record.key(), message.documentId());

        LOGGER.info(
                "action=documentProcessingResultReceived, "
                        + "eventType={}, eventId={}, "
                        + "correlationId={}, documentId={}, "
                        + "patientId={}, resultCount={}",
                message.eventType(),
                message.eventId(),
                message.correlationId(),
                message.documentId(),
                message.patientId(),
                resolveResultCount(message)
        );

        useCase.execute(toCommand(message));
    }

    private DocumentProcessingResultMessage deserialize(
            String payload
    ) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException(
                    "O payload Kafka é obrigatório."
            );
        }

        try {
            return STRICT_JSON_MAPPER.readValue(
                    payload,
                    DocumentProcessingResultMessage.class
            );
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "O payload Kafka não corresponde ao "
                            + "contrato de resultado agregado.",
                    exception
            );
        }
    }

    private void validateKafkaKey(
            String key,
            UUID documentId
    ) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(
                    "A chave Kafka é obrigatória."
            );
        }

        if (!documentId.toString().equals(key)) {
            throw new IllegalArgumentException(
                    "A chave Kafka não corresponde "
                            + "ao documentId da mensagem."
            );
        }
    }

    private ProcessDocumentProcessingResultCommand toCommand(
            DocumentProcessingResultMessage message
    ) {
        DocumentProcessingErrorMessage error =
                message.error();

        return new ProcessDocumentProcessingResultCommand(
                message.schemaVersion(),
                message.eventType(),
                message.eventId(),
                message.correlationId(),
                message.occurredAt(),
                message.documentId(),
                message.patientId(),
                message.summary(),
                message.primaryDocumentType(),
                message.specialty(),
                message.documentDate(),
                message.confidence(),
                mapResults(message.results()),
                error == null ? null : error.code(),
                error == null ? null : error.message(),
                error == null ? null : error.retryable()
        );
    }

    private List<ProcessDocumentProcessingResultItemCommand>
    mapResults(
            List<DocumentProcessingResultItemMessage> results
    ) {
        if (results == null) {
            return null;
        }

        return results
                .stream()
                .map(
                        item ->
                                new ProcessDocumentProcessingResultItemCommand(
                                        item.resultId(),
                                        item.documentType(),
                                        item.documentDate(),
                                        item.data()
                                )
                )
                .toList();
    }

    private int resolveResultCount(
            DocumentProcessingResultMessage message
    ) {
        return message.results() == null
                ? 0
                : message.results().size();
    }
}

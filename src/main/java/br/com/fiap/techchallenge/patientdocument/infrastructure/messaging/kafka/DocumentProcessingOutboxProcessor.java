package br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka;

import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.document.HealthDocumentJpaRepository;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.outbox.DocumentProcessingOutboxJpaRepository;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.outbox.DocumentProcessingOutboxStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@ConditionalOnProperty(
        name = "app.messaging.kafka.enabled",
        havingValue = "true"
)
public class DocumentProcessingOutboxProcessor {

    private final DocumentProcessingOutboxJpaRepository repository;
    private final HealthDocumentJpaRepository healthDocumentJpaRepository;
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final String topic;
    private final String documentFileBaseUrl;
    private final int sendTimeoutSeconds;

    public DocumentProcessingOutboxProcessor(
            DocumentProcessingOutboxJpaRepository repository,
            HealthDocumentJpaRepository healthDocumentJpaRepository,
            KafkaTemplate<Object, Object> kafkaTemplate,
            @Value("${app.messaging.kafka.topics.processing-requested}")
            String topic,
            @Value("${app.messaging.kafka.document-file-base-url}")
            String documentFileBaseUrl,
            @Value("${app.messaging.kafka.send-timeout-seconds}")
            int sendTimeoutSeconds
    ) {
        this.repository = repository;
        this.healthDocumentJpaRepository = healthDocumentJpaRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.documentFileBaseUrl = documentFileBaseUrl;
        this.sendTimeoutSeconds = sendTimeoutSeconds;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(UUID eventId) {
        var outboxEvent = repository.findById(eventId)
                .orElse(null);

        if (outboxEvent == null
                || outboxEvent.getStatus()
                == DocumentProcessingOutboxStatus.PUBLISHED) {
            return;
        }

        outboxEvent.registerAttempt();

        var message = new DocumentProcessingRequestedMessage(
                outboxEvent.getEventId(),
                outboxEvent.getDocumentId(),
                outboxEvent.getPatientId(),
                buildFileUrl(outboxEvent.getDocumentId())
        );

        try {
            kafkaTemplate.send(
                            topic,
                            outboxEvent.getDocumentId().toString(),
                            message
                    )
                    .get(sendTimeoutSeconds, TimeUnit.SECONDS);

            outboxEvent.markPublished();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            outboxEvent.markFailed(resolveErrorMessage(exception));
        } catch (ExecutionException
                 | TimeoutException
                 | RuntimeException exception) {
            outboxEvent.markFailed(resolveErrorMessage(exception));
        }
    }

    private String buildFileUrl(UUID documentId) {
        return healthDocumentJpaRepository
                .findById(documentId)
                .orElseThrow(() -> new RuntimeException("Documento sem path"))
                .getStoragePath();
    }

    private String resolveErrorMessage(Throwable throwable) {
        Throwable cause = throwable.getCause();

        if (cause != null && cause.getMessage() != null) {
            return cause.getMessage();
        }

        if (throwable.getMessage() != null) {
            return throwable.getMessage();
        }

        return throwable.getClass().getSimpleName();
    }
}

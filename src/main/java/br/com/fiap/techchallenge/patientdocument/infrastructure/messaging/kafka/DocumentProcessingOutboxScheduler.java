package br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka;

import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.outbox.DocumentProcessingOutboxJpaRepository;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.outbox.DocumentProcessingOutboxStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(
        name = "app.messaging.kafka.enabled",
        havingValue = "true"
)
public class DocumentProcessingOutboxScheduler {

    private final DocumentProcessingOutboxJpaRepository repository;
    private final DocumentProcessingOutboxProcessor processor;
    private final int maxAttempts;

    public DocumentProcessingOutboxScheduler(
            DocumentProcessingOutboxJpaRepository repository,
            DocumentProcessingOutboxProcessor processor,
            @Value("${app.messaging.kafka.max-attempts}")
            int maxAttempts
    ) {
        this.repository = repository;
        this.processor = processor;
        this.maxAttempts = maxAttempts;
    }

    @Scheduled(
            fixedDelayString =
                    "${app.messaging.kafka.outbox-fixed-delay}"
    )
    public void publishPendingEvents() {
        repository
                .findTop20ByStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(
                        List.of(
                                DocumentProcessingOutboxStatus.PENDING,
                                DocumentProcessingOutboxStatus.FAILED
                        ),
                        maxAttempts
                )
                .stream()
                .map(event -> event.getEventId())
                .forEach(processor::process);
    }
}

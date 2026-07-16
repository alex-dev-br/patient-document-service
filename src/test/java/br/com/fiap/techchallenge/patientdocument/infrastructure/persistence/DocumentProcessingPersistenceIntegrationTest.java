package br.com.fiap.techchallenge.patientdocument.infrastructure.persistence;

import br.com.fiap.techchallenge.patientdocument.TestcontainersConfiguration;
import br.com.fiap.techchallenge.patientdocument.application.document.event.DocumentProcessingRequestedEvent;
import br.com.fiap.techchallenge.patientdocument.application.document.gateway.DocumentProcessingEventGateway;
import br.com.fiap.techchallenge.patientdocument.application.document.gateway.ProcessedDocumentResultGateway;
import br.com.fiap.techchallenge.patientdocument.application.document.result.ProcessedDocumentResult;
import br.com.fiap.techchallenge.patientdocument.domain.document.DocumentProcessingStatus;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.inbox.DocumentProcessedInboxJpaRepository;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.outbox.DocumentProcessingOutboxJpaEntity;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.outbox.DocumentProcessingOutboxJpaRepository;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.outbox.DocumentProcessingOutboxStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        properties = {
                "app.messaging.kafka.enabled=false"
        }
)
@Import(TestcontainersConfiguration.class)
@Transactional
class DocumentProcessingPersistenceIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DocumentProcessingEventGateway documentProcessingEventGateway;

    @Autowired
    private DocumentProcessingOutboxJpaRepository outboxRepository;

    @Autowired
    private ProcessedDocumentResultGateway processedDocumentResultGateway;

    @Autowired
    private DocumentProcessedInboxJpaRepository inboxRepository;

    private UUID patientId;
    private UUID documentId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        documentId = UUID.randomUUID();

        insertPatient();
        insertHealthDocument();
    }

    @Test
    void shouldPersistPendingOutboxEventAndFindCorrelation() {
        UUID eventId = UUID.randomUUID();

        documentProcessingEventGateway.enqueue(
                new DocumentProcessingRequestedEvent(
                        eventId,
                        documentId,
                        patientId
                )
        );

        outboxRepository.flush();
        entityManager.clear();

        DocumentProcessingOutboxJpaEntity savedEvent =
                outboxRepository.findById(eventId).orElseThrow();

        assertThat(savedEvent.getEventId()).isEqualTo(eventId);
        assertThat(savedEvent.getDocumentId()).isEqualTo(documentId);
        assertThat(savedEvent.getPatientId()).isEqualTo(patientId);
        assertThat(savedEvent.getStatus())
                .isEqualTo(DocumentProcessingOutboxStatus.PENDING);
        assertThat(savedEvent.getAttemptCount()).isZero();
        assertThat(savedEvent.getCreatedAt()).isNotNull();
        assertThat(savedEvent.getPublishedAt()).isNull();
        assertThat(savedEvent.getErrorDetail()).isNull();

        assertThat(
                documentProcessingEventGateway
                        .existsByEventIdAndDocumentIdAndPatientId(
                                eventId,
                                documentId,
                                patientId
                        )
        ).isTrue();
    }

    @Test
    void shouldMarkOutboxEventAsPublishedAndRemoveItFromCandidates() {
        UUID eventId = enqueueEvent();

        DocumentProcessingOutboxJpaEntity event =
                outboxRepository.findById(eventId).orElseThrow();

        event.registerAttempt();
        event.markPublished();

        outboxRepository.saveAndFlush(event);
        entityManager.clear();

        DocumentProcessingOutboxJpaEntity publishedEvent =
                outboxRepository.findById(eventId).orElseThrow();

        assertThat(publishedEvent.getStatus())
                .isEqualTo(DocumentProcessingOutboxStatus.PUBLISHED);
        assertThat(publishedEvent.getAttemptCount()).isEqualTo(1);
        assertThat(publishedEvent.getPublishedAt()).isNotNull();
        assertThat(publishedEvent.getErrorDetail()).isNull();

        List<DocumentProcessingOutboxJpaEntity> candidates =
                outboxRepository
                        .findTop20ByStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(
                                List.of(
                                        DocumentProcessingOutboxStatus.PENDING,
                                        DocumentProcessingOutboxStatus.FAILED
                                ),
                                5
                        );

        assertThat(candidates)
                .extracting(DocumentProcessingOutboxJpaEntity::getEventId)
                .doesNotContain(eventId);
    }

    @Test
    void shouldRetryFailedOutboxEventOnlyBelowMaximumAttempts() {
        UUID eventId = enqueueEvent();

        DocumentProcessingOutboxJpaEntity event =
                outboxRepository.findById(eventId).orElseThrow();

        event.registerAttempt();
        event.registerAttempt();
        event.markFailed("Falha temporária ao publicar no Kafka.");

        outboxRepository.saveAndFlush(event);
        entityManager.clear();

        DocumentProcessingOutboxJpaEntity failedEvent =
                outboxRepository.findById(eventId).orElseThrow();

        assertThat(failedEvent.getStatus())
                .isEqualTo(DocumentProcessingOutboxStatus.FAILED);
        assertThat(failedEvent.getAttemptCount()).isEqualTo(2);
        assertThat(failedEvent.getErrorDetail())
                .isEqualTo("Falha temporária ao publicar no Kafka.");

        List<DocumentProcessingOutboxJpaEntity> belowMaximum =
                findOutboxCandidates(3);

        assertThat(belowMaximum)
                .extracting(DocumentProcessingOutboxJpaEntity::getEventId)
                .contains(eventId);

        List<DocumentProcessingOutboxJpaEntity> atMaximum =
                findOutboxCandidates(2);

        assertThat(atMaximum)
                .extracting(DocumentProcessingOutboxJpaEntity::getEventId)
                .doesNotContain(eventId);
    }

    @Test
    void shouldPersistJsonPayloadAndListInboxResultsInReceivedOrder() {
        UUID eventId = UUID.randomUUID();

        LocalDateTime firstReceivedAt =
                LocalDateTime.now().minusMinutes(2);

        LocalDateTime secondReceivedAt =
                LocalDateTime.now().minusMinutes(1);

        ProcessedDocumentResult secondResult = processedResult(
                UUID.randomUUID(),
                eventId,
                "resultado-002",
                "EXAME_TSH",
                payload("Resultado de TSH"),
                secondReceivedAt
        );

        ProcessedDocumentResult firstResult = processedResult(
                UUID.randomUUID(),
                eventId,
                "resultado-001",
                "EXAME_HEMOGRAMA",
                payload("Resultado de hemograma"),
                firstReceivedAt
        );

        // Salva fora de ordem para validar a ordenação da consulta.
        processedDocumentResultGateway.save(secondResult);
        processedDocumentResultGateway.save(firstResult);

        inboxRepository.flush();
        entityManager.clear();

        List<ProcessedDocumentResult> results =
                processedDocumentResultGateway
                        .findByDocumentId(documentId);

        assertThat(results).hasSize(2);

        assertThat(results)
                .extracting(ProcessedDocumentResult::externalResultId)
                .containsExactly(
                        "resultado-001",
                        "resultado-002"
                );

        assertThat(results.get(0).payload())
                .containsEntry(
                        "summary",
                        "Resultado de hemograma"
                );

        assertThat(results.get(0).payload())
                .containsEntry(
                        "source",
                        "integration-test"
                );

        assertThat(
                processedDocumentResultGateway
                        .existsByEventIdAndExternalResultId(
                                eventId,
                                "resultado-001"
                        )
        ).isTrue();
    }

    @Test
    void shouldRejectDuplicatedExternalResultForSameEvent() {
        UUID eventId = UUID.randomUUID();

        ProcessedDocumentResult firstResult = processedResult(
                UUID.randomUUID(),
                eventId,
                "resultado-duplicado",
                "EXAME_HEMOGRAMA",
                payload("Primeiro resultado"),
                LocalDateTime.now().minusMinutes(1)
        );

        ProcessedDocumentResult duplicatedResult = processedResult(
                UUID.randomUUID(),
                eventId,
                "resultado-duplicado",
                "EXAME_HEMOGRAMA",
                payload("Resultado duplicado"),
                LocalDateTime.now()
        );

        processedDocumentResultGateway.save(firstResult);
        inboxRepository.flush();

        assertThatThrownBy(() -> {
            processedDocumentResultGateway.save(duplicatedResult);
            inboxRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    private UUID enqueueEvent() {
        UUID eventId = UUID.randomUUID();

        documentProcessingEventGateway.enqueue(
                new DocumentProcessingRequestedEvent(
                        eventId,
                        documentId,
                        patientId
                )
        );

        outboxRepository.flush();

        return eventId;
    }

    private List<DocumentProcessingOutboxJpaEntity>
    findOutboxCandidates(int maximumAttempts) {
        return outboxRepository
                .findTop20ByStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(
                        List.of(
                                DocumentProcessingOutboxStatus.PENDING,
                                DocumentProcessingOutboxStatus.FAILED
                        ),
                        maximumAttempts
                );
    }

    private ProcessedDocumentResult processedResult(
            UUID id,
            UUID eventId,
            String externalResultId,
            String externalDocumentType,
            Map<String, Object> payload,
            LocalDateTime receivedAt
    ) {
        return new ProcessedDocumentResult(
                id,
                eventId,
                documentId,
                patientId,
                externalResultId,
                externalDocumentType,
                DocumentProcessingStatus.PROCESSED,
                payload,
                null,
                receivedAt
        );
    }

    private Map<String, Object> payload(String summary) {
        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("summary", summary);
        payload.put("source", "integration-test");

        return payload;
    }

    private void insertPatient() {
        jdbcTemplate.update(
                """
                INSERT INTO patients (
                    id,
                    name,
                    birth_date,
                    cpf,
                    email,
                    phone,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                patientId,
                "Paciente de Integração",
                LocalDate.of(1990, 1, 1),
                null,
                "integration@test.local",
                null,
                LocalDateTime.now(),
                null
        );
    }

    private void insertHealthDocument() {
        jdbcTemplate.update(
                """
                INSERT INTO health_documents (
                    id,
                    patient_id,
                    original_file_name,
                    stored_file_name,
                    storage_path,
                    content_type,
                    file_size,
                    processing_status,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                documentId,
                patientId,
                "exame-integracao.pdf",
                UUID.randomUUID() + ".pdf",
                "/integration/exame-integracao.pdf",
                "application/pdf",
                1024L,
                "PENDING",
                LocalDateTime.now()
        );
    }
}

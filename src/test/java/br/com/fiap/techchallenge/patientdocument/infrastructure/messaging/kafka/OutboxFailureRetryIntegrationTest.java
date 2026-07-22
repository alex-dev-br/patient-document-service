package br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka;

import br.com.fiap.techchallenge.patientdocument.KafkaTestcontainersConfiguration;
import br.com.fiap.techchallenge.patientdocument.TestcontainersConfiguration;
import br.com.fiap.techchallenge.patientdocument.domain.document.DocumentProcessingStatus;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.document.HealthDocumentJpaRepository;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.outbox.DocumentProcessingOutboxJpaEntity;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.outbox.DocumentProcessingOutboxJpaRepository;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.outbox.DocumentProcessingOutboxStatus;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(
        properties = {
                "app.messaging.kafka.enabled=true",
                "app.messaging.kafka.outbox-fixed-delay=600000",
                "app.messaging.kafka.document-file-base-url=http://localhost:8080/",
                "app.messaging.kafka.send-timeout-seconds=10",
                "app.messaging.kafka.max-attempts=3"
        }
)
@Import({
        TestcontainersConfiguration.class,
        KafkaTestcontainersConfiguration.class
})
class OutboxFailureRetryIntegrationTest {

    private static final Duration ASYNC_TIMEOUT =
            Duration.ofSeconds(20);

    private static final String DOCUMENT_FILE_BASE_URL =
            "http://localhost:8080/";

    private static final String SIMULATED_ERROR =
            "Kafka indisponível durante a primeira tentativa.";

    private static final int MAX_ATTEMPTS = 3;

    private static final ZoneId SAO_PAULO_ZONE_ID =
            ZoneId.of("America/Sao_Paulo");

    private static final LocalDateTime OUTBOX_CREATED_AT =
            LocalDateTime.of(
                    2026,
                    7,
                    22,
                    1,
                    0
            );

    @Autowired
    private DocumentProcessingOutboxProcessor realOutboxProcessor;

    /*
     * Neutraliza somente o scheduler iniciado automaticamente
     * pelo Spring.
     *
     * O teste cria manualmente um scheduler real no momento
     * adequado, depois que o consumidor Kafka está preparado.
     */
    @MockitoBean(enforceOverride = true)
    private DocumentProcessingOutboxScheduler automaticScheduler;

    @Autowired
    private DocumentProcessingOutboxJpaRepository outboxRepository;

    @Autowired
    private HealthDocumentJpaRepository healthDocumentJpaRepository;

    @Autowired
    private KafkaContainer kafkaContainer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Value("${app.messaging.kafka.topics.processing-requested}")
    private String processingRequestedTopic;

    private UUID patientId;
    private UUID documentId;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        patientId = UUID.randomUUID();
        documentId = UUID.randomUUID();
        eventId = UUID.randomUUID();

        insertPatient();
        insertPendingDocument();
        insertPendingOutboxEvent();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void shouldFailFirstAttemptAndPublishOnSchedulerRetry() {
        KafkaTemplate<Object, Object> failingKafkaTemplate =
                createFailingKafkaTemplate();

        processFirstAttemptWithFailure(
                failingKafkaTemplate
        );

        ArgumentCaptor<DocumentProcessingRequestedMessage>
                messageCaptor =
                ArgumentCaptor.forClass(
                        DocumentProcessingRequestedMessage.class
                );

        verify(failingKafkaTemplate)
                .send(
                        eq(processingRequestedTopic),
                        eq(documentId.toString()),
                        messageCaptor.capture()
                );

        DocumentProcessingRequestedMessage firstAttemptMessage =
                messageCaptor.getValue();

        assertThat(firstAttemptMessage.schemaVersion())
                .isEqualTo(1);

        assertThat(firstAttemptMessage.eventType())
                .isEqualTo(
                        "DOCUMENT_PROCESSING_REQUESTED"
                );

        assertThat(firstAttemptMessage.occurredAt())
                .isEqualTo(expectedOccurredAt());

        assertThat(firstAttemptMessage.eventId())
                .isEqualTo(eventId);

        assertThat(firstAttemptMessage.documentId())
                .isEqualTo(documentId);

        assertThat(firstAttemptMessage.patientId())
                .isEqualTo(patientId);

        DocumentProcessingOutboxJpaEntity failedEvent =
                findOutboxEvent();

        assertThat(failedEvent.getStatus())
                .isEqualTo(
                        DocumentProcessingOutboxStatus.FAILED
                );

        assertThat(failedEvent.getAttemptCount())
                .isEqualTo(1);

        assertThat(failedEvent.getErrorDetail())
                .isEqualTo(SIMULATED_ERROR);

        assertThat(failedEvent.getPublishedAt())
                .isNull();

        try (
                KafkaConsumer<String, String> consumer =
                        createRequestedMessageConsumer()
        ) {
            subscribeAndSeekToEnd(consumer);

            /*
             * Este scheduler é real.
             *
             * Ele consulta a Outbox, encontra o evento FAILED
             * abaixo do limite de tentativas e delega ao
             * processador real da aplicação.
             */
            DocumentProcessingOutboxScheduler retryScheduler =
                    new DocumentProcessingOutboxScheduler(
                            outboxRepository,
                            realOutboxProcessor,
                            MAX_ATTEMPTS
                    );

            retryScheduler.publishPendingEvents();

            ConsumerRecord<String, String> record =
                    awaitRecord(
                            consumer,
                            ASYNC_TIMEOUT
                    );

            assertThat(record.topic())
                    .isEqualTo(processingRequestedTopic);

            assertThat(record.key())
                    .isEqualTo(documentId.toString());

            assertThat(record.value())
                    .contains(
                            "\"schemaVersion\":1"
                    )
                    .contains(
                            "\"eventType\":"
                                    + "\"DOCUMENT_PROCESSING_REQUESTED\""
                    )
                    .contains(
                            "\"occurredAt\":\""
                                    + expectedOccurredAt()
                                    + "\""
                    )
                    .contains(
                            "\"eventId\":\""
                                    + eventId
                                    + "\""
                    )
                    .contains(
                            "\"documentId\":\""
                                    + documentId
                                    + "\""
                    )
                    .contains(
                            "\"patientId\":\""
                                    + patientId
                                    + "\""
                    )
                    .contains(
                            "\"fileUrl\":\""
                                    + expectedFileUrl()
                                    + "\""
                    );

            DocumentProcessingOutboxJpaEntity publishedEvent =
                    findOutboxEvent();

            assertThat(publishedEvent.getStatus())
                    .isEqualTo(
                            DocumentProcessingOutboxStatus.PUBLISHED
                    );

            assertThat(publishedEvent.getAttemptCount())
                    .isEqualTo(2);

            assertThat(publishedEvent.getErrorDetail())
                    .isNull();

            assertThat(publishedEvent.getPublishedAt())
                    .isNotNull();
        }
    }

    @SuppressWarnings("unchecked")
    private KafkaTemplate<Object, Object>
    createFailingKafkaTemplate() {
        KafkaTemplate<Object, Object> kafkaTemplate =
                mock(KafkaTemplate.class);

        doThrow(
                new IllegalStateException(SIMULATED_ERROR)
        )
                .when(kafkaTemplate)
                .send(
                        eq(processingRequestedTopic),
                        eq(documentId.toString()),
                        any(DocumentProcessingRequestedMessage.class)
                );

        return kafkaTemplate;
    }

    private void processFirstAttemptWithFailure(
            KafkaTemplate<Object, Object> failingKafkaTemplate
    ) {
        DocumentProcessingOutboxProcessor failingProcessor =
                new DocumentProcessingOutboxProcessor(
                        outboxRepository,
                        healthDocumentJpaRepository,
                        failingKafkaTemplate,
                        processingRequestedTopic,
                        DOCUMENT_FILE_BASE_URL,
                        1
                );

        /*
         * O processador criado manualmente não passa pelo proxy
         * @Transactional do Spring.
         *
         * Por isso, o TransactionTemplate mantém a entidade
         * gerenciada e confirma no PostgreSQL o estado FAILED.
         */
        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(
                transactionStatus ->
                        failingProcessor.process(eventId)
        );
    }

    private KafkaConsumer<String, String>
    createRequestedMessageConsumer() {
        Properties properties = new Properties();

        properties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaContainer.getBootstrapServers()
        );

        properties.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "patient-document-outbox-retry-test-"
                        + UUID.randomUUID()
        );

        properties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "latest"
        );

        properties.put(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                false
        );

        properties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        properties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        return new KafkaConsumer<>(properties);
    }

    private void subscribeAndSeekToEnd(
            KafkaConsumer<String, String> consumer
    ) {
        consumer.subscribe(
                List.of(processingRequestedTopic)
        );

        long deadline =
                System.nanoTime()
                        + ASYNC_TIMEOUT.toNanos();

        while (
                consumer.assignment().isEmpty()
                        && System.nanoTime() < deadline
        ) {
            consumer.poll(Duration.ofMillis(200));
        }

        assertThat(consumer.assignment())
                .as(
                        "O consumidor não recebeu uma partição "
                                + "do tópico da Outbox."
                )
                .isNotEmpty();

        /*
         * Obtém o fim atual antes da segunda tentativa.
         * A primeira tentativa utilizou um KafkaTemplate simulado
         * e não deixou mensagem no broker.
         */
        Map<TopicPartition, Long> endOffsets =
                consumer.endOffsets(
                        consumer.assignment()
                );

        endOffsets.forEach(
                (partition, offset) ->
                        consumer.seek(partition, offset)
        );

        endOffsets.forEach(
                (partition, expectedOffset) ->
                        assertThat(
                                consumer.position(partition)
                        )
                                .as(
                                        "Posição inicial incorreta "
                                                + "para a partição %s.",
                                        partition
                                )
                                .isEqualTo(expectedOffset)
        );
    }

    private ConsumerRecord<String, String> awaitRecord(
            KafkaConsumer<String, String> consumer,
            Duration timeout
    ) {
        long deadline =
                System.nanoTime()
                        + timeout.toNanos();

        while (System.nanoTime() < deadline) {
            ConsumerRecords<String, String> records =
                    consumer.poll(
                            Duration.ofMillis(250)
                    );

            if (!records.isEmpty()) {
                return records.iterator().next();
            }
        }

        throw new AssertionError(
                "Nenhuma mensagem da tentativa de recuperação "
                        + "foi recebida dentro do tempo esperado."
        );
    }

    private DocumentProcessingOutboxJpaEntity findOutboxEvent() {
        return outboxRepository
                .findById(eventId)
                .orElseThrow();
    }

    private Instant expectedOccurredAt() {
        return OUTBOX_CREATED_AT
                .atZone(SAO_PAULO_ZONE_ID)
                .toInstant();
    }

    private String expectedFileUrl() {
        return "/documentos/arquivo-outbox-retry.pdf";
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
                "Paciente do teste de recuperação da Outbox",
                LocalDate.of(1985, 5, 20),
                null,
                "outbox-retry@test.local",
                null,
                LocalDateTime.now(),
                null
        );
    }

    private void insertPendingDocument() {
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
                "exame-outbox-retry.pdf",
                "arquivo-outbox-retry.pdf",
                "/documentos/arquivo-outbox-retry.pdf",
                "application/pdf",
                2048L,
                DocumentProcessingStatus
                        .PENDING_PROCESSING
                        .name(),
                LocalDateTime.now()
        );
    }

    private void insertPendingOutboxEvent() {
        jdbcTemplate.update(
                """
                INSERT INTO document_processing_outbox (
                    event_id,
                    document_id,
                    patient_id,
                    status,
                    attempt_count,
                    error_detail,
                    created_at,
                    published_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                eventId,
                documentId,
                patientId,
                DocumentProcessingOutboxStatus.PENDING.name(),
                0,
                null,
                OUTBOX_CREATED_AT,
                null
        );
    }

    private void cleanDatabase() {
        jdbcTemplate.update(
                "DELETE FROM document_processed_inbox"
        );

        jdbcTemplate.update(
                "DELETE FROM document_processing_outbox"
        );

        jdbcTemplate.update(
                "DELETE FROM document_keywords"
        );

        jdbcTemplate.update(
                "DELETE FROM health_documents"
        );

        jdbcTemplate.update(
                "DELETE FROM patients"
        );
    }
}

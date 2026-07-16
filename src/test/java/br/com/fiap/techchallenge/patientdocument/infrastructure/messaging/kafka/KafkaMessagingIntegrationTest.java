package br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka;

import br.com.fiap.techchallenge.patientdocument.KafkaTestcontainersConfiguration;
import br.com.fiap.techchallenge.patientdocument.TestcontainersConfiguration;
import br.com.fiap.techchallenge.patientdocument.domain.document.DocumentProcessingStatus;
import br.com.fiap.techchallenge.patientdocument.domain.document.DocumentType;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.document.HealthDocumentJpaEntity;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.document.HealthDocumentJpaRepository;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.inbox.DocumentProcessedInboxJpaEntity;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.inbox.DocumentProcessedInboxJpaRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        properties = {
                "app.messaging.kafka.enabled=true",
                "app.messaging.kafka.outbox-fixed-delay=600000",
                "app.messaging.kafka.listener-retry-interval=100",
                "app.messaging.kafka.listener-max-retries=1"
        }
)
@Import({
        TestcontainersConfiguration.class,
        KafkaTestcontainersConfiguration.class
})
class KafkaMessagingIntegrationTest {

    private static final String EXTERNAL_RESULT_ID =
            "resultado-kafka-001";

    private static final String SUMMARY =
            "Hemograma processado por meio do Kafka.";

    private static final Duration ASYNC_TIMEOUT =
            Duration.ofSeconds(20);

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    private KafkaContainer kafkaContainer;

    @Autowired
    private HealthDocumentJpaRepository healthDocumentRepository;

    @Autowired
    private DocumentProcessedInboxJpaRepository inboxRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${app.messaging.kafka.topics.processed-response}")
    private String processedResponseTopic;

    @Value("${app.messaging.kafka.topics.processed-response-dlt}")
    private String processedResponseDltTopic;

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
        insertPublishedOutboxEvent();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void shouldConsumeValidMessageAndPersistProcessedResult()
            throws Exception {
        DocumentProcessedResponseMessage message =
                successfulMessage(eventId);

        kafkaTemplate
                .send(
                        processedResponseTopic,
                        documentId.toString(),
                        message
                )
                .get(10, TimeUnit.SECONDS);

        awaitCondition(
                () -> inboxRepository.count() == 1
                        && hasDocumentStatus(
                        DocumentProcessingStatus.PROCESSED
                ),
                ASYNC_TIMEOUT,
                "A resposta válida não foi processada pelo listener."
        );

        List<DocumentProcessedInboxJpaEntity> results =
                inboxRepository
                        .findByDocumentIdOrderByReceivedAtAsc(documentId);

        assertThat(results).hasSize(1);

        DocumentProcessedInboxJpaEntity result =
                results.getFirst();

        assertThat(result.getEventId())
                .isEqualTo(eventId);

        assertThat(result.getDocumentId())
                .isEqualTo(documentId);

        assertThat(result.getPatientId())
                .isEqualTo(patientId);

        assertThat(result.getExternalResultId())
                .isEqualTo(EXTERNAL_RESULT_ID);

        assertThat(result.getExternalDocumentType())
                .isEqualTo("EXAME_HEMOGRAMA");

        assertThat(result.getStatus())
                .isEqualTo(DocumentProcessingStatus.PROCESSED);

        assertThat(result.getPayload())
                .containsEntry("id", EXTERNAL_RESULT_ID)
                .containsEntry(
                        "patientId",
                        patientId.toString()
                )
                .containsEntry(
                        "documentType",
                        "EXAME_HEMOGRAMA"
                )
                .containsEntry(
                        "descricaoGeral",
                        SUMMARY
                );

        HealthDocumentJpaEntity document =
                findDocument();

        assertThat(document.getProcessingStatus())
                .isEqualTo(
                        DocumentProcessingStatus.PROCESSED.name()
                );

        assertThat(document.getDocumentType())
                .isEqualTo(
                        DocumentType.EXAME_LABORATORIAL.name()
                );

        assertThat(document.getDocumentDate())
                .isEqualTo(LocalDate.of(2026, 7, 10));

        assertThat(document.getSummary())
                .isEqualTo(SUMMARY);

        assertThat(document.getProcessedAt())
                .isNotNull();
    }

    @Test
    void shouldSendInvalidCorrelationMessageToDlt()
            throws Exception {
        UUID unknownEventId = UUID.randomUUID();

        try (
                KafkaConsumer<String, String> dltConsumer =
                        createDltConsumer()
        ) {
            subscribeAndWaitForAssignment(dltConsumer);

            DocumentProcessedResponseMessage invalidMessage =
                    successfulMessage(unknownEventId);

            kafkaTemplate
                    .send(
                            processedResponseTopic,
                            documentId.toString(),
                            invalidMessage
                    )
                    .get(10, TimeUnit.SECONDS);

            ConsumerRecord<String, String> dltRecord =
                    awaitDltRecord(
                            dltConsumer,
                            ASYNC_TIMEOUT
                    );

            assertThat(dltRecord.topic())
                    .isEqualTo(processedResponseDltTopic);

            assertThat(dltRecord.key())
                    .isEqualTo(documentId.toString());

            assertThat(dltRecord.value())
                    .contains(unknownEventId.toString())
                    .contains(documentId.toString())
                    .contains(patientId.toString())
                    .contains(EXTERNAL_RESULT_ID);

            assertThat(inboxRepository.count())
                    .isZero();

            HealthDocumentJpaEntity document =
                    findDocument();

            assertThat(document.getProcessingStatus())
                    .isEqualTo(
                            DocumentProcessingStatus
                                    .PENDING_PROCESSING
                                    .name()
                    );

            assertThat(document.getDocumentType())
                    .isNull();

            assertThat(document.getSummary())
                    .isNull();

            assertThat(document.getProcessedAt())
                    .isNull();
        }
    }

    private DocumentProcessedResponseMessage successfulMessage(
            UUID messageEventId
    ) {
        Map<String, Object> payload =
                new LinkedHashMap<>();

        payload.put("id", EXTERNAL_RESULT_ID);
        payload.put("patientId", patientId.toString());
        payload.put("documentType", "EXAME_HEMOGRAMA");
        payload.put(
                "documentDate",
                "2026-07-10T09:30:00"
        );
        payload.put("descricaoGeral", SUMMARY);

        return new DocumentProcessedResponseMessage(
                messageEventId,
                documentId,
                patientId,
                null,
                payload,
                null
        );
    }

    private KafkaConsumer<String, String> createDltConsumer() {
        Properties properties = new Properties();

        properties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaContainer.getBootstrapServers()
        );

        properties.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "patient-document-dlt-test-" + UUID.randomUUID()
        );

        properties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
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

    private void subscribeAndWaitForAssignment(
            KafkaConsumer<String, String> consumer
    ) {
        consumer.subscribe(
                List.of(processedResponseDltTopic)
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
                        "O consumidor de teste não recebeu "
                                + "uma partição da DLT."
                )
                .isNotEmpty();
    }

    private ConsumerRecord<String, String> awaitDltRecord(
            KafkaConsumer<String, String> consumer,
            Duration timeout
    ) {
        long deadline =
                System.nanoTime() + timeout.toNanos();

        while (System.nanoTime() < deadline) {
            ConsumerRecords<String, String> records =
                    consumer.poll(Duration.ofMillis(250));

            if (!records.isEmpty()) {
                return records.iterator().next();
            }
        }

        throw new AssertionError(
                "Nenhuma mensagem foi recebida na DLT "
                        + "dentro do tempo esperado."
        );
    }

    private void awaitCondition(
            BooleanSupplier condition,
            Duration timeout,
            String failureMessage
    ) {
        long deadline =
                System.nanoTime() + timeout.toNanos();

        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();

                throw new AssertionError(
                        "A espera assíncrona foi interrompida.",
                        exception
                );
            }
        }

        throw new AssertionError(failureMessage);
    }

    private boolean hasDocumentStatus(
            DocumentProcessingStatus status
    ) {
        return healthDocumentRepository
                .findById(documentId)
                .map(
                        document ->
                                status.name().equals(
                                        document.getProcessingStatus()
                                )
                )
                .orElse(false);
    }

    private HealthDocumentJpaEntity findDocument() {
        return healthDocumentRepository
                .findById(documentId)
                .orElseThrow();
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
                "Paciente do teste Kafka",
                LocalDate.of(1985, 5, 20),
                null,
                "kafka-integration@test.local",
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
                "exame-kafka.pdf",
                "arquivo-kafka.pdf",
                "/documentos/arquivo-kafka.pdf",
                "application/pdf",
                2048L,
                DocumentProcessingStatus
                        .PENDING_PROCESSING
                        .name(),
                LocalDateTime.now()
        );
    }

    private void insertPublishedOutboxEvent() {
        LocalDateTime now = LocalDateTime.now();

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
                "PUBLISHED",
                1,
                null,
                now,
                now
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

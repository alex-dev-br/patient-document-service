package br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka;

import br.com.fiap.techchallenge.patientdocument.KafkaTestcontainersConfiguration;
import br.com.fiap.techchallenge.patientdocument.TestcontainersConfiguration;
import br.com.fiap.techchallenge.patientdocument.application.document.command.ProcessDocumentProcessingResultCommand;
import br.com.fiap.techchallenge.patientdocument.application.document.command.ProcessDocumentProcessingResultItemCommand;
import br.com.fiap.techchallenge.patientdocument.application.document.usecase.ProcessDocumentProcessingResultUseCase;
import br.com.fiap.techchallenge.patientdocument.domain.document.DocumentProcessingStatus;
import br.com.fiap.techchallenge.patientdocument.domain.document.DocumentType;
import br.com.fiap.techchallenge.patientdocument.domain.document.MedicalSpecialty;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.document.HealthDocumentJpaEntity;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.document.HealthDocumentJpaRepository;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.inbox.DocumentProcessedInboxJpaEntity;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.inbox.DocumentProcessedInboxJpaRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
class DocumentProcessingResultKafkaIntegrationTest {

    private static final Duration ASYNC_TIMEOUT =
            Duration.ofSeconds(20);

    private static final Instant OCCURRED_AT =
            Instant.parse("2026-07-22T18:00:00Z");

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    private KafkaContainer kafkaContainer;

    @Autowired
    private ProcessDocumentProcessingResultUseCase useCase;

    @Autowired
    private HealthDocumentJpaRepository
            healthDocumentRepository;

    @Autowired
    private DocumentProcessedInboxJpaRepository
            inboxRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${app.messaging.kafka.topics.processing-result}")
    private String processingResultTopic;

    @Value(
            "${app.messaging.kafka.topics.processing-result-dlt}"
    )
    private String processingResultDltTopic;

    private UUID patientId;
    private UUID documentId;
    private UUID correlationId;
    private UUID responseEventId;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        patientId = UUID.randomUUID();
        documentId = UUID.randomUUID();
        correlationId = UUID.randomUUID();
        responseEventId = UUID.randomUUID();

        insertPatient();
        insertPendingDocument();
        insertPublishedOutboxEvent();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void shouldConsumeTwoResultsAndRemainIdempotent()
            throws Exception {
        DocumentProcessingResultMessage message =
                completedMessage();

        sendMessage(
                documentId.toString(),
                message
        );

        awaitCondition(
                () -> inboxRepository.count() == 2
                        && hasDocumentStatus(
                        DocumentProcessingStatus.PROCESSED
                ),
                ASYNC_TIMEOUT,
                "O resultado agregado não foi persistido."
        );

        List<DocumentProcessedInboxJpaEntity> results =
                inboxRepository
                        .findByDocumentIdOrderByReceivedAtAsc(
                                documentId
                        );

        assertThat(results)
                .hasSize(2)
                .extracting(
                        DocumentProcessedInboxJpaEntity::
                                getExternalResultId
                )
                .containsExactlyInAnyOrder(
                        "resultado-001",
                        "resultado-002"
                );

        assertThat(results)
                .allSatisfy(
                        result -> {
                            assertThat(result.getEventId())
                                    .isEqualTo(responseEventId);

                            assertThat(result.getCorrelationId())
                                    .isEqualTo(correlationId);

                            assertThat(result.getDocumentId())
                                    .isEqualTo(documentId);

                            assertThat(result.getPatientId())
                                    .isEqualTo(patientId);

                            assertThat(result.getStatus())
                                    .isEqualTo(
                                            DocumentProcessingStatus
                                                    .PROCESSED
                                    );

                            assertThat(result.getErrorCode())
                                    .isNull();

                            assertThat(result.getErrorDetail())
                                    .isNull();
                        }
                );

        DocumentProcessedInboxJpaEntity firstResult =
                results.stream()
                        .filter(
                                result ->
                                        "resultado-001".equals(
                                                result
                                                        .getExternalResultId()
                                        )
                        )
                        .findFirst()
                        .orElseThrow();

        assertThat(firstResult.getExternalDocumentType())
                .isEqualTo("EXAME_HEMOGRAMA");

        assertThat(firstResult.getDocumentDate())
                .isEqualTo(LocalDate.of(2026, 7, 10));

        assertThat(firstResult.getPayload())
                .containsEntry(
                        "exameTipo",
                        "HEMOGRAMA"
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

        assertThat(document.getSpecialty())
                .isEqualTo(
                        MedicalSpecialty
                                .EXAMES_LABORATORIAIS
                                .name()
                );

        assertThat(document.getDocumentDate())
                .isEqualTo(LocalDate.of(2026, 7, 10));

        assertThat(document.getSummary())
                .isEqualTo(
                        "Documento processado contendo: "
                                + "exame hemograma, receita."
                );

        assertThat(document.getConfidence())
                .isEqualByComparingTo("0.92");

        sendMessage(
                documentId.toString(),
                message
        );

        assertCountRemainsStable(
                2,
                Duration.ofSeconds(2)
        );
    }

    @Test
    void shouldConsumeStructuredFailure()
            throws Exception {
        sendMessage(
                documentId.toString(),
                failedMessage()
        );

        awaitCondition(
                () -> inboxRepository.count() == 1
                        && hasDocumentStatus(
                        DocumentProcessingStatus.FAILED
                ),
                ASYNC_TIMEOUT,
                "A falha agregada não foi persistida."
        );

        DocumentProcessedInboxJpaEntity result =
                inboxRepository
                        .findByDocumentIdOrderByReceivedAtAsc(
                                documentId
                        )
                        .getFirst();

        assertThat(result.getEventId())
                .isEqualTo(responseEventId);

        assertThat(result.getCorrelationId())
                .isEqualTo(correlationId);

        assertThat(result.getExternalResultId())
                .isEqualTo("FAILED");

        assertThat(result.getStatus())
                .isEqualTo(DocumentProcessingStatus.FAILED);

        assertThat(result.getPayload()).isNull();

        assertThat(result.getErrorCode())
                .isEqualTo("AI_PROCESSING_FAILED");

        assertThat(result.getErrorDetail())
                .isEqualTo(
                        "Não foi possível processar "
                                + "o documento."
                );

        assertThat(result.getErrorRetryable())
                .isFalse();
    }

    @Test
    void shouldSendDivergentKafkaKeyToResultDlt()
            throws Exception {
        try (
                KafkaConsumer<String, String> dltConsumer =
                        createDltConsumer()
        ) {
            subscribeAndWaitForAssignment(dltConsumer);

            String divergentKey =
                    UUID.randomUUID().toString();

            sendMessage(
                    divergentKey,
                    completedMessage()
            );

            ConsumerRecord<String, String> dltRecord =
                    awaitDltRecord(
                            dltConsumer,
                            ASYNC_TIMEOUT
                    );

            assertThat(dltRecord.topic())
                    .isEqualTo(processingResultDltTopic);

            assertThat(dltRecord.key())
                    .isEqualTo(divergentKey);

            assertThat(dltRecord.value())
                    .contains(responseEventId.toString())
                    .contains(documentId.toString());

            assertThat(inboxRepository.count())
                    .isZero();

            assertThat(findDocument().getProcessingStatus())
                    .isEqualTo(
                            DocumentProcessingStatus
                                    .PENDING_PROCESSING
                                    .name()
                    );
        }
    }

    @Test
    void shouldSendUnknownJsonPropertyToResultDlt()
            throws Exception {
        try (
                KafkaConsumer<String, String> dltConsumer =
                        createDltConsumer()
        ) {
            subscribeAndWaitForAssignment(dltConsumer);

            String rawMessage =
                    completedRawJson().replace(
                            """
                            "results": [
                            """,
                            """
                            "futureEnvelopeField": "não permitido",
                            "results": [
                            """
                    );

            sendRawJson(
                    documentId.toString(),
                    rawMessage
            );

            ConsumerRecord<String, String> dltRecord =
                    awaitDltRecord(
                            dltConsumer,
                            ASYNC_TIMEOUT
                    );

            assertThat(dltRecord.topic())
                    .isEqualTo(processingResultDltTopic);

            assertThat(dltRecord.key())
                    .isEqualTo(documentId.toString());

            assertThat(dltRecord.value())
                    .contains("futureEnvelopeField")
                    .contains(responseEventId.toString());

            assertThat(inboxRepository.count())
                    .isZero();

            assertThat(findDocument().getProcessingStatus())
                    .isEqualTo(
                            DocumentProcessingStatus
                                    .PENDING_PROCESSING
                                    .name()
                    );
        }
    }

    @Test
    void shouldRollbackAllChangesWhenSecondResultFails()
            throws Exception {
        ProcessDocumentProcessingResultCommand command =
                new ProcessDocumentProcessingResultCommand(
                        1,
                        "DOCUMENT_PROCESSING_COMPLETED",
                        responseEventId,
                        correlationId,
                        OCCURRED_AT,
                        documentId,
                        patientId,
                        "Documento cujo segundo resultado falhará.",
                        "EXAME_LABORATORIAL",
                        null,
                        LocalDate.of(2026, 7, 10),
                        null,
                        List.of(
                                new ProcessDocumentProcessingResultItemCommand(
                                        "resultado-valido",
                                        "EXAME_HEMOGRAMA",
                                        LocalDate.of(2026, 7, 10),
                                        Map.of(
                                                "exameTipo",
                                                "HEMOGRAMA"
                                        )
                                ),
                                new ProcessDocumentProcessingResultItemCommand(
                                        "x".repeat(65),
                                        "RECEITA",
                                        LocalDate.of(2026, 7, 11),
                                        Map.of(
                                                "descricaoGeral",
                                                "Resultado inválido."
                                        )
                                )
                        ),
                        null,
                        null,
                        null
                );

        assertThatThrownBy(
                () -> useCase.execute(command)
        )
                .isInstanceOf(RuntimeException.class);

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

    private void sendMessage(
            String key,
            DocumentProcessingResultMessage message
    ) throws Exception {
        kafkaTemplate
                .send(
                        processingResultTopic,
                        key,
                        message
                )
                .get(10, TimeUnit.SECONDS);
    }

    private void sendRawJson(
            String key,
            String value
    ) throws Exception {
        Properties properties =
                new Properties();

        properties.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaContainer.getBootstrapServers()
        );

        try (
                KafkaProducer<String, String> producer =
                        new KafkaProducer<>(
                                properties,
                                new StringSerializer(),
                                new StringSerializer()
                        )
        ) {
            producer.send(
                    new ProducerRecord<>(
                            processingResultTopic,
                            key,
                            value
                    )
            ).get(10, TimeUnit.SECONDS);
        }
    }

    private DocumentProcessingResultMessage
    completedMessage() {
        return new DocumentProcessingResultMessage(
                1,
                "DOCUMENT_PROCESSING_COMPLETED",
                responseEventId,
                correlationId,
                OCCURRED_AT,
                documentId,
                patientId,
                "Documento processado contendo: "
                        + "exame hemograma, receita.",
                "EXAME_LABORATORIAL",
                "EXAMES_LABORATORIAIS",
                LocalDate.of(2026, 7, 10),
                new BigDecimal("0.92"),
                List.of(
                        new DocumentProcessingResultItemMessage(
                                "resultado-001",
                                "EXAME_HEMOGRAMA",
                                LocalDate.of(2026, 7, 10),
                                Map.of(
                                        "exameTipo",
                                        "HEMOGRAMA"
                                )
                        ),
                        new DocumentProcessingResultItemMessage(
                                "resultado-002",
                                "RECEITA",
                                LocalDate.of(2026, 7, 11),
                                Map.of(
                                        "descricaoGeral",
                                        "Receita médica."
                                )
                        )
                ),
                null
        );
    }

    private DocumentProcessingResultMessage
    failedMessage() {
        return new DocumentProcessingResultMessage(
                1,
                "DOCUMENT_PROCESSING_FAILED",
                responseEventId,
                correlationId,
                OCCURRED_AT,
                documentId,
                patientId,
                null,
                null,
                null,
                null,
                null,
                null,
                new DocumentProcessingErrorMessage(
                        "AI_PROCESSING_FAILED",
                        "Não foi possível processar o documento.",
                        false
                )
        );
    }

    private String completedRawJson() {
        return """
                {
                  "schemaVersion": 1,
                  "eventType": "DOCUMENT_PROCESSING_COMPLETED",
                  "eventId": "%s",
                  "correlationId": "%s",
                  "occurredAt": "2026-07-22T18:00:00Z",
                  "documentId": "%s",
                  "patientId": "%s",
                  "summary": "Documento processado contendo: exame hemograma, receita.",
                  "primaryDocumentType": "EXAME_LABORATORIAL",
                  "specialty": "EXAMES_LABORATORIAIS",
                  "documentDate": "2026-07-10",
                  "confidence": 0.92,
                  "results": [
                    {
                      "resultId": "resultado-001",
                      "documentType": "EXAME_HEMOGRAMA",
                      "documentDate": "2026-07-10",
                      "data": {
                        "exameTipo": "HEMOGRAMA"
                      }
                    }
                  ],
                  "error": null
                }
                """.formatted(
                responseEventId,
                correlationId,
                documentId,
                patientId
        );
    }

    private KafkaConsumer<String, String>
    createDltConsumer() {
        Properties properties =
                new Properties();

        properties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaContainer.getBootstrapServers()
        );

        properties.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "processing-result-dlt-test-"
                        + UUID.randomUUID()
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
                List.of(processingResultDltTopic)
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
                                + "uma partição da DLT agregada."
                )
                .isNotEmpty();

        consumer.seekToEnd(consumer.assignment());
    }

    private ConsumerRecord<String, String> awaitDltRecord(
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
                return records
                        .iterator()
                        .next();
            }
        }

        throw new AssertionError(
                "Nenhuma mensagem foi recebida na DLT "
                        + "agregada dentro do tempo esperado."
        );
    }

    private void assertCountRemainsStable(
            long expectedCount,
            Duration duration
    ) throws InterruptedException {
        long deadline =
                System.nanoTime()
                        + duration.toNanos();

        while (System.nanoTime() < deadline) {
            assertThat(inboxRepository.count())
                    .isEqualTo(expectedCount);

            Thread.sleep(100);
        }
    }

    private void awaitCondition(
            BooleanSupplier condition,
            Duration timeout,
            String failureMessage
    ) {
        long deadline =
                System.nanoTime()
                        + timeout.toNanos();

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
                                        document
                                                .getProcessingStatus()
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
                "Paciente do teste agregado",
                LocalDate.of(1985, 5, 20),
                null,
                "aggregate-kafka@test.local",
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
                "resultado-agregado.pdf",
                "arquivo-agregado.pdf",
                "/documentos/arquivo-agregado.pdf",
                "application/pdf",
                2048L,
                DocumentProcessingStatus
                        .PENDING_PROCESSING
                        .name(),
                LocalDateTime.now()
        );
    }

    private void insertPublishedOutboxEvent() {
        LocalDateTime now =
                LocalDateTime.now();

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
                correlationId,
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

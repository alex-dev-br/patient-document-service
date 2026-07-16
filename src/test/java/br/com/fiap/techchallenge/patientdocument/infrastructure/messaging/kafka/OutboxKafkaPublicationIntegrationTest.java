package br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka;

import br.com.fiap.techchallenge.patientdocument.KafkaTestcontainersConfiguration;
import br.com.fiap.techchallenge.patientdocument.TestcontainersConfiguration;
import br.com.fiap.techchallenge.patientdocument.domain.document.DocumentProcessingStatus;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        properties = {
                "app.messaging.kafka.enabled=true",
                "app.messaging.kafka.outbox-fixed-delay=600000",
                "app.messaging.kafka.document-file-base-url=http://localhost:8080/",
                "app.messaging.kafka.send-timeout-seconds=10"
        }
)
@Import({
        TestcontainersConfiguration.class,
        KafkaTestcontainersConfiguration.class
})
class OutboxKafkaPublicationIntegrationTest {

    private static final Duration ASYNC_TIMEOUT =
            Duration.ofSeconds(20);

    private static final Duration NO_MESSAGE_TIMEOUT =
            Duration.ofSeconds(2);

    @Autowired
    private DocumentProcessingOutboxProcessor outboxProcessor;

    /*
     * Substitui somente o scheduler real.
     *
     * Isso impede que ele encontre e publique automaticamente
     * os eventos PENDING antes que o consumidor do teste esteja
     * pronto para recebê-los.
     *
     * O DocumentProcessingOutboxProcessor continua sendo o
     * componente real da aplicação.
     */
    @MockitoBean(enforceOverride = true)
    private DocumentProcessingOutboxScheduler outboxScheduler;

    @Autowired
    private DocumentProcessingOutboxJpaRepository outboxRepository;

    @Autowired
    private KafkaContainer kafkaContainer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void shouldPublishPendingOutboxEventAndMarkItAsPublished() {
        insertPendingOutboxEvent();

        try (
                KafkaConsumer<String, String> consumer =
                        createRequestedMessageConsumer()
        ) {
            subscribeAndSeekToEnd(consumer);

            /*
             * Agora somente esta chamada publica o evento.
             * O scheduler está substituído por um mock.
             */
            outboxProcessor.process(eventId);

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

            DocumentProcessingOutboxJpaEntity persistedEvent =
                    findOutboxEvent();

            assertThat(persistedEvent.getStatus())
                    .isEqualTo(
                            DocumentProcessingOutboxStatus.PUBLISHED
                    );

            assertThat(persistedEvent.getAttemptCount())
                    .isEqualTo(1);

            assertThat(persistedEvent.getPublishedAt())
                    .isNotNull();

            assertThat(persistedEvent.getErrorDetail())
                    .isNull();
        }
    }

    @Test
    void shouldNotRepublishAnAlreadyPublishedOutboxEvent() {
        LocalDateTime originalPublishedAt =
                insertPublishedOutboxEvent();

        try (
                KafkaConsumer<String, String> consumer =
                        createRequestedMessageConsumer()
        ) {
            subscribeAndSeekToEnd(consumer);

            outboxProcessor.process(eventId);

            ConsumerRecords<String, String> records =
                    consumer.poll(NO_MESSAGE_TIMEOUT);

            assertThat(records.isEmpty())
                    .as(
                            "Um evento já publicado não deveria "
                                    + "gerar outra mensagem Kafka."
                    )
                    .isTrue();

            DocumentProcessingOutboxJpaEntity persistedEvent =
                    findOutboxEvent();

            assertThat(persistedEvent.getStatus())
                    .isEqualTo(
                            DocumentProcessingOutboxStatus.PUBLISHED
                    );

            assertThat(persistedEvent.getAttemptCount())
                    .isEqualTo(1);

            assertThat(persistedEvent.getPublishedAt())
                    .isEqualTo(originalPublishedAt);

            assertThat(persistedEvent.getErrorDetail())
                    .isNull();
        }
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
                "patient-document-outbox-test-"
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
                        "O consumidor de teste não recebeu "
                                + "uma partição do tópico da Outbox."
                )
                .isNotEmpty();

        /*
         * Obtém explicitamente o fim atual de cada partição.
         *
         * Diferentemente de seekToEnd(), que pode resolver o offset
         * apenas no próximo poll, endOffsets() retorna os valores
         * existentes antes da publicação realizada pelo teste.
         */
        Map<TopicPartition, Long> endOffsets =
                consumer.endOffsets(consumer.assignment());

        endOffsets.forEach(
                (partition, offset) ->
                        consumer.seek(partition, offset)
        );

        /*
         * consumer.position() força a materialização da posição
         * antes que o processador publique a nova mensagem.
         */
        endOffsets.forEach(
                (partition, expectedOffset) ->
                        assertThat(consumer.position(partition))
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
                System.nanoTime() + timeout.toNanos();

        while (System.nanoTime() < deadline) {
            ConsumerRecords<String, String> records =
                    consumer.poll(Duration.ofMillis(250));

            if (!records.isEmpty()) {
                return records.iterator().next();
            }
        }

        throw new AssertionError(
                "Nenhuma mensagem da Outbox foi recebida "
                        + "dentro do tempo esperado."
        );
    }

    private DocumentProcessingOutboxJpaEntity findOutboxEvent() {
        return outboxRepository
                .findById(eventId)
                .orElseThrow();
    }

    private String expectedFileUrl() {
        return "http://localhost:8080/documents/"
                + documentId
                + "/file";
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
                "Paciente do teste de publicação da Outbox",
                LocalDate.of(1985, 5, 20),
                null,
                "outbox-publication@test.local",
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
                "exame-outbox.pdf",
                "arquivo-outbox.pdf",
                "/documentos/arquivo-outbox.pdf",
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
                LocalDateTime.now(),
                null
        );
    }

    private LocalDateTime insertPublishedOutboxEvent() {
        LocalDateTime publishedAt =
                LocalDateTime.now()
                        .minusMinutes(1)
                        .withNano(0);

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
                DocumentProcessingOutboxStatus.PUBLISHED.name(),
                1,
                null,
                publishedAt.minusSeconds(5),
                publishedAt
        );

        return publishedAt;
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

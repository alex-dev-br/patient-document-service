package br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka;

import br.com.fiap.techchallenge.patientdocument.KafkaTestcontainersConfiguration;
import br.com.fiap.techchallenge.patientdocument.TestcontainersConfiguration;
import br.com.fiap.techchallenge.patientdocument.application.document.command.UploadHealthDocumentCommand;
import br.com.fiap.techchallenge.patientdocument.application.document.usecase.UploadHealthDocumentUseCase;
import br.com.fiap.techchallenge.patientdocument.application.storage.command.StoreFileCommand;
import br.com.fiap.techchallenge.patientdocument.application.storage.gateway.StorageGateway;
import br.com.fiap.techchallenge.patientdocument.application.storage.result.StoredFile;
import br.com.fiap.techchallenge.patientdocument.domain.document.DocumentProcessingStatus;
import br.com.fiap.techchallenge.patientdocument.domain.document.DocumentType;
import br.com.fiap.techchallenge.patientdocument.domain.document.HealthDocument;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.document.HealthDocumentJpaEntity;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.document.HealthDocumentJpaRepository;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.inbox.DocumentProcessedInboxJpaEntity;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.inbox.DocumentProcessedInboxJpaRepository;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.kafka.KafkaContainer;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(
        properties = {
                "app.messaging.kafka.enabled=true",
                "app.messaging.kafka.outbox-fixed-delay=600000",
                "app.messaging.kafka.document-file-base-url=http://localhost:8080/",
                "app.messaging.kafka.send-timeout-seconds=10",
                "app.messaging.kafka.listener-retry-interval=100",
                "app.messaging.kafka.listener-max-retries=1"
        }
)
@Import({
        TestcontainersConfiguration.class,
        KafkaTestcontainersConfiguration.class
})
class DocumentProcessingEndToEndIntegrationTest {

    private static final String ORIGINAL_FILE_NAME =
            "hemograma-end-to-end.pdf";

    private static final String STORED_FILE_NAME =
            "arquivo-hemograma-end-to-end.pdf";

    private static final String STORAGE_PATH =
            "http://nextcloud.local/remote.php/dav/files/"
                    + "nextcloud/20260716/"
                    + STORED_FILE_NAME;

    private static final String CONTENT_TYPE =
            "application/pdf";

    private static final byte[] FILE_CONTENT =
            "conteúdo do hemograma do teste ponta a ponta"
                    .getBytes(StandardCharsets.UTF_8);

    private static final String EXTERNAL_RESULT_ID =
            "resultado-end-to-end-001";

    private static final String EXTERNAL_DOCUMENT_TYPE =
            "EXAME_HEMOGRAMA";

    private static final String SUMMARY =
            "Hemograma processado no fluxo ponta a ponta.";

    private static final LocalDate DOCUMENT_DATE =
            LocalDate.of(2026, 7, 10);

    private static final Duration ASYNC_TIMEOUT =
            Duration.ofSeconds(20);

    @Autowired
    private UploadHealthDocumentUseCase uploadHealthDocumentUseCase;

    @Autowired
    private DocumentProcessingOutboxProcessor outboxProcessor;

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    private KafkaContainer kafkaContainer;

    @Autowired
    private HealthDocumentJpaRepository healthDocumentRepository;

    @Autowired
    private DocumentProcessingOutboxJpaRepository outboxRepository;

    @Autowired
    private DocumentProcessedInboxJpaRepository inboxRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean(
            name = "nextcloudStorageGateway",
            enforceOverride = true
    )
    private StorageGateway storageGateway;

    /*
     * Impede a publicação automática antes que o consumidor
     * do teste esteja preparado.
     *
     * O processador da Outbox permanece real.
     */
    @MockitoBean(enforceOverride = true)
    private DocumentProcessingOutboxScheduler outboxScheduler;

    @Value("${app.messaging.kafka.topics.processing-requested}")
    private String processingRequestedTopic;

    @Value("${app.messaging.kafka.topics.processed-response}")
    private String processedResponseTopic;

    private UUID patientId;
    private UUID documentId;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        patientId = UUID.randomUUID();

        insertPatient();
        configureSuccessfulStorage();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void shouldProcessUploadedDocumentEndToEnd()
            throws Exception {

        /*
         * 1. Upload do arquivo.
         *
         * O caso de uso real grava o documento e o evento da
         * Outbox na mesma transação.
         */
        HealthDocument uploadedDocument =
                uploadHealthDocumentUseCase.execute(
                        validUploadCommand()
                );

        documentId = uploadedDocument.getId();

        assertThat(healthDocumentRepository.count())
                .isEqualTo(1);

        assertThat(outboxRepository.count())
                .isEqualTo(1);

        DocumentProcessingOutboxJpaEntity pendingEvent =
                outboxRepository.findAll().getFirst();

        eventId = pendingEvent.getEventId();

        assertThat(pendingEvent.getDocumentId())
                .isEqualTo(documentId);

        assertThat(pendingEvent.getPatientId())
                .isEqualTo(patientId);

        assertThat(pendingEvent.getStatus())
                .isEqualTo(DocumentProcessingOutboxStatus.PENDING);

        assertThat(pendingEvent.getAttemptCount())
                .isZero();

        HealthDocumentJpaEntity pendingDocument =
                findDocument();

        assertThat(pendingDocument.getProcessingStatus())
                .isEqualTo(
                        DocumentProcessingStatus
                                .PENDING_PROCESSING
                                .name()
                );

        /*
         * 2. Prepara o consumidor antes de publicar a mensagem
         * da Outbox.
         */
        try (
                KafkaConsumer<String, String> requestedConsumer =
                        createRequestedMessageConsumer()
        ) {
            subscribeAndSeekToEnd(requestedConsumer);

            /*
             * 3. Publicação real da Outbox no Kafka.
             */
            outboxProcessor.process(eventId);

            ConsumerRecord<String, String> requestedRecord =
                    awaitRequestedRecord(
                            requestedConsumer,
                            ASYNC_TIMEOUT
                    );

            assertRequestedMessage(requestedRecord);

            DocumentProcessingOutboxJpaEntity publishedEvent =
                    findOutboxEvent();

            assertThat(publishedEvent.getStatus())
                    .isEqualTo(
                            DocumentProcessingOutboxStatus.PUBLISHED
                    );

            assertThat(publishedEvent.getAttemptCount())
                    .isEqualTo(1);

            assertThat(publishedEvent.getPublishedAt())
                    .isNotNull();

            assertThat(publishedEvent.getErrorDetail())
                    .isNull();

            /*
             * 4. Simula a resposta produzida pelo serviço de IA.
             *
             * A publicação é real no tópico de respostas.
             */
            kafkaTemplate
                    .send(
                            processedResponseTopic,
                            documentId.toString(),
                            successfulResponseMessage()
                    )
                    .get(10, TimeUnit.SECONDS);

            /*
             * 5. Aguarda o listener consumir a resposta,
             * criar a Inbox e atualizar o documento.
             */
            awaitCondition(
                    () -> inboxRepository.count() == 1
                            && hasDocumentStatus(
                            DocumentProcessingStatus.PROCESSED
                    ),
                    ASYNC_TIMEOUT,
                    "O documento não concluiu o fluxo "
                            + "ponta a ponta."
            );

            assertProcessedInbox();
            assertProcessedDocument();
        }

        verify(storageGateway)
                .store(any(StoreFileCommand.class));

        verify(storageGateway, never())
                .delete(any(String.class));
    }

    private void assertRequestedMessage(
            ConsumerRecord<String, String> record
    ) {
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
                )
                .contains(
                        "\"contentType\":\""
                                + CONTENT_TYPE
                                + "\""
                );
    }

    private void assertProcessedInbox() {
        List<DocumentProcessedInboxJpaEntity> results =
                inboxRepository
                        .findByDocumentIdOrderByReceivedAtAsc(
                                documentId
                        );

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
                .isEqualTo(EXTERNAL_DOCUMENT_TYPE);

        assertThat(result.getStatus())
                .isEqualTo(
                        DocumentProcessingStatus.PROCESSED
                );

        assertThat(result.getPayload())
                .containsEntry(
                        "id",
                        EXTERNAL_RESULT_ID
                )
                .containsEntry(
                        "patientId",
                        patientId.toString()
                )
                .containsEntry(
                        "documentType",
                        EXTERNAL_DOCUMENT_TYPE
                )
                .containsEntry(
                        "descricaoGeral",
                        SUMMARY
                );
    }

    private void assertProcessedDocument() {
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
                .isEqualTo(DOCUMENT_DATE);

        assertThat(document.getSummary())
                .isEqualTo(SUMMARY);

        assertThat(document.getProcessedAt())
                .isNotNull();

        /*
         * Os metadados originais do upload também precisam
         * permanecer preservados após o processamento.
         */
        assertThat(document.getPatientId())
                .isEqualTo(patientId);

        assertThat(document.getOriginalFileName())
                .isEqualTo(ORIGINAL_FILE_NAME);

        assertThat(document.getStoredFileName())
                .isEqualTo(STORED_FILE_NAME);

        assertThat(document.getStoragePath())
                .isEqualTo(STORAGE_PATH);

        assertThat(document.getContentType())
                .isEqualTo(CONTENT_TYPE);

        assertThat(document.getFileSize())
                .isEqualTo((long) FILE_CONTENT.length);
    }

    private DocumentProcessedResponseMessage
    successfulResponseMessage() {

        Map<String, Object> payload =
                new LinkedHashMap<>();

        payload.put(
                "id",
                EXTERNAL_RESULT_ID
        );

        payload.put(
                "patientId",
                patientId.toString()
        );

        payload.put(
                "documentType",
                EXTERNAL_DOCUMENT_TYPE
        );

        payload.put(
                "documentDate",
                "2026-07-10T09:30:00"
        );

        payload.put(
                "descricaoGeral",
                SUMMARY
        );

        return new DocumentProcessedResponseMessage(
                eventId,
                documentId,
                patientId,
                null,
                payload,
                null
        );
    }

    private void configureSuccessfulStorage() {
        when(storageGateway.store(any(StoreFileCommand.class)))
                .thenReturn(
                        new StoredFile(
                                ORIGINAL_FILE_NAME,
                                STORED_FILE_NAME,
                                STORAGE_PATH,
                                CONTENT_TYPE,
                                (long) FILE_CONTENT.length
                        )
                );
    }

    private UploadHealthDocumentCommand validUploadCommand() {
        return new UploadHealthDocumentCommand(
                patientId,
                ORIGINAL_FILE_NAME,
                CONTENT_TYPE,
                (long) FILE_CONTENT.length,
                new ByteArrayInputStream(FILE_CONTENT)
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
                "patient-document-end-to-end-test-"
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
                        "O consumidor do teste não recebeu "
                                + "uma partição do tópico "
                                + "de solicitações."
                )
                .isNotEmpty();

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

    private ConsumerRecord<String, String>
    awaitRequestedRecord(
            KafkaConsumer<String, String> consumer,
            Duration timeout
    ) {
        long deadline =
                System.nanoTime() + timeout.toNanos();

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
                "Nenhuma solicitação de processamento "
                        + "foi recebida dentro do tempo esperado."
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

    private DocumentProcessingOutboxJpaEntity
    findOutboxEvent() {
        return outboxRepository
                .findById(eventId)
                .orElseThrow();
    }

    private String expectedFileUrl() {
        return STORAGE_PATH;
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
                "Paciente do teste ponta a ponta",
                LocalDate.of(1985, 5, 20),
                null,
                "end-to-end@test.local",
                null,
                LocalDateTime.now(),
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

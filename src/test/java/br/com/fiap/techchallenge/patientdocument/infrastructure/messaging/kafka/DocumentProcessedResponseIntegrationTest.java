package br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka;

import br.com.fiap.techchallenge.patientdocument.TestcontainersConfiguration;
import br.com.fiap.techchallenge.patientdocument.application.document.usecase.ProcessDocumentProcessedResponseUseCase;
import br.com.fiap.techchallenge.patientdocument.domain.document.DocumentProcessingStatus;
import br.com.fiap.techchallenge.patientdocument.domain.document.DocumentType;
import br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka.validation.DocumentProcessedResponseValidator;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.document.HealthDocumentJpaEntity;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.document.HealthDocumentJpaRepository;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.inbox.DocumentProcessedInboxJpaEntity;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.inbox.DocumentProcessedInboxJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
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
class DocumentProcessedResponseIntegrationTest {

    private static final String FIRST_RESULT_ID =
            "resultado-ia-001";

    private static final String SECOND_RESULT_ID =
            "resultado-ia-002";

    private static final String FIRST_SUMMARY =
            "Hemograma sem alterações relevantes.";

    private static final String SECOND_SUMMARY =
            "Laudo complementar do exame.";

    @Autowired
    private ProcessDocumentProcessedResponseUseCase useCase;

    @Autowired
    private HealthDocumentJpaRepository healthDocumentRepository;

    @Autowired
    private DocumentProcessedInboxJpaRepository inboxRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private DocumentProcessedResponseListener listener;

    private UUID patientId;
    private UUID documentId;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        patientId = UUID.randomUUID();
        documentId = UUID.randomUUID();
        eventId = UUID.randomUUID();

        listener = new DocumentProcessedResponseListener(
                useCase,
                new DocumentProcessedResponseValidator()
        );

        insertPatient();
        insertPendingDocument();
        insertPublishedOutboxEvent();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void shouldPersistSuccessfulResponseAndUpdateDocument() {
        Map<String, Object> payload = successfulPayload(
                FIRST_RESULT_ID,
                "EXAME_HEMOGRAMA",
                "2026-07-10T09:30:00",
                FIRST_SUMMARY
        );

        listener.consume(successMessage(payload));

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
                .isEqualTo(FIRST_RESULT_ID);

        assertThat(result.getExternalDocumentType())
                .isEqualTo("EXAME_HEMOGRAMA");

        assertThat(result.getStatus())
                .isEqualTo(DocumentProcessingStatus.PROCESSED);

        assertThat(result.getPayload())
                .containsEntry("id", FIRST_RESULT_ID)
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
                        FIRST_SUMMARY
                );

        assertThat(result.getErrorDetail())
                .isNull();

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
                .isEqualTo(FIRST_SUMMARY);

        assertThat(document.getProcessedAt())
                .isNotNull();
    }

    @Test
    void shouldIgnoreDuplicatedSuccessfulResponse() {
        DocumentProcessedResponseMessage message =
                successMessage(
                        successfulPayload(
                                FIRST_RESULT_ID,
                                "EXAME_HEMOGRAMA",
                                "2026-07-10",
                                FIRST_SUMMARY
                        )
                );

        listener.consume(message);
        listener.consume(message);

        assertThat(inboxRepository.count())
                .isEqualTo(1);

        HealthDocumentJpaEntity document =
                findDocument();

        assertThat(document.getProcessingStatus())
                .isEqualTo(
                        DocumentProcessingStatus.PROCESSED.name()
                );

        assertThat(document.getSummary())
                .isEqualTo(FIRST_SUMMARY);
    }

    @Test
    void shouldStoreMultipleResultsWithoutOverwritingFirstSummary() {
        listener.consume(
                successMessage(
                        successfulPayload(
                                FIRST_RESULT_ID,
                                "EXAME_HEMOGRAMA",
                                "2026-07-10",
                                FIRST_SUMMARY
                        )
                )
        );

        listener.consume(
                successMessage(
                        successfulPayload(
                                SECOND_RESULT_ID,
                                "LAUDO_IMAGEM",
                                "2026-07-11",
                                SECOND_SUMMARY
                        )
                )
        );

        List<DocumentProcessedInboxJpaEntity> results =
                inboxRepository
                        .findByDocumentIdOrderByReceivedAtAsc(documentId);

        assertThat(results)
                .hasSize(2)
                .extracting(
                        DocumentProcessedInboxJpaEntity::
                                getExternalResultId
                )
                .containsExactlyInAnyOrder(
                        FIRST_RESULT_ID,
                        SECOND_RESULT_ID
                );

        assertThat(results)
                .extracting(
                        DocumentProcessedInboxJpaEntity::
                                getExternalDocumentType
                )
                .containsExactlyInAnyOrder(
                        "EXAME_HEMOGRAMA",
                        "LAUDO_IMAGEM"
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

        assertThat(document.getSummary())
                .isEqualTo(FIRST_SUMMARY);

        assertThat(document.getDocumentDate())
                .isEqualTo(LocalDate.of(2026, 7, 10));
    }

    @Test
    void shouldPersistFailureAndMarkDocumentAsFailed() {
        listener.consume(
                new DocumentProcessedResponseMessage(
                        eventId,
                        documentId,
                        patientId,
                        "FAILED",
                        null,
                        "Não foi possível interpretar o documento."
                )
        );

        List<DocumentProcessedInboxJpaEntity> results =
                inboxRepository
                        .findByDocumentIdOrderByReceivedAtAsc(documentId);

        assertThat(results).hasSize(1);

        DocumentProcessedInboxJpaEntity result =
                results.getFirst();

        assertThat(result.getExternalResultId())
                .isEqualTo("FAILED");

        assertThat(result.getStatus())
                .isEqualTo(DocumentProcessingStatus.FAILED);

        assertThat(result.getPayload())
                .isNull();

        assertThat(result.getErrorDetail())
                .isEqualTo(
                        "Não foi possível interpretar o documento."
                );

        HealthDocumentJpaEntity document =
                findDocument();

        assertThat(document.getProcessingStatus())
                .isEqualTo(
                        DocumentProcessingStatus.FAILED.name()
                );

        assertThat(document.getProcessedAt())
                .isNotNull();
    }

    @Test
    void shouldNotDowngradeProcessedDocumentWhenLateFailureArrives() {
        listener.consume(
                successMessage(
                        successfulPayload(
                                FIRST_RESULT_ID,
                                "EXAME_HEMOGRAMA",
                                "2026-07-10",
                                FIRST_SUMMARY
                        )
                )
        );

        listener.consume(
                new DocumentProcessedResponseMessage(
                        eventId,
                        documentId,
                        patientId,
                        "FAILED",
                        null,
                        "Falha recebida depois do resultado válido."
                )
        );

        List<DocumentProcessedInboxJpaEntity> results =
                inboxRepository
                        .findByDocumentIdOrderByReceivedAtAsc(documentId);

        assertThat(results).hasSize(2);

        assertThat(results)
                .extracting(
                        DocumentProcessedInboxJpaEntity::getStatus
                )
                .containsExactlyInAnyOrder(
                        DocumentProcessingStatus.PROCESSED,
                        DocumentProcessingStatus.FAILED
                );

        HealthDocumentJpaEntity document =
                findDocument();

        assertThat(document.getProcessingStatus())
                .isEqualTo(
                        DocumentProcessingStatus.PROCESSED.name()
                );

        assertThat(document.getSummary())
                .isEqualTo(FIRST_SUMMARY);

        assertThat(document.getDocumentType())
                .isEqualTo(
                        DocumentType.EXAME_LABORATORIAL.name()
                );
    }

    @Test
    void shouldRejectResponseWithoutMatchingOriginalRequest() {
        UUID unknownEventId = UUID.randomUUID();

        DocumentProcessedResponseMessage message =
                new DocumentProcessedResponseMessage(
                        unknownEventId,
                        documentId,
                        patientId,
                        null,
                        successfulPayload(
                                FIRST_RESULT_ID,
                                "EXAME_HEMOGRAMA",
                                "2026-07-10",
                                FIRST_SUMMARY
                        ),
                        null
                );

        assertThatThrownBy(
                () -> listener.consume(message)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "A resposta não corresponde a uma "
                                + "solicitação de processamento."
                );

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

    @Test
    void shouldPersistVersionOneSuccessfulResponse() {
        Map<String, Object> payload = successfulPayload(
                FIRST_RESULT_ID,
                "EXAME_HEMOGRAMA",
                "2026-07-10T09:30:00",
                FIRST_SUMMARY
        );

        listener.consume(
                new DocumentProcessedResponseMessage(
                        1,
                        "DOCUMENT_PROCESSED_RESPONSE",
                        Instant.parse("2026-07-21T18:00:00Z"),
                        eventId,
                        documentId,
                        patientId,
                        "PROCESSED",
                        payload,
                        null,
                        null
                )
        );

        List<DocumentProcessedInboxJpaEntity> results =
                inboxRepository
                        .findByDocumentIdOrderByReceivedAtAsc(documentId);

        assertThat(results).hasSize(1);

        DocumentProcessedInboxJpaEntity result =
                results.getFirst();

        assertThat(result.getSchemaVersion())
                .isEqualTo(1);

        assertThat(result.getOccurredAt())
                .isEqualTo(
                        Instant.parse("2026-07-21T18:00:00Z")
                );

        assertThat(result.getEventId())
                .isEqualTo(eventId);

        assertThat(result.getExternalResultId())
                .isEqualTo(FIRST_RESULT_ID);

        assertThat(result.getStatus())
                .isEqualTo(DocumentProcessingStatus.PROCESSED);

        assertThat(result.getErrorCode())
                .isNull();

        assertThat(result.getErrorDetail())
                .isNull();

        assertThat(result.getErrorRetryable())
                .isNull();

        HealthDocumentJpaEntity document =
                findDocument();

        assertThat(document.getProcessingStatus())
                .isEqualTo(
                        DocumentProcessingStatus.PROCESSED.name()
                );

        assertThat(document.getSummary())
                .isEqualTo(FIRST_SUMMARY);
    }

    @Test
    void shouldPersistVersionOneStructuredFailure() {
        listener.consume(
                new DocumentProcessedResponseMessage(
                        1,
                        "DOCUMENT_PROCESSED_RESPONSE",
                        Instant.parse("2026-07-21T18:00:00Z"),
                        eventId,
                        documentId,
                        patientId,
                        "FAILED",
                        null,
                        new DocumentProcessingErrorMessage(
                                "AI_QUOTA_EXCEEDED",
                                "Limite temporário do serviço de IA atingido.",
                                true
                        ),
                        null
                )
        );

        List<DocumentProcessedInboxJpaEntity> results =
                inboxRepository
                        .findByDocumentIdOrderByReceivedAtAsc(documentId);

        assertThat(results).hasSize(1);

        DocumentProcessedInboxJpaEntity result =
                results.getFirst();

        assertThat(result.getSchemaVersion())
                .isEqualTo(1);

        assertThat(result.getOccurredAt())
                .isEqualTo(
                        Instant.parse("2026-07-21T18:00:00Z")
                );

        assertThat(result.getExternalResultId())
                .isEqualTo("FAILED");

        assertThat(result.getStatus())
                .isEqualTo(DocumentProcessingStatus.FAILED);

        assertThat(result.getPayload())
                .isNull();

        assertThat(result.getErrorCode())
                .isEqualTo("AI_QUOTA_EXCEEDED");

        assertThat(result.getErrorDetail())
                .isEqualTo(
                        "Limite temporário do serviço de IA atingido."
                );

        assertThat(result.getErrorRetryable())
                .isTrue();

        HealthDocumentJpaEntity document =
                findDocument();

        assertThat(document.getProcessingStatus())
                .isEqualTo(
                        DocumentProcessingStatus.FAILED.name()
                );

        assertThat(document.getProcessedAt())
                .isNotNull();
    }
    private DocumentProcessedResponseMessage successMessage(
            Map<String, Object> payload
    ) {
        return new DocumentProcessedResponseMessage(
                eventId,
                documentId,
                patientId,
                null,
                payload,
                null
        );
    }

    private Map<String, Object> successfulPayload(
            String resultId,
            String documentType,
            String documentDate,
            String summary
    ) {
        Map<String, Object> payload =
                new LinkedHashMap<>();

        payload.put("id", resultId);
        payload.put("patientId", patientId.toString());
        payload.put("documentType", documentType);
        payload.put("documentDate", documentDate);
        payload.put("descricaoGeral", summary);

        return payload;
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
                "Paciente do processamento de IA",
                LocalDate.of(1985, 5, 20),
                null,
                "processed-response@test.local",
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
                "exame-processamento.pdf",
                "arquivo-processamento.pdf",
                "/documentos/arquivo-processamento.pdf",
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

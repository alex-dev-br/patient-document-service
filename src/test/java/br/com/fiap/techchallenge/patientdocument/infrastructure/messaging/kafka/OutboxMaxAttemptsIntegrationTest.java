package br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka;

import br.com.fiap.techchallenge.patientdocument.TestcontainersConfiguration;
import br.com.fiap.techchallenge.patientdocument.domain.document.DocumentProcessingStatus;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.outbox.DocumentProcessingOutboxJpaEntity;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.outbox.DocumentProcessingOutboxJpaRepository;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.outbox.DocumentProcessingOutboxStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest(
        properties = {
                "app.messaging.kafka.enabled=false"
        }
)
@Import(TestcontainersConfiguration.class)
class OutboxMaxAttemptsIntegrationTest {

    private static final int MAX_ATTEMPTS = 3;

    private static final String ORIGINAL_ERROR_DETAIL =
            "Kafka permaneceu indisponível após o limite de tentativas.";

    @Autowired
    private DocumentProcessingOutboxJpaRepository outboxRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        insertFailedOutboxEventAtMaximumAttempts();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void shouldNotProcessFailedEventAtMaximumAttempts() {
        DocumentProcessingOutboxProcessor processor =
                mock(DocumentProcessingOutboxProcessor.class);

        DocumentProcessingOutboxScheduler scheduler =
                new DocumentProcessingOutboxScheduler(
                        outboxRepository,
                        processor,
                        MAX_ATTEMPTS
                );

        scheduler.publishPendingEvents();

        /*
         * Caso o evento fosse selecionado, o scheduler chamaria
         * processor.process(eventId).
         *
         * A ausência de qualquer interação confirma que nenhum
         * processamento ou tentativa de publicação foi iniciado.
         */
        verifyNoInteractions(processor);

        DocumentProcessingOutboxJpaEntity persistedEvent =
                findOutboxEvent();

        assertThat(persistedEvent.getStatus())
                .isEqualTo(
                        DocumentProcessingOutboxStatus.FAILED
                );

        assertThat(persistedEvent.getAttemptCount())
                .isEqualTo(MAX_ATTEMPTS);

        assertThat(persistedEvent.getErrorDetail())
                .isEqualTo(ORIGINAL_ERROR_DETAIL);

        assertThat(persistedEvent.getPublishedAt())
                .isNull();
    }

    private DocumentProcessingOutboxJpaEntity findOutboxEvent() {
        return outboxRepository
                .findById(eventId)
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
                "Paciente do teste de limite da Outbox",
                LocalDate.of(1985, 5, 20),
                null,
                "outbox-max-attempts@test.local",
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
                "exame-outbox-max-attempts.pdf",
                "arquivo-outbox-max-attempts.pdf",
                "/documentos/arquivo-outbox-max-attempts.pdf",
                "application/pdf",
                2048L,
                DocumentProcessingStatus
                        .PENDING_PROCESSING
                        .name(),
                LocalDateTime.now()
        );
    }

    private void insertFailedOutboxEventAtMaximumAttempts() {
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
                DocumentProcessingOutboxStatus.FAILED.name(),
                MAX_ATTEMPTS,
                ORIGINAL_ERROR_DETAIL,
                LocalDateTime.now().minusMinutes(5),
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

package br.com.fiap.techchallenge.patientdocument.application.document.usecase;

import br.com.fiap.techchallenge.patientdocument.TestcontainersConfiguration;
import br.com.fiap.techchallenge.patientdocument.application.document.command.UploadHealthDocumentCommand;
import br.com.fiap.techchallenge.patientdocument.application.document.event.DocumentProcessingRequestedEvent;
import br.com.fiap.techchallenge.patientdocument.application.document.gateway.DocumentProcessingEventGateway;
import br.com.fiap.techchallenge.patientdocument.application.exception.ResourceNotFoundException;
import br.com.fiap.techchallenge.patientdocument.application.exception.StorageException;
import br.com.fiap.techchallenge.patientdocument.application.storage.command.StoreFileCommand;
import br.com.fiap.techchallenge.patientdocument.application.storage.gateway.StorageGateway;
import br.com.fiap.techchallenge.patientdocument.application.storage.result.StoredFile;
import br.com.fiap.techchallenge.patientdocument.domain.document.DocumentProcessingStatus;
import br.com.fiap.techchallenge.patientdocument.domain.document.HealthDocument;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.document.HealthDocumentJpaEntity;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.document.HealthDocumentJpaRepository;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.outbox.DocumentProcessingOutboxJpaEntity;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.outbox.DocumentProcessingOutboxJpaRepository;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.outbox.DocumentProcessingOutboxStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(
        properties = {
                "app.messaging.kafka.enabled=false"
        }
)
@Import(TestcontainersConfiguration.class)
class UploadHealthDocumentIntegrationTest {

    private static final String ORIGINAL_FILE_NAME =
            "exame-integracao.pdf";

    private static final String STORED_FILE_NAME =
            "arquivo-nextcloud-001.pdf";

    private static final String STORAGE_PATH =
            "http://nextcloud.local/remote.php/dav/files/"
                    + "nextcloud/20260716/arquivo-nextcloud-001.pdf";

    private static final String CONTENT_TYPE =
            "application/pdf";

    private static final byte[] FILE_CONTENT =
            "conteúdo do exame de integração"
                    .getBytes(StandardCharsets.UTF_8);

    @Autowired
    private UploadHealthDocumentUseCase uploadHealthDocumentUseCase;

    @Autowired
    private HealthDocumentJpaRepository healthDocumentRepository;

    @Autowired
    private DocumentProcessingOutboxJpaRepository outboxRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean(
            name = "nextcloudStorageGateway",
            enforceOverride = true
    )
    private StorageGateway storageGateway;

    @MockitoSpyBean
    private DocumentProcessingEventGateway documentProcessingEventGateway;

    private UUID patientId;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        patientId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void shouldPersistDocumentAndOutboxEventInSameTransaction() {
        insertPatient();
        configureSuccessfulStorage();

        HealthDocument savedDocument =
                uploadHealthDocumentUseCase.execute(
                        validUploadCommand()
                );

        assertThat(healthDocumentRepository.count())
                .isEqualTo(1);

        assertThat(outboxRepository.count())
                .isEqualTo(1);

        HealthDocumentJpaEntity persistedDocument =
                healthDocumentRepository
                        .findById(savedDocument.getId())
                        .orElseThrow();

        assertThat(persistedDocument.getPatientId())
                .isEqualTo(patientId);

        assertThat(persistedDocument.getOriginalFileName())
                .isEqualTo(ORIGINAL_FILE_NAME);

        assertThat(persistedDocument.getStoredFileName())
                .isEqualTo(STORED_FILE_NAME);

        assertThat(persistedDocument.getStoragePath())
                .isEqualTo(STORAGE_PATH);

        assertThat(persistedDocument.getContentType())
                .isEqualTo(CONTENT_TYPE);

        assertThat(persistedDocument.getFileSize())
                .isEqualTo((long) FILE_CONTENT.length);

        assertThat(persistedDocument.getProcessingStatus())
                .isEqualTo(
                        DocumentProcessingStatus.PENDING_PROCESSING.name()
                );

        List<DocumentProcessingOutboxJpaEntity> outboxEvents =
                outboxRepository.findAll();

        assertThat(outboxEvents).hasSize(1);

        DocumentProcessingOutboxJpaEntity outboxEvent =
                outboxEvents.getFirst();

        assertThat(outboxEvent.getDocumentId())
                .isEqualTo(savedDocument.getId());

        assertThat(outboxEvent.getPatientId())
                .isEqualTo(patientId);

        assertThat(outboxEvent.getStatus())
                .isEqualTo(DocumentProcessingOutboxStatus.PENDING);

        assertThat(outboxEvent.getAttemptCount())
                .isZero();

        assertThat(outboxEvent.getCreatedAt())
                .isNotNull();

        assertThat(outboxEvent.getPublishedAt())
                .isNull();

        ArgumentCaptor<DocumentProcessingRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(
                        DocumentProcessingRequestedEvent.class
                );

        verify(storageGateway)
                .store(any(StoreFileCommand.class));

        verify(documentProcessingEventGateway)
                .enqueue(eventCaptor.capture());

        DocumentProcessingRequestedEvent capturedEvent =
                eventCaptor.getValue();

        assertThat(capturedEvent.eventId())
                .isEqualTo(outboxEvent.getEventId());

        assertThat(capturedEvent.documentId())
                .isEqualTo(savedDocument.getId());

        assertThat(capturedEvent.patientId())
                .isEqualTo(patientId);

        verify(storageGateway, never())
                .delete(any(String.class));
    }

    @Test
    void shouldRollbackDocumentWhenOutboxEnqueueFails() {
        insertPatient();
        configureSuccessfulStorage();

        doThrow(
                new IllegalStateException(
                        "Falha forçada ao gravar a Outbox."
                )
        ).when(documentProcessingEventGateway)
                .enqueue(any(DocumentProcessingRequestedEvent.class));

        assertThatThrownBy(
                () -> uploadHealthDocumentUseCase.execute(
                        validUploadCommand()
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Falha forçada ao gravar a Outbox."
                );

        assertThat(healthDocumentRepository.count())
                .isZero();

        assertThat(outboxRepository.count())
                .isZero();

        verify(storageGateway)
                .store(any(StoreFileCommand.class));

        verify(documentProcessingEventGateway)
                .enqueue(any(DocumentProcessingRequestedEvent.class));

        verify(storageGateway)
                .delete(STORAGE_PATH);
    }

    @Test
    void shouldNotStoreFileWhenPatientDoesNotExist() {
        assertThatThrownBy(
                () -> uploadHealthDocumentUseCase.execute(
                        validUploadCommand()
                )
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(
                        "Paciente não encontrado"
                );

        assertThat(healthDocumentRepository.count())
                .isZero();

        assertThat(outboxRepository.count())
                .isZero();

        verify(storageGateway, never())
                .store(any(StoreFileCommand.class));

        verify(documentProcessingEventGateway, never())
                .enqueue(any(DocumentProcessingRequestedEvent.class));

        verify(storageGateway, never())
                .delete(any(String.class));
    }

    @Test
    void shouldRejectEmptyFileBeforeCallingStorage() {
        insertPatient();

        UploadHealthDocumentCommand emptyFileCommand =
                new UploadHealthDocumentCommand(
                        patientId,
                        ORIGINAL_FILE_NAME,
                        CONTENT_TYPE,
                        0L,
                        new ByteArrayInputStream(new byte[0])
                );

        assertThatThrownBy(
                () -> uploadHealthDocumentUseCase.execute(
                        emptyFileCommand
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "O arquivo enviado está vazio."
                );

        assertThat(healthDocumentRepository.count())
                .isZero();

        assertThat(outboxRepository.count())
                .isZero();

        verify(storageGateway, never())
                .store(any(StoreFileCommand.class));

        verify(documentProcessingEventGateway, never())
                .enqueue(any(DocumentProcessingRequestedEvent.class));

        verify(storageGateway, never())
                .delete(any(String.class));
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
                "Paciente do teste de upload",
                LocalDate.of(1990, 1, 1),
                null,
                "upload-integration@test.local",
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

    @Test
    void shouldPreserveOriginalExceptionWhenStorageCleanupFails() {
        insertPatient();
        configureSuccessfulStorage();

        IllegalStateException originalException =
                new IllegalStateException(
                        "Falha original ao gravar a Outbox."
                );

        StorageException cleanupException =
                new StorageException(
                        "Falha ao excluir o arquivo do Nextcloud."
                );

        doThrow(originalException)
                .when(documentProcessingEventGateway)
                .enqueue(any(DocumentProcessingRequestedEvent.class));

        doThrow(cleanupException)
                .when(storageGateway)
                .delete(STORAGE_PATH);

        Throwable thrownException = catchThrowable(
                () -> uploadHealthDocumentUseCase.execute(
                        validUploadCommand()
                )
        );

        assertThat(thrownException)
                .isSameAs(originalException);

        assertThat(thrownException.getSuppressed())
                .containsExactly(cleanupException);

        assertThat(healthDocumentRepository.count())
                .isZero();

        assertThat(outboxRepository.count())
                .isZero();

        verify(storageGateway)
                .store(any(StoreFileCommand.class));

        verify(storageGateway)
                .delete(STORAGE_PATH);
    }
}

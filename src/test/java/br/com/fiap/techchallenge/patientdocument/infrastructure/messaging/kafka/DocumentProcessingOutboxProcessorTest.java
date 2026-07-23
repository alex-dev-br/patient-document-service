package br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka;

import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.document.HealthDocumentJpaEntity;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.document.HealthDocumentJpaRepository;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.outbox.DocumentProcessingOutboxJpaEntity;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.outbox.DocumentProcessingOutboxJpaRepository;
import br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.outbox.DocumentProcessingOutboxStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentProcessingOutboxProcessorTest {

    private static final String TOPIC =
            "document-processing-requested";

    private static final String FILE_URL =
            "http://nextcloud-service/remote.php/dav/files/"
                    + "nextcloud/20260723/documento.png";

    private static final String CONTENT_TYPE =
            "image/png";

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "e9c28d3e-8092-4aa8-adc4-b58776ab846e"
            );

    private static final UUID DOCUMENT_ID =
            UUID.fromString(
                    "ab15e266-f35f-431c-8ddd-ce6044c42bc7"
            );

    private static final UUID PATIENT_ID =
            UUID.fromString(
                    "a531af40-fbaa-4326-940e-7f75298986cb"
            );

    private static final LocalDateTime CREATED_AT =
            LocalDateTime.of(
                    2026,
                    7,
                    23,
                    9,
                    8,
                    37
            );

    @Mock
    private DocumentProcessingOutboxJpaRepository repository;

    @Mock
    private HealthDocumentJpaRepository
            healthDocumentJpaRepository;

    @Mock
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Mock
    private DocumentProcessingOutboxJpaEntity outboxEvent;

    @Mock
    private HealthDocumentJpaEntity document;

    private DocumentProcessingOutboxProcessor processor;

    @BeforeEach
    void setUp() {
        processor =
                new DocumentProcessingOutboxProcessor(
                        repository,
                        healthDocumentJpaRepository,
                        kafkaTemplate,
                        TOPIC,
                        "http://patient-document-service:8443",
                        10
                );
    }

    @Test
    void shouldPublishContentTypeFromPersistedDocument()
            throws Exception {
        configurePendingEventForMessage();

        when(healthDocumentJpaRepository.findById(DOCUMENT_ID))
                .thenReturn(Optional.of(document));

        when(document.getStoragePath())
                .thenReturn(FILE_URL);

        when(document.getContentType())
                .thenReturn(CONTENT_TYPE);

        CompletableFuture<SendResult<Object, Object>>
                completedFuture =
                CompletableFuture.completedFuture(null);

        when(
                kafkaTemplate.send(
                        eq(TOPIC),
                        eq(DOCUMENT_ID.toString()),
                        any(
                                DocumentProcessingRequestedMessage
                                        .class
                        )
                )
        ).thenReturn(completedFuture);

        processor.process(EVENT_ID);

        ArgumentCaptor<DocumentProcessingRequestedMessage>
                messageCaptor =
                ArgumentCaptor.forClass(
                        DocumentProcessingRequestedMessage.class
                );

        verify(kafkaTemplate)
                .send(
                        eq(TOPIC),
                        eq(DOCUMENT_ID.toString()),
                        messageCaptor.capture()
                );

        DocumentProcessingRequestedMessage message =
                messageCaptor.getValue();

        Instant expectedOccurredAt =
                CREATED_AT
                        .atZone(
                                ZoneId.of(
                                        "America/Sao_Paulo"
                                )
                        )
                        .toInstant();

        assertThat(message.schemaVersion())
                .isEqualTo(1);

        assertThat(message.eventType())
                .isEqualTo(
                        "DOCUMENT_PROCESSING_REQUESTED"
                );

        assertThat(message.occurredAt())
                .isEqualTo(expectedOccurredAt);

        assertThat(message.eventId())
                .isEqualTo(EVENT_ID);

        assertThat(message.documentId())
                .isEqualTo(DOCUMENT_ID);

        assertThat(message.patientId())
                .isEqualTo(PATIENT_ID);

        assertThat(message.fileUrl())
                .isEqualTo(FILE_URL);

        assertThat(message.contentType())
                .isEqualTo(CONTENT_TYPE);

        verify(outboxEvent)
                .registerAttempt();

        verify(outboxEvent)
                .markPublished();

        verify(outboxEvent, never())
                .markFailed(any(String.class));
    }

    @Test
    void shouldMarkEventAsFailedWhenDocumentDoesNotExist() {
        configurePendingEventLookup();

        when(healthDocumentJpaRepository.findById(DOCUMENT_ID))
                .thenReturn(Optional.empty());

        processor.process(EVENT_ID);

        verify(outboxEvent)
                .registerAttempt();

        verify(outboxEvent)
                .markFailed(
                        "Documento não encontrado para "
                                + "publicação da Outbox: "
                                + DOCUMENT_ID
                );

        verify(outboxEvent, never())
                .markPublished();

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldMarkEventAsFailedWhenContentTypeIsBlank() {
        configurePendingEventForMessage();

        when(healthDocumentJpaRepository.findById(DOCUMENT_ID))
                .thenReturn(Optional.of(document));

        when(document.getStoragePath())
                .thenReturn(FILE_URL);

        when(document.getContentType())
                .thenReturn(" ");

        processor.process(EVENT_ID);

        verify(outboxEvent)
                .registerAttempt();

        verify(outboxEvent)
                .markFailed(
                        "O contentType é obrigatório."
                );

        verify(outboxEvent, never())
                .markPublished();

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldIgnoreAlreadyPublishedEvent() {
        when(repository.findById(EVENT_ID))
                .thenReturn(Optional.of(outboxEvent));

        when(outboxEvent.getStatus())
                .thenReturn(
                        DocumentProcessingOutboxStatus.PUBLISHED
                );

        processor.process(EVENT_ID);

        verify(outboxEvent, never())
                .registerAttempt();

        verifyNoInteractions(
                healthDocumentJpaRepository,
                kafkaTemplate
        );
    }

    private void configurePendingEventLookup() {
        when(repository.findById(EVENT_ID))
                .thenReturn(Optional.of(outboxEvent));

        when(outboxEvent.getStatus())
                .thenReturn(
                        DocumentProcessingOutboxStatus.PENDING
                );

        when(outboxEvent.getDocumentId())
                .thenReturn(DOCUMENT_ID);
    }

    private void configurePendingEventForMessage() {
        configurePendingEventLookup();

        when(outboxEvent.getEventId())
                .thenReturn(EVENT_ID);

        when(outboxEvent.getPatientId())
                .thenReturn(PATIENT_ID);

        when(outboxEvent.getCreatedAt())
                .thenReturn(CREATED_AT);
    }
}
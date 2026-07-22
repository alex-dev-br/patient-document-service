package br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka;

import br.com.fiap.techchallenge.patientdocument.application.document.command.ProcessDocumentProcessedResponseCommand;
import br.com.fiap.techchallenge.patientdocument.application.document.usecase.ProcessDocumentProcessedResponseUseCase;
import br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka.validation.DocumentProcessedResponseValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DocumentProcessedResponseListenerTest {

    private static final UUID EVENT_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID DOCUMENT_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID PATIENT_ID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final Instant OCCURRED_AT =
            Instant.parse("2026-07-21T18:00:00Z");

    @Mock
    private ProcessDocumentProcessedResponseUseCase useCase;

    private DocumentProcessedResponseListener listener;

    @BeforeEach
    void setUp() {
        listener = new DocumentProcessedResponseListener(
                useCase,
                new DocumentProcessedResponseValidator()
        );
    }

    @Test
    void shouldConvertStructuredErrorIntoCurrentCommandFormat() {
        DocumentProcessedResponseMessage message =
                new DocumentProcessedResponseMessage(
                        1,
                        "DOCUMENT_PROCESSED_RESPONSE",
                        OCCURRED_AT,
                        EVENT_ID,
                        DOCUMENT_ID,
                        PATIENT_ID,
                        "FAILED",
                        null,
                        new DocumentProcessingErrorMessage(
                                "AI_QUOTA_EXCEEDED",
                                "Limite temporário do serviço de IA atingido.",
                                true
                        ),
                        null
                );

        listener.consume(message);

        ArgumentCaptor<ProcessDocumentProcessedResponseCommand> captor =
                ArgumentCaptor.forClass(
                        ProcessDocumentProcessedResponseCommand.class
                );

        verify(useCase).execute(captor.capture());

        ProcessDocumentProcessedResponseCommand command =
                captor.getValue();

        assertThat(command.eventId()).isEqualTo(EVENT_ID);
        assertThat(command.documentId()).isEqualTo(DOCUMENT_ID);
        assertThat(command.patientId()).isEqualTo(PATIENT_ID);
        assertThat(command.schemaVersion()).isEqualTo(1);
        assertThat(command.occurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(command.status()).isEqualTo("FAILED");
        assertThat(command.document()).isNull();
        assertThat(command.errorCode())
                .isEqualTo("AI_QUOTA_EXCEEDED");
        assertThat(command.errorDetail())
                .isEqualTo(
                        "Limite temporário do serviço de IA atingido."
                );
        assertThat(command.errorRetryable()).isTrue();
    }

    @Test
    void shouldKeepLegacyErrorDetail() {
        DocumentProcessedResponseMessage message =
                new DocumentProcessedResponseMessage(
                        EVENT_ID,
                        DOCUMENT_ID,
                        PATIENT_ID,
                        "FAILED",
                        null,
                        "Falha no formato legado."
                );

        listener.consume(message);

        ArgumentCaptor<ProcessDocumentProcessedResponseCommand> captor =
                ArgumentCaptor.forClass(
                        ProcessDocumentProcessedResponseCommand.class
                );

        verify(useCase).execute(captor.capture());

        assertThat(captor.getValue().errorDetail())
                .isEqualTo("Falha no formato legado.");
    }

    @Test
    void shouldRejectInvalidVersionOneMessageBeforeUseCase() {
        DocumentProcessedResponseMessage message =
                new DocumentProcessedResponseMessage(
                        1,
                        "INVALID_EVENT_TYPE",
                        OCCURRED_AT,
                        EVENT_ID,
                        DOCUMENT_ID,
                        PATIENT_ID,
                        "FAILED",
                        null,
                        new DocumentProcessingErrorMessage(
                                "AI_PROCESSING_FAILED",
                                "Falha no processamento.",
                                false
                        ),
                        null
                );

        assertThatThrownBy(() -> listener.consume(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "eventType inválido: INVALID_EVENT_TYPE"
                );

        verifyNoInteractions(useCase);
    }
}

package br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentProcessingRequestedMessageTest {

    private static final Instant OCCURRED_AT =
            Instant.parse("2026-07-22T03:00:00Z");

    private static final String FILE_URL =
            "https://patient-document-service:8443/"
                    + "documents/documento/file";

    private static final String CONTENT_TYPE =
            "application/pdf";

    @Test
    void shouldCreateVersionOneMessage() {
        UUID eventId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        DocumentProcessingRequestedMessage message =
                DocumentProcessingRequestedMessage.versionOne(
                        OCCURRED_AT,
                        eventId,
                        documentId,
                        patientId,
                        FILE_URL,
                        CONTENT_TYPE
                );

        assertThat(message.schemaVersion())
                .isEqualTo(1);

        assertThat(message.eventType())
                .isEqualTo(
                        "DOCUMENT_PROCESSING_REQUESTED"
                );

        assertThat(message.occurredAt())
                .isEqualTo(OCCURRED_AT);

        assertThat(message.eventId())
                .isEqualTo(eventId);

        assertThat(message.documentId())
                .isEqualTo(documentId);

        assertThat(message.patientId())
                .isEqualTo(patientId);

        assertThat(message.fileUrl())
                .isEqualTo(FILE_URL);

        assertThat(message.contentType())
                .isEqualTo(CONTENT_TYPE);
    }

    @Test
    void shouldRejectUnsupportedSchemaVersion() {
        assertThatThrownBy(
                () -> new DocumentProcessingRequestedMessage(
                        2,
                        DocumentProcessingRequestedMessage.EVENT_TYPE,
                        OCCURRED_AT,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        FILE_URL,
                        CONTENT_TYPE
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "A schemaVersion deve ser igual a 1."
                );
    }

    @Test
    void shouldRejectInvalidEventType() {
        assertThatThrownBy(
                () -> new DocumentProcessingRequestedMessage(
                        DocumentProcessingRequestedMessage
                                .SCHEMA_VERSION,
                        "INVALID_EVENT_TYPE",
                        OCCURRED_AT,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        FILE_URL,
                        CONTENT_TYPE
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "DOCUMENT_PROCESSING_REQUESTED"
                );
    }

    @Test
    void shouldRejectBlankFileUrl() {
        assertThatThrownBy(
                () -> DocumentProcessingRequestedMessage
                        .versionOne(
                                OCCURRED_AT,
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                " ",
                                CONTENT_TYPE
                        )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "O fileUrl é obrigatório."
                );
    }

    @Test
    void shouldRejectNullContentType() {
        assertThatThrownBy(
                () -> DocumentProcessingRequestedMessage
                        .versionOne(
                                OCCURRED_AT,
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                FILE_URL,
                                null
                        )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "O contentType é obrigatório."
                );
    }

    @Test
    void shouldRejectBlankContentType() {
        assertThatThrownBy(
                () -> DocumentProcessingRequestedMessage
                        .versionOne(
                                OCCURRED_AT,
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                FILE_URL,
                                " "
                        )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "O contentType é obrigatório."
                );
    }

    @Test
    void shouldRejectContentTypeAboveMaximumLength() {
        assertThatThrownBy(
                () -> DocumentProcessingRequestedMessage
                        .versionOne(
                                OCCURRED_AT,
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                FILE_URL,
                                "a".repeat(101)
                        )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "O contentType deve possuir no máximo "
                                + "100 caracteres."
                );
    }
}
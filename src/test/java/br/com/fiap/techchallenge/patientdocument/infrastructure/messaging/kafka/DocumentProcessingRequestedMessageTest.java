package br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentProcessingRequestedMessageTest {

    private static final Instant OCCURRED_AT =
            Instant.parse("2026-07-22T03:00:00Z");

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
                        "/documentos/exame.pdf"
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
                .isEqualTo("/documentos/exame.pdf");
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
                        "/documentos/exame.pdf"
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
                        "/documentos/exame.pdf"
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
                                " "
                        )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "O fileUrl é obrigatório."
                );
    }
}

package br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka.validation;

import br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka.DocumentProcessedResponseMessage;
import br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka.DocumentProcessingErrorMessage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentProcessedResponseValidatorTest {

    private static final UUID EVENT_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID DOCUMENT_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID PATIENT_ID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final Instant OCCURRED_AT =
            Instant.parse("2026-07-21T18:00:00Z");

    private final DocumentProcessedResponseValidator validator =
            new DocumentProcessedResponseValidator();

    @Test
    void shouldRecognizeLegacyResponse() {
        DocumentProcessedResponseMessage message =
                legacySuccess();

        assertThat(validator.isLegacy(message))
                .isTrue();

        assertThatCode(() -> validator.validate(message))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAcceptLegacyFailure() {
        DocumentProcessedResponseMessage message =
                new DocumentProcessedResponseMessage(
                        EVENT_ID,
                        DOCUMENT_ID,
                        PATIENT_ID,
                        "FAILED",
                        null,
                        "Falha legada."
                );

        assertThatCode(() -> validator.validate(message))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAcceptVersionOneSuccessfulResponse() {
        DocumentProcessedResponseMessage message =
                versionOneSuccess();

        assertThat(validator.isLegacy(message))
                .isFalse();

        assertThatCode(() -> validator.validate(message))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAcceptVersionOneFailureResponse() {
        DocumentProcessedResponseMessage message =
                versionOneFailure();

        assertThatCode(() -> validator.validate(message))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectMissingCommonIdentifier() {
        DocumentProcessedResponseMessage message =
                new DocumentProcessedResponseMessage(
                        1,
                        "DOCUMENT_PROCESSED_RESPONSE",
                        OCCURRED_AT,
                        null,
                        DOCUMENT_ID,
                        PATIENT_ID,
                        "PROCESSED",
                        Map.of("id", "resultado-001"),
                        null,
                        null
                );

        assertThatThrownBy(() -> validator.validate(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O eventId é obrigatório.");
    }

    @Test
    void shouldRejectUnsupportedSchemaVersion() {
        DocumentProcessedResponseMessage message =
                new DocumentProcessedResponseMessage(
                        2,
                        "DOCUMENT_PROCESSED_RESPONSE",
                        OCCURRED_AT,
                        EVENT_ID,
                        DOCUMENT_ID,
                        PATIENT_ID,
                        "PROCESSED",
                        Map.of("id", "resultado-001"),
                        null,
                        null
                );

        assertThatThrownBy(() -> validator.validate(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("schemaVersion não suportada: 2");
    }

    @Test
    void shouldRejectInvalidEventType() {
        DocumentProcessedResponseMessage message =
                new DocumentProcessedResponseMessage(
                        1,
                        "INVALID_EVENT_TYPE",
                        OCCURRED_AT,
                        EVENT_ID,
                        DOCUMENT_ID,
                        PATIENT_ID,
                        "PROCESSED",
                        Map.of("id", "resultado-001"),
                        null,
                        null
                );

        assertThatThrownBy(() -> validator.validate(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("eventType inválido: INVALID_EVENT_TYPE");
    }

    @Test
    void shouldRejectVersionOneWithoutOccurredAt() {
        DocumentProcessedResponseMessage message =
                new DocumentProcessedResponseMessage(
                        1,
                        "DOCUMENT_PROCESSED_RESPONSE",
                        null,
                        EVENT_ID,
                        DOCUMENT_ID,
                        PATIENT_ID,
                        "PROCESSED",
                        Map.of("id", "resultado-001"),
                        null,
                        null
                );

        assertThatThrownBy(() -> validator.validate(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "O occurredAt é obrigatório no contrato v1."
                );
    }

    @Test
    void shouldRejectProcessedResponseWithError() {
        DocumentProcessedResponseMessage message =
                new DocumentProcessedResponseMessage(
                        1,
                        "DOCUMENT_PROCESSED_RESPONSE",
                        OCCURRED_AT,
                        EVENT_ID,
                        DOCUMENT_ID,
                        PATIENT_ID,
                        "PROCESSED",
                        Map.of("id", "resultado-001"),
                        new DocumentProcessingErrorMessage(
                                "UNEXPECTED_ERROR",
                                "Erro inesperado.",
                                false
                        ),
                        null
                );

        assertThatThrownBy(() -> validator.validate(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Uma resposta PROCESSED não pode conter error."
                );
    }

    @Test
    void shouldRejectFailedResponseWithoutStructuredError() {
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
                        null,
                        null
                );

        assertThatThrownBy(() -> validator.validate(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Uma resposta FAILED precisa conter error."
                );
    }

    @Test
    void shouldRejectInvalidStructuredErrorCode() {
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
                                "invalid-code",
                                "Erro inválido.",
                                false
                        ),
                        null
                );

        assertThatThrownBy(() -> validator.validate(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "O error.code possui formato inválido."
                );
    }

    @Test
    void shouldRejectStructuredErrorWithoutRetryable() {
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
                                "AI_PROCESSING_FAILED",
                                "Falha no processamento.",
                                null
                        ),
                        null
                );

        assertThatThrownBy(() -> validator.validate(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "O error.retryable é obrigatório."
                );
    }

    @Test
    void shouldRejectNullMessage() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "A mensagem de resposta é obrigatória."
                );
    }

    @Test
    void shouldRejectNonCanonicalVersionOneStatus() {
        DocumentProcessedResponseMessage message =
                new DocumentProcessedResponseMessage(
                        1,
                        "DOCUMENT_PROCESSED_RESPONSE",
                        OCCURRED_AT,
                        EVENT_ID,
                        DOCUMENT_ID,
                        PATIENT_ID,
                        "processed",
                        Map.of("id", "resultado-001"),
                        null,
                        null
                );

        assertThatThrownBy(() -> validator.validate(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Status de resposta inválido: processed"
                );
    }
    @Test
    void shouldRejectProcessedResponseWithoutDocumentId() {
        DocumentProcessedResponseMessage message =
                new DocumentProcessedResponseMessage(
                        1,
                        "DOCUMENT_PROCESSED_RESPONSE",
                        OCCURRED_AT,
                        EVENT_ID,
                        DOCUMENT_ID,
                        PATIENT_ID,
                        "PROCESSED",
                        Map.of("descricaoGeral", "Documento sem id."),
                        null,
                        null
                );

        assertThatThrownBy(() -> validator.validate(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Uma resposta PROCESSED precisa conter document.id."
                );
    }
    @Test
    void shouldRejectProcessedResponseWithNonStringDocumentId() {
        DocumentProcessedResponseMessage message =
                new DocumentProcessedResponseMessage(
                        1,
                        "DOCUMENT_PROCESSED_RESPONSE",
                        OCCURRED_AT,
                        EVENT_ID,
                        DOCUMENT_ID,
                        PATIENT_ID,
                        "PROCESSED",
                        Map.<String, Object>of("id", 123),
                        null,
                        null
                );

        assertThatThrownBy(() -> validator.validate(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "O document.id precisa ser uma string."
                );
    }

    @Test
    void shouldRejectProcessedResponseWithDocumentIdAboveLimit() {
        DocumentProcessedResponseMessage message =
                new DocumentProcessedResponseMessage(
                        1,
                        "DOCUMENT_PROCESSED_RESPONSE",
                        OCCURRED_AT,
                        EVENT_ID,
                        DOCUMENT_ID,
                        PATIENT_ID,
                        "PROCESSED",
                        Map.of("id", "x".repeat(65)),
                        null,
                        null
                );

        assertThatThrownBy(() -> validator.validate(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "O document.id excede 64 caracteres."
                );
    }
    private DocumentProcessedResponseMessage legacySuccess() {
        return new DocumentProcessedResponseMessage(
                EVENT_ID,
                DOCUMENT_ID,
                PATIENT_ID,
                null,
                Map.of("id", "resultado-legado"),
                null
        );
    }

    private DocumentProcessedResponseMessage versionOneSuccess() {
        return new DocumentProcessedResponseMessage(
                1,
                "DOCUMENT_PROCESSED_RESPONSE",
                OCCURRED_AT,
                EVENT_ID,
                DOCUMENT_ID,
                PATIENT_ID,
                "PROCESSED",
                Map.of("id", "resultado-v1"),
                null,
                null
        );
    }

    private DocumentProcessedResponseMessage versionOneFailure() {
        return new DocumentProcessedResponseMessage(
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
    }
}

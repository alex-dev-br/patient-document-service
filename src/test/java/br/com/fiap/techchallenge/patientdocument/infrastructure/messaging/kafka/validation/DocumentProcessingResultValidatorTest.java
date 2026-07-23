package br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka.validation;

import br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka.DocumentProcessingErrorMessage;
import br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka.DocumentProcessingResultItemMessage;
import br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka.DocumentProcessingResultMessage;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentProcessingResultValidatorTest {

    private static final UUID RESPONSE_EVENT_ID =
            UUID.fromString(
                    "11111111-1111-1111-1111-111111111111"
            );

    private static final UUID CORRELATION_ID =
            UUID.fromString(
                    "22222222-2222-2222-2222-222222222222"
            );

    private static final UUID DOCUMENT_ID =
            UUID.fromString(
                    "33333333-3333-3333-3333-333333333333"
            );

    private static final UUID PATIENT_ID =
            UUID.fromString(
                    "44444444-4444-4444-4444-444444444444"
            );

    private static final Instant OCCURRED_AT =
            Instant.parse("2026-07-22T18:00:00Z");

    private static final LocalDate DOCUMENT_DATE =
            LocalDate.of(2026, 7, 10);

    private final DocumentProcessingResultValidator validator =
            new DocumentProcessingResultValidator();

    @Test
    void shouldAcceptCompletedResult() {
        assertThatCode(
                () -> validator.validate(completedMessage())
        )
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAcceptFailedResult() {
        assertThatCode(
                () -> validator.validate(failedMessage())
        )
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectMissingCorrelationId() {
        DocumentProcessingResultMessage message =
                new DocumentProcessingResultMessage(
                        1,
                        "DOCUMENT_PROCESSING_COMPLETED",
                        RESPONSE_EVENT_ID,
                        null,
                        OCCURRED_AT,
                        DOCUMENT_ID,
                        PATIENT_ID,
                        "Hemograma processado.",
                        "EXAME_HEMOGRAMA",
                        "EXAMES_LABORATORIAIS",
                        DOCUMENT_DATE,
                        new BigDecimal("0.92"),
                        List.of(validItem()),
                        null
                );

        assertThatThrownBy(
                () -> validator.validate(message)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "O correlationId é obrigatório."
                );
    }

    @Test
    void shouldRejectMissingEventType() {
        DocumentProcessingResultMessage message =
                new DocumentProcessingResultMessage(
                        1,
                        null,
                        RESPONSE_EVENT_ID,
                        CORRELATION_ID,
                        OCCURRED_AT,
                        DOCUMENT_ID,
                        PATIENT_ID,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        assertThatThrownBy(
                () -> validator.validate(message)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "O eventType é obrigatório."
                );
    }

    @Test
    void shouldRejectEventIdEqualToCorrelationId() {
        DocumentProcessingResultMessage message =
                new DocumentProcessingResultMessage(
                        1,
                        "DOCUMENT_PROCESSING_FAILED",
                        RESPONSE_EVENT_ID,
                        RESPONSE_EVENT_ID,
                        OCCURRED_AT,
                        DOCUMENT_ID,
                        PATIENT_ID,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        validError()
                );

        assertThatThrownBy(
                () -> validator.validate(message)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "O eventId deve ser diferente "
                                + "do correlationId."
                );
    }

    @Test
    void shouldRejectDuplicatedResultId() {
        DocumentProcessingResultMessage message =
                new DocumentProcessingResultMessage(
                        1,
                        "DOCUMENT_PROCESSING_COMPLETED",
                        RESPONSE_EVENT_ID,
                        CORRELATION_ID,
                        OCCURRED_AT,
                        DOCUMENT_ID,
                        PATIENT_ID,
                        "Hemograma processado.",
                        "EXAME_HEMOGRAMA",
                        null,
                        DOCUMENT_DATE,
                        null,
                        List.of(
                                validItem(),
                                validItem()
                        ),
                        null
                );

        assertThatThrownBy(
                () -> validator.validate(message)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "O resultId resultado-001 "
                                + "está duplicado."
                );
    }

    @Test
    void shouldRejectInvalidEventType() {
        DocumentProcessingResultMessage message =
                new DocumentProcessingResultMessage(
                        1,
                        "INVALID_EVENT_TYPE",
                        RESPONSE_EVENT_ID,
                        CORRELATION_ID,
                        OCCURRED_AT,
                        DOCUMENT_ID,
                        PATIENT_ID,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        assertThatThrownBy(
                () -> validator.validate(message)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "eventType inválido: INVALID_EVENT_TYPE"
                );
    }

    @Test
    void shouldRejectCompletedResultWithoutItems() {
        DocumentProcessingResultMessage message =
                new DocumentProcessingResultMessage(
                        1,
                        "DOCUMENT_PROCESSING_COMPLETED",
                        RESPONSE_EVENT_ID,
                        CORRELATION_ID,
                        OCCURRED_AT,
                        DOCUMENT_ID,
                        PATIENT_ID,
                        "Hemograma processado.",
                        "EXAME_HEMOGRAMA",
                        null,
                        DOCUMENT_DATE,
                        null,
                        List.of(),
                        null
                );

        assertThatThrownBy(
                () -> validator.validate(message)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Uma resposta concluída precisa conter "
                                + "ao menos um resultado."
                );
    }

    @Test
    void shouldRejectCompletedResultWithError() {
        DocumentProcessingResultMessage message =
                new DocumentProcessingResultMessage(
                        1,
                        "DOCUMENT_PROCESSING_COMPLETED",
                        RESPONSE_EVENT_ID,
                        CORRELATION_ID,
                        OCCURRED_AT,
                        DOCUMENT_ID,
                        PATIENT_ID,
                        "Hemograma processado.",
                        "EXAME_HEMOGRAMA",
                        null,
                        DOCUMENT_DATE,
                        null,
                        List.of(validItem()),
                        validError()
                );

        assertThatThrownBy(
                () -> validator.validate(message)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Uma resposta concluída não pode conter error."
                );
    }

    @Test
    void shouldRejectFailureWithSuccessFields() {
        DocumentProcessingResultMessage message =
                new DocumentProcessingResultMessage(
                        1,
                        "DOCUMENT_PROCESSING_FAILED",
                        RESPONSE_EVENT_ID,
                        CORRELATION_ID,
                        OCCURRED_AT,
                        DOCUMENT_ID,
                        PATIENT_ID,
                        "Resumo indevido.",
                        null,
                        null,
                        null,
                        null,
                        null,
                        validError()
                );

        assertThatThrownBy(
                () -> validator.validate(message)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Uma resposta de falha não pode conter "
                                + "campos de sucesso."
                );
    }

    @Test
    void shouldRejectFailureWithoutError() {
        DocumentProcessingResultMessage message =
                new DocumentProcessingResultMessage(
                        1,
                        "DOCUMENT_PROCESSING_FAILED",
                        RESPONSE_EVENT_ID,
                        CORRELATION_ID,
                        OCCURRED_AT,
                        DOCUMENT_ID,
                        PATIENT_ID,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        assertThatThrownBy(
                () -> validator.validate(message)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Uma resposta de falha precisa conter error."
                );
    }

    @Test
    void shouldRejectConfidenceAboveOne() {
        DocumentProcessingResultMessage message =
                new DocumentProcessingResultMessage(
                        1,
                        "DOCUMENT_PROCESSING_COMPLETED",
                        RESPONSE_EVENT_ID,
                        CORRELATION_ID,
                        OCCURRED_AT,
                        DOCUMENT_ID,
                        PATIENT_ID,
                        "Hemograma processado.",
                        "EXAME_HEMOGRAMA",
                        null,
                        DOCUMENT_DATE,
                        new BigDecimal("1.01"),
                        List.of(validItem()),
                        null
                );

        assertThatThrownBy(
                () -> validator.validate(message)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "O confidence deve estar entre 0 e 1."
                );
    }

    @Test
    void shouldRejectResultIdAboveLimit() {
        DocumentProcessingResultItemMessage invalidItem =
                new DocumentProcessingResultItemMessage(
                        "x".repeat(65),
                        "EXAME_HEMOGRAMA",
                        DOCUMENT_DATE,
                        Map.of()
                );

        DocumentProcessingResultMessage message =
                new DocumentProcessingResultMessage(
                        1,
                        "DOCUMENT_PROCESSING_COMPLETED",
                        RESPONSE_EVENT_ID,
                        CORRELATION_ID,
                        OCCURRED_AT,
                        DOCUMENT_ID,
                        PATIENT_ID,
                        "Hemograma processado.",
                        "EXAME_HEMOGRAMA",
                        null,
                        DOCUMENT_DATE,
                        null,
                        List.of(invalidItem),
                        null
                );

        assertThatThrownBy(
                () -> validator.validate(message)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "O resultId da posição 0 "
                                + "excede 64 caracteres."
                );
    }

    @Test
    void shouldRejectErrorMessageAboveLimit() {
        DocumentProcessingResultMessage message =
                new DocumentProcessingResultMessage(
                        1,
                        "DOCUMENT_PROCESSING_FAILED",
                        RESPONSE_EVENT_ID,
                        CORRELATION_ID,
                        OCCURRED_AT,
                        DOCUMENT_ID,
                        PATIENT_ID,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        new DocumentProcessingErrorMessage(
                                "AI_PROCESSING_FAILED",
                                "x".repeat(2001),
                                false
                        )
                );

        assertThatThrownBy(
                () -> validator.validate(message)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "O error.message excede 2000 caracteres."
                );
    }

    private DocumentProcessingResultMessage completedMessage() {
        return new DocumentProcessingResultMessage(
                1,
                "DOCUMENT_PROCESSING_COMPLETED",
                RESPONSE_EVENT_ID,
                CORRELATION_ID,
                OCCURRED_AT,
                DOCUMENT_ID,
                PATIENT_ID,
                "Hemograma processado.",
                "EXAME_HEMOGRAMA",
                "EXAMES_LABORATORIAIS",
                DOCUMENT_DATE,
                new BigDecimal("0.92"),
                List.of(validItem()),
                null
        );
    }

    private DocumentProcessingResultMessage failedMessage() {
        return new DocumentProcessingResultMessage(
                1,
                "DOCUMENT_PROCESSING_FAILED",
                RESPONSE_EVENT_ID,
                CORRELATION_ID,
                OCCURRED_AT,
                DOCUMENT_ID,
                PATIENT_ID,
                null,
                null,
                null,
                null,
                null,
                null,
                validError()
        );
    }

    private DocumentProcessingResultItemMessage validItem() {
        return new DocumentProcessingResultItemMessage(
                "resultado-001",
                "EXAME_HEMOGRAMA",
                DOCUMENT_DATE,
                Map.of(
                        "exam",
                        "hemograma"
                )
        );
    }

    private DocumentProcessingErrorMessage validError() {
        return new DocumentProcessingErrorMessage(
                "AI_PROCESSING_FAILED",
                "Não foi possível processar o documento.",
                false
        );
    }
}

package br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka.validation;

import br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka.DocumentProcessedResponseMessage;
import br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka.DocumentProcessingErrorMessage;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@Component
public class DocumentProcessedResponseValidator {

    public static final int SUPPORTED_SCHEMA_VERSION = 1;

    public static final String EXPECTED_EVENT_TYPE =
            "DOCUMENT_PROCESSED_RESPONSE";

    private static final String STATUS_PROCESSED = "PROCESSED";
    private static final String STATUS_FAILED = "FAILED";

    private static final int MAX_DOCUMENT_RESULT_ID_LENGTH = 64;

    private static final Pattern ERROR_CODE_PATTERN =
            Pattern.compile("^[A-Z][A-Z0-9_]*$");

    public void validate(DocumentProcessedResponseMessage message) {
        if (message == null) {
            throw new IllegalArgumentException(
                    "A mensagem de resposta é obrigatória."
            );
        }

        requireNonNull(message.eventId(), "O eventId é obrigatório.");
        requireNonNull(message.documentId(), "O documentId é obrigatório.");
        requireNonNull(message.patientId(), "O patientId é obrigatório.");

        if (isLegacy(message)) {
            validateLegacyPayload(message);
            return;
        }

        validateVersionOne(message);
    }

    public boolean isLegacy(DocumentProcessedResponseMessage message) {
        return message != null
                && message.schemaVersion() == null
                && isBlank(message.eventType())
                && message.occurredAt() == null
                && message.error() == null;
    }

    private void validateLegacyPayload(
            DocumentProcessedResponseMessage message
    ) {
        String status = normalizeStatus(message.status());

        if (status == null) {
            if (message.document() == null) {
                throw new IllegalArgumentException(
                        "A resposta legada precisa conter status ou document."
                );
            }

            return;
        }

        validateStatus(status);

        if (STATUS_PROCESSED.equals(status)
                && message.document() == null) {
            throw new IllegalArgumentException(
                    "Uma resposta PROCESSED precisa conter document."
            );
        }

        if (STATUS_FAILED.equals(status)
                && message.document() != null) {
            throw new IllegalArgumentException(
                    "Uma resposta FAILED não pode conter document."
            );
        }
    }

    private void validateVersionOne(
            DocumentProcessedResponseMessage message
    ) {
        if (!Objects.equals(
                message.schemaVersion(),
                SUPPORTED_SCHEMA_VERSION
        )) {
            throw new IllegalArgumentException(
                    "schemaVersion não suportada: "
                            + message.schemaVersion()
            );
        }

        if (!EXPECTED_EVENT_TYPE.equals(message.eventType())) {
            throw new IllegalArgumentException(
                    "eventType inválido: " + message.eventType()
            );
        }

        requireNonNull(
                message.occurredAt(),
                "O occurredAt é obrigatório no contrato v1."
        );

        String status = message.status();

        if (isBlank(status)) {
            throw new IllegalArgumentException(
                    "O status é obrigatório no contrato v1."
            );
        }

        validateStatus(status);
        validateLegacyErrorDetail(message.errorDetail());

        if (STATUS_PROCESSED.equals(status)) {
            validateProcessedResponse(message);
            return;
        }

        validateFailedResponse(message);
    }

    private void validateProcessedResponse(
            DocumentProcessedResponseMessage message
    ) {
        if (message.document() == null) {
            throw new IllegalArgumentException(
                    "Uma resposta PROCESSED precisa conter document."
            );
        }

        Object externalResultIdValue =
                message.document().get("id");

        if (externalResultIdValue == null) {
            throw new IllegalArgumentException(
                    "Uma resposta PROCESSED precisa conter document.id."
            );
        }

        if (!(externalResultIdValue instanceof String externalResultId)) {
            throw new IllegalArgumentException(
                    "O document.id precisa ser uma string."
            );
        }

        if (externalResultId.isBlank()) {
            throw new IllegalArgumentException(
                    "Uma resposta PROCESSED precisa conter document.id."
            );
        }

        if (externalResultId.length()
                > MAX_DOCUMENT_RESULT_ID_LENGTH) {
            throw new IllegalArgumentException(
                    "O document.id excede 64 caracteres."
            );
        }

        if (message.error() != null) {
            throw new IllegalArgumentException(
                    "Uma resposta PROCESSED não pode conter error."
            );
        }
    }

    private void validateFailedResponse(
            DocumentProcessedResponseMessage message
    ) {
        if (message.document() != null) {
            throw new IllegalArgumentException(
                    "Uma resposta FAILED não pode conter document."
            );
        }

        if (message.error() == null) {
            throw new IllegalArgumentException(
                    "Uma resposta FAILED precisa conter error."
            );
        }

        validateStructuredError(message.error());
    }

    private void validateStructuredError(
            DocumentProcessingErrorMessage error
    ) {
        if (isBlank(error.code())) {
            throw new IllegalArgumentException(
                    "O error.code é obrigatório."
            );
        }

        if (error.code().length() > 100
                || !ERROR_CODE_PATTERN.matcher(error.code()).matches()) {
            throw new IllegalArgumentException(
                    "O error.code possui formato inválido."
            );
        }

        if (isBlank(error.message())) {
            throw new IllegalArgumentException(
                    "O error.message é obrigatório."
            );
        }

        if (error.message().length() > 1000) {
            throw new IllegalArgumentException(
                    "O error.message excede 1000 caracteres."
            );
        }

        if (error.retryable() == null) {
            throw new IllegalArgumentException(
                    "O error.retryable é obrigatório."
            );
        }
    }

    private void validateLegacyErrorDetail(String errorDetail) {
        if (errorDetail != null && errorDetail.length() > 2000) {
            throw new IllegalArgumentException(
                    "O errorDetail excede 2000 caracteres."
            );
        }
    }

    private void validateStatus(String status) {
        if (!STATUS_PROCESSED.equals(status)
                && !STATUS_FAILED.equals(status)) {
            throw new IllegalArgumentException(
                    "Status de resposta inválido: " + status
            );
        }
    }

    private String normalizeStatus(String status) {
        if (isBlank(status)) {
            return null;
        }

        return status.trim().toUpperCase(Locale.ROOT);
    }

    private void requireNonNull(Object value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

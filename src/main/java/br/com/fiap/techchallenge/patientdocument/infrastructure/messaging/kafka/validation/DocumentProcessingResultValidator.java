package br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka.validation;

import br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka.DocumentProcessingErrorMessage;
import br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka.DocumentProcessingResultItemMessage;
import br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka.DocumentProcessingResultMessage;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
public class DocumentProcessingResultValidator {

    public static final int SUPPORTED_SCHEMA_VERSION = 1;

    public static final String COMPLETED_EVENT_TYPE =
            "DOCUMENT_PROCESSING_COMPLETED";

    public static final String FAILED_EVENT_TYPE =
            "DOCUMENT_PROCESSING_FAILED";

    private static final int MAX_SUMMARY_LENGTH = 4000;
    private static final int MAX_TYPE_LENGTH = 100;
    private static final int MAX_SPECIALTY_LENGTH = 100;
    private static final int MAX_RESULT_ID_LENGTH = 64;
    private static final int MAX_ERROR_CODE_LENGTH = 100;
    private static final int MAX_ERROR_MESSAGE_LENGTH = 2000;

    private static final BigDecimal MIN_CONFIDENCE =
            BigDecimal.ZERO;

    private static final BigDecimal MAX_CONFIDENCE =
            BigDecimal.ONE;

    public void validate(DocumentProcessingResultMessage message) {
        if (message == null) {
            throw new IllegalArgumentException(
                    "A mensagem de resultado é obrigatória."
            );
        }

        validateCommonFields(message);

        switch (message.eventType()) {
            case COMPLETED_EVENT_TYPE ->
                    validateCompleted(message);

            case FAILED_EVENT_TYPE ->
                    validateFailed(message);

            default ->
                    throw new IllegalArgumentException(
                            "eventType inválido: "
                                    + message.eventType()
                    );
        }
    }

    private void validateCommonFields(
            DocumentProcessingResultMessage message
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

        requireNonBlank(
                message.eventType(),
                "O eventType é obrigatório."
        );

        requireNonNull(
                message.eventId(),
                "O eventId é obrigatório."
        );

        requireNonNull(
                message.correlationId(),
                "O correlationId é obrigatório."
        );

        if (message.eventId().equals(
                message.correlationId()
        )) {
            throw new IllegalArgumentException(
                    "O eventId deve ser diferente "
                            + "do correlationId."
            );
        }

        requireNonNull(
                message.occurredAt(),
                "O occurredAt é obrigatório."
        );

        requireNonNull(
                message.documentId(),
                "O documentId é obrigatório."
        );

        requireNonNull(
                message.patientId(),
                "O patientId é obrigatório."
        );
    }

    private void validateCompleted(
            DocumentProcessingResultMessage message
    ) {
        requireNonBlank(
                message.summary(),
                "O summary é obrigatório."
        );

        validateMaximumLength(
                message.summary(),
                MAX_SUMMARY_LENGTH,
                "O summary excede 4000 caracteres."
        );

        requireNonBlank(
                message.primaryDocumentType(),
                "O primaryDocumentType é obrigatório."
        );

        validateMaximumLength(
                message.primaryDocumentType(),
                MAX_TYPE_LENGTH,
                "O primaryDocumentType excede 100 caracteres."
        );

        validateOptionalText(
                message.specialty(),
                MAX_SPECIALTY_LENGTH,
                "O specialty não pode ser vazio.",
                "O specialty excede 100 caracteres."
        );

        validateConfidence(message.confidence());
        validateResults(message.results());

        if (message.error() != null) {
            throw new IllegalArgumentException(
                    "Uma resposta concluída não pode conter error."
            );
        }
    }

    private void validateResults(
            List<DocumentProcessingResultItemMessage> results
    ) {
        if (results == null || results.isEmpty()) {
            throw new IllegalArgumentException(
                    "Uma resposta concluída precisa conter "
                            + "ao menos um resultado."
            );
        }

        Set<String> resultIds = new HashSet<>();

        for (int index = 0; index < results.size(); index++) {
            DocumentProcessingResultItemMessage item =
                    results.get(index);

            if (item == null) {
                throw new IllegalArgumentException(
                        "O resultado da posição "
                                + index
                                + " é obrigatório."
                );
            }

            validateResultItem(item, index);

            if (!resultIds.add(item.resultId())) {
                throw new IllegalArgumentException(
                        "O resultId "
                                + item.resultId()
                                + " está duplicado."
                );
            }
        }
    }

    private void validateResultItem(
            DocumentProcessingResultItemMessage item,
            int index
    ) {
        requireNonBlank(
                item.resultId(),
                "O resultId da posição "
                        + index
                        + " é obrigatório."
        );

        validateMaximumLength(
                item.resultId(),
                MAX_RESULT_ID_LENGTH,
                "O resultId da posição "
                        + index
                        + " excede 64 caracteres."
        );

        requireNonBlank(
                item.documentType(),
                "O documentType da posição "
                        + index
                        + " é obrigatório."
        );

        validateMaximumLength(
                item.documentType(),
                MAX_TYPE_LENGTH,
                "O documentType da posição "
                        + index
                        + " excede 100 caracteres."
        );

        if (item.data() == null) {
            throw new IllegalArgumentException(
                    "O data da posição "
                            + index
                            + " é obrigatório."
            );
        }
    }

    private void validateConfidence(BigDecimal confidence) {
        if (confidence == null) {
            return;
        }

        if (confidence.compareTo(MIN_CONFIDENCE) < 0
                || confidence.compareTo(MAX_CONFIDENCE) > 0) {
            throw new IllegalArgumentException(
                    "O confidence deve estar entre 0 e 1."
            );
        }
    }

    private void validateFailed(
            DocumentProcessingResultMessage message
    ) {
        if (message.summary() != null
                || message.primaryDocumentType() != null
                || message.specialty() != null
                || message.documentDate() != null
                || message.confidence() != null
                || message.results() != null) {
            throw new IllegalArgumentException(
                    "Uma resposta de falha não pode conter "
                            + "campos de sucesso."
            );
        }

        validateError(message.error());
    }

    private void validateError(
            DocumentProcessingErrorMessage error
    ) {
        if (error == null) {
            throw new IllegalArgumentException(
                    "Uma resposta de falha precisa conter error."
            );
        }

        requireNonBlank(
                error.code(),
                "O error.code é obrigatório."
        );

        validateMaximumLength(
                error.code(),
                MAX_ERROR_CODE_LENGTH,
                "O error.code excede 100 caracteres."
        );

        requireNonBlank(
                error.message(),
                "O error.message é obrigatório."
        );

        validateMaximumLength(
                error.message(),
                MAX_ERROR_MESSAGE_LENGTH,
                "O error.message excede 2000 caracteres."
        );

        if (error.retryable() == null) {
            throw new IllegalArgumentException(
                    "O error.retryable é obrigatório."
            );
        }
    }

    private void validateOptionalText(
            String value,
            int maximumLength,
            String blankMessage,
            String maximumLengthMessage
    ) {
        if (value == null) {
            return;
        }

        if (value.isBlank()) {
            throw new IllegalArgumentException(blankMessage);
        }

        validateMaximumLength(
                value,
                maximumLength,
                maximumLengthMessage
        );
    }

    private void validateMaximumLength(
            String value,
            int maximumLength,
            String message
    ) {
        if (value != null && value.length() > maximumLength) {
            throw new IllegalArgumentException(message);
        }
    }

    private void requireNonBlank(
            String value,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private void requireNonNull(
            Object value,
            String message
    ) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }
}

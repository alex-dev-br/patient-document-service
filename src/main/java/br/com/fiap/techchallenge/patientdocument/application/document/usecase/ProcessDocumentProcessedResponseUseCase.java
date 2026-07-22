package br.com.fiap.techchallenge.patientdocument.application.document.usecase;

import br.com.fiap.techchallenge.patientdocument.application.document.command.ProcessDocumentProcessedResponseCommand;
import br.com.fiap.techchallenge.patientdocument.application.document.gateway.DocumentProcessingEventGateway;
import br.com.fiap.techchallenge.patientdocument.application.document.gateway.HealthDocumentGateway;
import br.com.fiap.techchallenge.patientdocument.application.document.gateway.ProcessedDocumentResultGateway;
import br.com.fiap.techchallenge.patientdocument.application.document.result.ProcessedDocumentResult;
import br.com.fiap.techchallenge.patientdocument.application.exception.ResourceNotFoundException;
import br.com.fiap.techchallenge.patientdocument.domain.document.DocumentProcessingStatus;
import br.com.fiap.techchallenge.patientdocument.domain.document.DocumentType;
import br.com.fiap.techchallenge.patientdocument.domain.document.HealthDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessDocumentProcessedResponseUseCase {

    private static final String FAILED_RESULT_ID = "FAILED";

    private final DocumentProcessingEventGateway documentProcessingEventGateway;
    private final ProcessedDocumentResultGateway processedDocumentResultGateway;
    private final HealthDocumentGateway healthDocumentGateway;

    @Transactional
    public void execute(ProcessDocumentProcessedResponseCommand command) {
        validateRequiredFields(command);
        validateOriginalRequest(command);

        HealthDocument healthDocument = healthDocumentGateway
                .findById(command.documentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Documento não encontrado: " + command.documentId()
                ));

        validatePatient(command, healthDocument);

        DocumentProcessingStatus status = resolveStatus(command);
        String externalResultId = resolveExternalResultId(command, status);

        if (processedDocumentResultGateway
                .existsByEventIdAndExternalResultId(
                        command.eventId(),
                        externalResultId
                )) {
            return;
        }

        Map<String, Object> payload = command.document();

        ProcessedDocumentResult result = new ProcessedDocumentResult(
                UUID.randomUUID(),
                command.schemaVersion(),
                command.occurredAt(),
                command.eventId(),
                command.documentId(),
                command.patientId(),
                externalResultId,
                extractText(payload, "documentType"),
                status,
                payload,
                truncate(command.errorCode(), 100),
                truncate(command.errorDetail(), 2000),
                command.errorRetryable(),
                LocalDateTime.now()
        );

        processedDocumentResultGateway.save(result);

        if (status == DocumentProcessingStatus.FAILED) {
            markAsFailedWhenNecessary(healthDocument);
            return;
        }

        /*
         * O primeiro resultado marca o documento original como processado.
         * Outros resultados do mesmo evento permanecem armazenados no JSONB,
         * sem sobrescrever o resumo principal.
         */
        if (healthDocument.getProcessingStatus()
                != DocumentProcessingStatus.PROCESSED) {
            applyFirstSuccessfulResult(healthDocument, payload);
        }
    }

    private void validateRequiredFields(
            ProcessDocumentProcessedResponseCommand command
    ) {
        Objects.requireNonNull(command, "A mensagem é obrigatória.");
        Objects.requireNonNull(command.eventId(), "O eventId é obrigatório.");
        Objects.requireNonNull(command.documentId(), "O documentId é obrigatório.");
        Objects.requireNonNull(command.patientId(), "O patientId é obrigatório.");
    }

    private void validateOriginalRequest(
            ProcessDocumentProcessedResponseCommand command
    ) {
        boolean validRequest =
                documentProcessingEventGateway
                        .existsByEventIdAndDocumentIdAndPatientId(
                                command.eventId(),
                                command.documentId(),
                                command.patientId()
                        );

        if (!validRequest) {
            throw new IllegalArgumentException(
                    "A resposta não corresponde a uma solicitação de processamento."
            );
        }
    }

    private void validatePatient(
            ProcessDocumentProcessedResponseCommand command,
            HealthDocument healthDocument
    ) {
        if (!healthDocument.getPatientId().equals(command.patientId())) {
            throw new IllegalArgumentException(
                    "O patientId da resposta não corresponde ao documento."
            );
        }

        if (command.document() == null) {
            return;
        }

        String payloadPatientId = extractText(
                command.document(),
                "patientId"
        );

        if (payloadPatientId != null
                && !command.patientId().toString().equals(payloadPatientId)) {
            throw new IllegalArgumentException(
                    "O patientId interno do resultado é divergente."
            );
        }
    }

    private DocumentProcessingStatus resolveStatus(
            ProcessDocumentProcessedResponseCommand command
    ) {
        /*
         * O contrato atual de sucesso ainda não possui status.
         * A presença de document significa PROCESSED.
         */
        if (command.status() == null || command.status().isBlank()) {
            if (command.document() != null) {
                return DocumentProcessingStatus.PROCESSED;
            }

            throw new IllegalArgumentException(
                    "A mensagem precisa conter status ou document."
            );
        }

        try {
            DocumentProcessingStatus status =
                    DocumentProcessingStatus.valueOf(
                            command.status().trim().toUpperCase()
                    );

            if (status != DocumentProcessingStatus.PROCESSED
                    && status != DocumentProcessingStatus.FAILED) {
                throw new IllegalArgumentException(
                        "Status de resposta inválido: " + command.status()
                );
            }

            return status;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Status de resposta inválido: " + command.status(),
                    exception
            );
        }
    }

    private String resolveExternalResultId(
            ProcessDocumentProcessedResponseCommand command,
            DocumentProcessingStatus status
    ) {
        if (status == DocumentProcessingStatus.FAILED) {
            return FAILED_RESULT_ID;
        }

        String externalResultId = extractText(
                command.document(),
                "id"
        );

        if (externalResultId == null || externalResultId.isBlank()) {
            throw new IllegalArgumentException(
                    "O document.id do resultado é obrigatório."
            );
        }

        return externalResultId;
    }

    private void applyFirstSuccessfulResult(
            HealthDocument healthDocument,
            Map<String, Object> payload
    ) {
        String externalDocumentType = extractText(
                payload,
                "documentType"
        );

        DocumentType documentType =
                mapDocumentType(externalDocumentType);

        LocalDate documentDate = parseDocumentDate(
                extractText(payload, "documentDate")
        );

        String summary = firstNonBlank(
                extractText(payload, "descricaoGeral"),
                extractText(payload, "resumoCaso"),
                externalDocumentType == null
                        ? "Documento processado pela inteligência artificial."
                        : "Documento processado como " + externalDocumentType + "."
        );

        List<String> keywords = externalDocumentType == null
                ? List.of()
                : List.of(externalDocumentType);

        HealthDocument updated = healthDocument.applyAiResult(
                documentType,
                null,
                documentDate,
                summary,
                keywords,
                null,
                DocumentProcessingStatus.PROCESSED
        );

        healthDocumentGateway.save(updated);
    }

    private void markAsFailedWhenNecessary(
            HealthDocument healthDocument
    ) {
        if (healthDocument.getProcessingStatus()
                == DocumentProcessingStatus.PROCESSED) {
            return;
        }

        HealthDocument failed = healthDocument.applyAiResult(
                healthDocument.getDocumentType(),
                healthDocument.getSpecialty(),
                healthDocument.getDocumentDate(),
                healthDocument.getSummary(),
                healthDocument.getKeywords(),
                healthDocument.getConfidence(),
                DocumentProcessingStatus.FAILED
        );

        healthDocumentGateway.save(failed);
    }

    private DocumentType mapDocumentType(String externalType) {
        if (externalType == null || externalType.isBlank()) {
            return DocumentType.OUTRO;
        }

        String normalized = externalType.trim().toUpperCase();

        if (normalized.startsWith("EXAME_")) {
            return DocumentType.EXAME_LABORATORIAL;
        }

        if (normalized.startsWith("LAUDO_")) {
            return DocumentType.LAUDO_MEDICO;
        }

        return switch (normalized) {
            case "RECEITA" -> DocumentType.RECEITA_MEDICA;
            case "ENCAMINHAMENTO" -> DocumentType.ENCAMINHAMENTO;
            case "RELATORIO" -> DocumentType.RELATORIO_MEDICO;
            default -> DocumentType.OUTRO;
        };
    }

    private LocalDate parseDocumentDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(value).toLocalDate();
        } catch (DateTimeParseException ignored) {
            // Tenta os demais formatos.
        }

        try {
            return OffsetDateTime.parse(value).toLocalDate();
        } catch (DateTimeParseException ignored) {
            // Tenta somente a data.
        }

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String extractText(
            Map<String, Object> payload,
            String field
    ) {
        if (payload == null) {
            return null;
        }

        Object value = payload.get(field);

        if (value == null) {
            return null;
        }

        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return null;
    }

    private String truncate(String value, int maximumLength) {
        if (value == null) {
            return null;
        }

        return value.substring(
                0,
                Math.min(value.length(), maximumLength)
        );
    }
}

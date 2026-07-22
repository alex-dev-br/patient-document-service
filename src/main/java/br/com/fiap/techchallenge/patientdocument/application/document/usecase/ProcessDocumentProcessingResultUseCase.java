package br.com.fiap.techchallenge.patientdocument.application.document.usecase;

import br.com.fiap.techchallenge.patientdocument.application.document.command.ProcessDocumentProcessingResultCommand;
import br.com.fiap.techchallenge.patientdocument.application.document.command.ProcessDocumentProcessingResultItemCommand;
import br.com.fiap.techchallenge.patientdocument.application.document.gateway.DocumentProcessingEventGateway;
import br.com.fiap.techchallenge.patientdocument.application.document.gateway.HealthDocumentGateway;
import br.com.fiap.techchallenge.patientdocument.application.document.gateway.ProcessedDocumentResultGateway;
import br.com.fiap.techchallenge.patientdocument.application.document.result.ProcessedDocumentResult;
import br.com.fiap.techchallenge.patientdocument.application.exception.ResourceNotFoundException;
import br.com.fiap.techchallenge.patientdocument.domain.document.DocumentProcessingStatus;
import br.com.fiap.techchallenge.patientdocument.domain.document.DocumentType;
import br.com.fiap.techchallenge.patientdocument.domain.document.HealthDocument;
import br.com.fiap.techchallenge.patientdocument.domain.document.MedicalSpecialty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessDocumentProcessingResultUseCase {

    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    private static final String COMPLETED_EVENT_TYPE =
            "DOCUMENT_PROCESSING_COMPLETED";

    private static final String FAILED_EVENT_TYPE =
            "DOCUMENT_PROCESSING_FAILED";

    private static final String FAILED_RESULT_ID =
            "FAILED";

    private final DocumentProcessingEventGateway
            documentProcessingEventGateway;

    private final ProcessedDocumentResultGateway
            processedDocumentResultGateway;

    private final HealthDocumentGateway
            healthDocumentGateway;

    @Transactional
    public void execute(
            ProcessDocumentProcessingResultCommand command
    ) {
        validateCommand(command);
        validateOriginalRequest(command);

        HealthDocument healthDocument =
                healthDocumentGateway
                        .findById(command.documentId())
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Documento não encontrado: "
                                                + command.documentId()
                                )
                        );

        validatePatient(command, healthDocument);

        switch (command.eventType()) {
            case COMPLETED_EVENT_TYPE ->
                    processCompleted(
                            command,
                            healthDocument
                    );

            case FAILED_EVENT_TYPE ->
                    processFailed(
                            command,
                            healthDocument
                    );

            default ->
                    throw new IllegalArgumentException(
                            "eventType inválido: "
                                    + command.eventType()
                    );
        }
    }

    private void processCompleted(
            ProcessDocumentProcessingResultCommand command,
            HealthDocument healthDocument
    ) {
        LocalDateTime receivedAt =
                LocalDateTime.now();

        for (
                ProcessDocumentProcessingResultItemCommand item
                : command.results()
        ) {
            if (processedDocumentResultGateway
                    .existsByEventIdAndExternalResultId(
                            command.eventId(),
                            item.resultId()
                    )) {
                continue;
            }

            ProcessedDocumentResult result =
                    new ProcessedDocumentResult(
                            UUID.randomUUID(),
                            command.schemaVersion(),
                            command.occurredAt(),
                            command.eventId(),
                            command.correlationId(),
                            command.documentId(),
                            command.patientId(),
                            item.resultId(),
                            item.documentType(),
                            item.documentDate(),
                            DocumentProcessingStatus.PROCESSED,
                            item.data(),
                            null,
                            null,
                            null,
                            receivedAt
                    );

            processedDocumentResultGateway.save(result);
        }

        if (healthDocument.getProcessingStatus()
                == DocumentProcessingStatus.PROCESSED) {
            return;
        }

        HealthDocument updated =
                healthDocument.applyAiResult(
                        mapDocumentType(
                                command.primaryDocumentType()
                        ),
                        mapSpecialty(command.specialty()),
                        resolveDocumentDate(command),
                        command.summary(),
                        buildKeywords(command.results()),
                        command.confidence(),
                        DocumentProcessingStatus.PROCESSED
                );

        healthDocumentGateway.save(updated);
    }

    private void processFailed(
            ProcessDocumentProcessingResultCommand command,
            HealthDocument healthDocument
    ) {
        if (processedDocumentResultGateway
                .existsByEventIdAndExternalResultId(
                        command.eventId(),
                        FAILED_RESULT_ID
                )) {
            return;
        }

        ProcessedDocumentResult result =
                new ProcessedDocumentResult(
                        UUID.randomUUID(),
                        command.schemaVersion(),
                        command.occurredAt(),
                        command.eventId(),
                        command.correlationId(),
                        command.documentId(),
                        command.patientId(),
                        FAILED_RESULT_ID,
                        null,
                        null,
                        DocumentProcessingStatus.FAILED,
                        null,
                        truncate(command.errorCode(), 100),
                        truncate(command.errorDetail(), 2000),
                        command.errorRetryable(),
                        LocalDateTime.now()
                );

        processedDocumentResultGateway.save(result);

        if (healthDocument.getProcessingStatus()
                == DocumentProcessingStatus.PROCESSED) {
            return;
        }

        HealthDocument failed =
                healthDocument.applyAiResult(
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

    private void validateCommand(
            ProcessDocumentProcessingResultCommand command
    ) {
        Objects.requireNonNull(
                command,
                "A mensagem é obrigatória."
        );

        if (!Objects.equals(
                command.schemaVersion(),
                SUPPORTED_SCHEMA_VERSION
        )) {
            throw new IllegalArgumentException(
                    "schemaVersion não suportada: "
                            + command.schemaVersion()
            );
        }

        requireNonBlank(
                command.eventType(),
                "O eventType é obrigatório."
        );

        Objects.requireNonNull(
                command.eventId(),
                "O eventId é obrigatório."
        );

        Objects.requireNonNull(
                command.correlationId(),
                "O correlationId é obrigatório."
        );

        if (command.eventId().equals(
                command.correlationId()
        )) {
            throw new IllegalArgumentException(
                    "O eventId deve ser diferente "
                            + "do correlationId."
            );
        }

        Objects.requireNonNull(
                command.occurredAt(),
                "O occurredAt é obrigatório."
        );

        Objects.requireNonNull(
                command.documentId(),
                "O documentId é obrigatório."
        );

        Objects.requireNonNull(
                command.patientId(),
                "O patientId é obrigatório."
        );

        switch (command.eventType()) {
            case COMPLETED_EVENT_TYPE ->
                    validateCompleted(command);

            case FAILED_EVENT_TYPE ->
                    validateFailed(command);

            default ->
                    throw new IllegalArgumentException(
                            "eventType inválido: "
                                    + command.eventType()
                    );
        }
    }

    private void validateCompleted(
            ProcessDocumentProcessingResultCommand command
    ) {
        requireNonBlank(
                command.summary(),
                "O summary é obrigatório."
        );

        requireNonBlank(
                command.primaryDocumentType(),
                "O primaryDocumentType é obrigatório."
        );

        if (command.results() == null
                || command.results().isEmpty()) {
            throw new IllegalArgumentException(
                    "Uma resposta concluída precisa conter "
                            + "ao menos um resultado."
            );
        }

        if (command.errorCode() != null
                || command.errorDetail() != null
                || command.errorRetryable() != null) {
            throw new IllegalArgumentException(
                    "Uma resposta concluída não pode conter erro."
            );
        }

        Set<String> resultIds =
                new HashSet<>();

        for (
                ProcessDocumentProcessingResultItemCommand item
                : command.results()
        ) {
            if (item == null) {
                throw new IllegalArgumentException(
                        "A lista de resultados não pode "
                                + "conter item nulo."
                );
            }

            requireNonBlank(
                    item.resultId(),
                    "O resultId é obrigatório."
            );

            requireNonBlank(
                    item.documentType(),
                    "O documentType é obrigatório."
            );

            Objects.requireNonNull(
                    item.data(),
                    "O data do resultado é obrigatório."
            );

            if (!resultIds.add(item.resultId())) {
                throw new IllegalArgumentException(
                        "O resultId "
                                + item.resultId()
                                + " está duplicado."
                );
            }
        }
    }

    private void validateFailed(
            ProcessDocumentProcessingResultCommand command
    ) {
        if (command.summary() != null
                || command.primaryDocumentType() != null
                || command.specialty() != null
                || command.documentDate() != null
                || command.confidence() != null
                || command.results() != null) {
            throw new IllegalArgumentException(
                    "Uma resposta de falha não pode conter "
                            + "campos de sucesso."
            );
        }

        requireNonBlank(
                command.errorCode(),
                "O errorCode é obrigatório."
        );

        requireNonBlank(
                command.errorDetail(),
                "O errorDetail é obrigatório."
        );

        Objects.requireNonNull(
                command.errorRetryable(),
                "O errorRetryable é obrigatório."
        );
    }

    private void validateOriginalRequest(
            ProcessDocumentProcessingResultCommand command
    ) {
        boolean validRequest =
                documentProcessingEventGateway
                        .existsByEventIdAndDocumentIdAndPatientId(
                                command.correlationId(),
                                command.documentId(),
                                command.patientId()
                        );

        if (!validRequest) {
            throw new IllegalArgumentException(
                    "A resposta não corresponde a uma "
                            + "solicitação de processamento."
            );
        }
    }

    private void validatePatient(
            ProcessDocumentProcessingResultCommand command,
            HealthDocument healthDocument
    ) {
        if (!healthDocument
                .getPatientId()
                .equals(command.patientId())) {
            throw new IllegalArgumentException(
                    "O patientId da resposta não corresponde "
                            + "ao documento."
            );
        }
    }

    private DocumentType mapDocumentType(
            String externalType
    ) {
        String normalized =
                externalType
                        .trim()
                        .toUpperCase(Locale.ROOT);

        if (normalized.startsWith("EXAME_")) {
            return DocumentType.EXAME_LABORATORIAL;
        }

        if (normalized.startsWith("LAUDO_")) {
            return DocumentType.LAUDO_MEDICO;
        }

        return switch (normalized) {
            case "RECEITA",
                 "RECEITA_MEDICA" ->
                    DocumentType.RECEITA_MEDICA;

            case "LAUDO",
                 "LAUDO_MEDICO" ->
                    DocumentType.LAUDO_MEDICO;

            case "ENCAMINHAMENTO" ->
                    DocumentType.ENCAMINHAMENTO;

            case "RELATORIO",
                 "RELATORIO_MEDICO" ->
                    DocumentType.RELATORIO_MEDICO;

            case "VACINACAO" ->
                    DocumentType.VACINACAO;

            case "INTERNACAO" ->
                    DocumentType.INTERNACAO;

            case "ORIENTACAO_POS_CONSULTA" ->
                    DocumentType.ORIENTACAO_POS_CONSULTA;

            default ->
                    DocumentType.OUTRO;
        };
    }

    private MedicalSpecialty mapSpecialty(
            String specialty
    ) {
        if (specialty == null || specialty.isBlank()) {
            return null;
        }

        try {
            return MedicalSpecialty.valueOf(
                    specialty
                            .trim()
                            .toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            return MedicalSpecialty.OUTRA;
        }
    }

    private LocalDate resolveDocumentDate(
            ProcessDocumentProcessingResultCommand command
    ) {
        if (command.documentDate() != null) {
            return command.documentDate();
        }

        return command.results()
                .stream()
                .map(
                        ProcessDocumentProcessingResultItemCommand
                                ::documentDate
                )
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(null);
    }

    private List<String> buildKeywords(
            List<ProcessDocumentProcessingResultItemCommand>
                    results
    ) {
        return results.stream()
                .map(
                        ProcessDocumentProcessingResultItemCommand
                                ::documentType
                )
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private void requireNonBlank(
            String value,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private String truncate(
            String value,
            int maximumLength
    ) {
        if (value == null) {
            return null;
        }

        return value.substring(
                0,
                Math.min(
                        value.length(),
                        maximumLength
                )
        );
    }
}

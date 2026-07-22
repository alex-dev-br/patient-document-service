package br.com.fiap.techchallenge.patientdocument.application.document.usecase;

import br.com.fiap.techchallenge.patientdocument.application.document.command.ProcessDocumentProcessingResultCommand;
import br.com.fiap.techchallenge.patientdocument.application.document.command.ProcessDocumentProcessingResultItemCommand;
import br.com.fiap.techchallenge.patientdocument.application.document.gateway.DocumentProcessingEventGateway;
import br.com.fiap.techchallenge.patientdocument.application.document.gateway.HealthDocumentGateway;
import br.com.fiap.techchallenge.patientdocument.application.document.gateway.ProcessedDocumentResultGateway;
import br.com.fiap.techchallenge.patientdocument.application.document.result.ProcessedDocumentResult;
import br.com.fiap.techchallenge.patientdocument.domain.document.DocumentProcessingStatus;
import br.com.fiap.techchallenge.patientdocument.domain.document.DocumentType;
import br.com.fiap.techchallenge.patientdocument.domain.document.HealthDocument;
import br.com.fiap.techchallenge.patientdocument.domain.document.MedicalSpecialty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessDocumentProcessingResultUseCaseTest {

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

    private static final UUID OTHER_PATIENT_ID =
            UUID.fromString(
                    "55555555-5555-5555-5555-555555555555"
            );

    private static final Instant OCCURRED_AT =
            Instant.parse("2026-07-22T18:00:00Z");

    @Mock
    private DocumentProcessingEventGateway
            documentProcessingEventGateway;

    @Mock
    private ProcessedDocumentResultGateway
            processedDocumentResultGateway;

    @Mock
    private HealthDocumentGateway
            healthDocumentGateway;

    private ProcessDocumentProcessingResultUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase =
                new ProcessDocumentProcessingResultUseCase(
                        documentProcessingEventGateway,
                        processedDocumentResultGateway,
                        healthDocumentGateway
                );
    }

    @Test
    void shouldStoreAllResultsAndUpdatePendingDocument() {
        HealthDocument pendingDocument =
                pendingDocument();

        mockValidRequest(
                PATIENT_ID,
                pendingDocument
        );

        when(
                processedDocumentResultGateway
                        .existsByEventIdAndExternalResultId(
                                RESPONSE_EVENT_ID,
                                "resultado-001"
                        )
        ).thenReturn(false);

        when(
                processedDocumentResultGateway
                        .existsByEventIdAndExternalResultId(
                                RESPONSE_EVENT_ID,
                                "resultado-002"
                        )
        ).thenReturn(false);

        useCase.execute(completedCommand());

        ArgumentCaptor<ProcessedDocumentResult>
                resultCaptor =
                ArgumentCaptor.forClass(
                        ProcessedDocumentResult.class
                );

        verify(
                processedDocumentResultGateway,
                times(2)
        ).save(resultCaptor.capture());

        List<ProcessedDocumentResult> savedResults =
                resultCaptor.getAllValues();

        assertThat(savedResults)
                .extracting(
                        ProcessedDocumentResult
                                ::externalResultId
                )
                .containsExactly(
                        "resultado-001",
                        "resultado-002"
                );

        ProcessedDocumentResult firstResult =
                savedResults.getFirst();

        assertThat(firstResult.eventId())
                .isEqualTo(RESPONSE_EVENT_ID);

        assertThat(firstResult.correlationId())
                .isEqualTo(CORRELATION_ID);

        assertThat(firstResult.documentId())
                .isEqualTo(DOCUMENT_ID);

        assertThat(firstResult.patientId())
                .isEqualTo(PATIENT_ID);

        assertThat(firstResult.documentDate())
                .isEqualTo(LocalDate.of(2026, 7, 10));

        assertThat(firstResult.externalDocumentType())
                .isEqualTo("EXAME_HEMOGRAMA");

        assertThat(firstResult.payload())
                .containsEntry(
                        "exameTipo",
                        "HEMOGRAMA"
                );

        assertThat(firstResult.status())
                .isEqualTo(
                        DocumentProcessingStatus.PROCESSED
                );

        ArgumentCaptor<HealthDocument>
                documentCaptor =
                ArgumentCaptor.forClass(
                        HealthDocument.class
                );

        verify(healthDocumentGateway)
                .save(documentCaptor.capture());

        HealthDocument updatedDocument =
                documentCaptor.getValue();

        assertThat(updatedDocument.getDocumentType())
                .isEqualTo(
                        DocumentType.EXAME_LABORATORIAL
                );

        assertThat(updatedDocument.getSpecialty())
                .isEqualTo(
                        MedicalSpecialty
                                .EXAMES_LABORATORIAIS
                );

        assertThat(updatedDocument.getDocumentDate())
                .isEqualTo(LocalDate.of(2026, 7, 10));

        assertThat(updatedDocument.getSummary())
                .isEqualTo(
                        "Documento processado contendo: "
                                + "exame hemograma, receita."
                );

        assertThat(updatedDocument.getConfidence())
                .isEqualByComparingTo("0.92");

        assertThat(updatedDocument.getKeywords())
                .containsExactly(
                        "EXAME_HEMOGRAMA",
                        "RECEITA"
                );

        assertThat(updatedDocument.getProcessingStatus())
                .isEqualTo(
                        DocumentProcessingStatus.PROCESSED
                );

        verify(documentProcessingEventGateway)
                .existsByEventIdAndDocumentIdAndPatientId(
                        CORRELATION_ID,
                        DOCUMENT_ID,
                        PATIENT_ID
                );
    }

    @Test
    void shouldPersistOnlyNonDuplicatedItems() {
        HealthDocument pendingDocument =
                pendingDocument();

        mockValidRequest(
                PATIENT_ID,
                pendingDocument
        );

        when(
                processedDocumentResultGateway
                        .existsByEventIdAndExternalResultId(
                                RESPONSE_EVENT_ID,
                                "resultado-001"
                        )
        ).thenReturn(true);

        when(
                processedDocumentResultGateway
                        .existsByEventIdAndExternalResultId(
                                RESPONSE_EVENT_ID,
                                "resultado-002"
                        )
        ).thenReturn(false);

        useCase.execute(completedCommand());

        ArgumentCaptor<ProcessedDocumentResult>
                resultCaptor =
                ArgumentCaptor.forClass(
                        ProcessedDocumentResult.class
                );

        verify(processedDocumentResultGateway)
                .save(resultCaptor.capture());

        assertThat(
                resultCaptor
                        .getValue()
                        .externalResultId()
        ).isEqualTo("resultado-002");

        verify(healthDocumentGateway)
                .save(any(HealthDocument.class));
    }

    @Test
    void shouldIgnoreRepeatedCompletedEnvelope() {
        HealthDocument processedDocument =
                processedDocument();

        mockValidRequest(
                PATIENT_ID,
                processedDocument
        );

        when(
                processedDocumentResultGateway
                        .existsByEventIdAndExternalResultId(
                                RESPONSE_EVENT_ID,
                                "resultado-001"
                        )
        ).thenReturn(true);

        when(
                processedDocumentResultGateway
                        .existsByEventIdAndExternalResultId(
                                RESPONSE_EVENT_ID,
                                "resultado-002"
                        )
        ).thenReturn(true);

        useCase.execute(completedCommand());

        verify(
                processedDocumentResultGateway,
                never()
        ).save(any());

        verify(
                healthDocumentGateway,
                never()
        ).save(any());
    }

    @Test
    void shouldStoreFailureAndMarkPendingDocumentAsFailed() {
        HealthDocument pendingDocument =
                pendingDocument();

        mockValidRequest(
                PATIENT_ID,
                pendingDocument
        );

        when(
                processedDocumentResultGateway
                        .existsByEventIdAndExternalResultId(
                                RESPONSE_EVENT_ID,
                                "FAILED"
                        )
        ).thenReturn(false);

        useCase.execute(failedCommand(PATIENT_ID));

        ArgumentCaptor<ProcessedDocumentResult>
                resultCaptor =
                ArgumentCaptor.forClass(
                        ProcessedDocumentResult.class
                );

        verify(processedDocumentResultGateway)
                .save(resultCaptor.capture());

        ProcessedDocumentResult savedResult =
                resultCaptor.getValue();

        assertThat(savedResult.eventId())
                .isEqualTo(RESPONSE_EVENT_ID);

        assertThat(savedResult.correlationId())
                .isEqualTo(CORRELATION_ID);

        assertThat(savedResult.externalResultId())
                .isEqualTo("FAILED");

        assertThat(savedResult.status())
                .isEqualTo(
                        DocumentProcessingStatus.FAILED
                );

        assertThat(savedResult.errorCode())
                .isEqualTo("AI_PROCESSING_FAILED");

        assertThat(savedResult.errorDetail())
                .isEqualTo(
                        "Não foi possível processar "
                                + "o documento."
                );

        assertThat(savedResult.errorRetryable())
                .isFalse();

        ArgumentCaptor<HealthDocument>
                documentCaptor =
                ArgumentCaptor.forClass(
                        HealthDocument.class
                );

        verify(healthDocumentGateway)
                .save(documentCaptor.capture());

        assertThat(
                documentCaptor
                        .getValue()
                        .getProcessingStatus()
        ).isEqualTo(DocumentProcessingStatus.FAILED);
    }

    @Test
    void shouldNotDowngradeProcessedDocumentAfterFailure() {
        HealthDocument processedDocument =
                processedDocument();

        mockValidRequest(
                PATIENT_ID,
                processedDocument
        );

        when(
                processedDocumentResultGateway
                        .existsByEventIdAndExternalResultId(
                                RESPONSE_EVENT_ID,
                                "FAILED"
                        )
        ).thenReturn(false);

        useCase.execute(failedCommand(PATIENT_ID));

        verify(processedDocumentResultGateway)
                .save(any(ProcessedDocumentResult.class));

        verify(
                healthDocumentGateway,
                never()
        ).save(any());
    }

    @Test
    void shouldValidateRequestUsingCorrelationId() {
        when(
                documentProcessingEventGateway
                        .existsByEventIdAndDocumentIdAndPatientId(
                                CORRELATION_ID,
                                DOCUMENT_ID,
                                PATIENT_ID
                        )
        ).thenReturn(false);

        assertThatThrownBy(
                () -> useCase.execute(completedCommand())
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "A resposta não corresponde a uma "
                                + "solicitação de processamento."
                );

        verifyNoInteractions(
                processedDocumentResultGateway,
                healthDocumentGateway
        );
    }

    @Test
    void shouldRejectPatientDifferentFromDocumentOwner() {
        HealthDocument pendingDocument =
                pendingDocument();

        when(
                documentProcessingEventGateway
                        .existsByEventIdAndDocumentIdAndPatientId(
                                CORRELATION_ID,
                                DOCUMENT_ID,
                                OTHER_PATIENT_ID
                        )
        ).thenReturn(true);

        when(healthDocumentGateway.findById(DOCUMENT_ID))
                .thenReturn(
                        Optional.of(pendingDocument)
                );

        assertThatThrownBy(
                () -> useCase.execute(
                        failedCommand(OTHER_PATIENT_ID)
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "O patientId da resposta não corresponde "
                                + "ao documento."
                );

        verifyNoInteractions(
                processedDocumentResultGateway
        );

        verify(
                healthDocumentGateway,
                never()
        ).save(any());
    }

    private void mockValidRequest(
            UUID patientId,
            HealthDocument healthDocument
    ) {
        when(
                documentProcessingEventGateway
                        .existsByEventIdAndDocumentIdAndPatientId(
                                CORRELATION_ID,
                                DOCUMENT_ID,
                                patientId
                        )
        ).thenReturn(true);

        when(healthDocumentGateway.findById(DOCUMENT_ID))
                .thenReturn(
                        Optional.of(healthDocument)
                );
    }

    private ProcessDocumentProcessingResultCommand
    completedCommand() {
        return new ProcessDocumentProcessingResultCommand(
                1,
                "DOCUMENT_PROCESSING_COMPLETED",
                RESPONSE_EVENT_ID,
                CORRELATION_ID,
                OCCURRED_AT,
                DOCUMENT_ID,
                PATIENT_ID,
                "Documento processado contendo: "
                        + "exame hemograma, receita.",
                "EXAME_LABORATORIAL",
                "EXAMES_LABORATORIAIS",
                LocalDate.of(2026, 7, 10),
                new BigDecimal("0.92"),
                List.of(
                        new ProcessDocumentProcessingResultItemCommand(
                                "resultado-001",
                                "EXAME_HEMOGRAMA",
                                LocalDate.of(2026, 7, 10),
                                Map.of(
                                        "exameTipo",
                                        "HEMOGRAMA"
                                )
                        ),
                        new ProcessDocumentProcessingResultItemCommand(
                                "resultado-002",
                                "RECEITA",
                                LocalDate.of(2026, 7, 11),
                                Map.of(
                                        "descricaoGeral",
                                        "Receita médica."
                                )
                        )
                ),
                null,
                null,
                null
        );
    }

    private ProcessDocumentProcessingResultCommand
    failedCommand(UUID patientId) {
        return new ProcessDocumentProcessingResultCommand(
                1,
                "DOCUMENT_PROCESSING_FAILED",
                RESPONSE_EVENT_ID,
                CORRELATION_ID,
                OCCURRED_AT,
                DOCUMENT_ID,
                patientId,
                null,
                null,
                null,
                null,
                null,
                null,
                "AI_PROCESSING_FAILED",
                "Não foi possível processar o documento.",
                false
        );
    }

    private HealthDocument pendingDocument() {
        return HealthDocument.restore(
                DOCUMENT_ID,
                PATIENT_ID,
                "documento.pdf",
                "arquivo-armazenado.pdf",
                "/storage/documento.pdf",
                "application/pdf",
                1024L,
                null,
                null,
                null,
                null,
                null,
                DocumentProcessingStatus.PENDING_PROCESSING,
                LocalDateTime.of(
                        2026,
                        7,
                        15,
                        10,
                        0
                ),
                null,
                List.of()
        );
    }

    private HealthDocument processedDocument() {
        return HealthDocument.restore(
                DOCUMENT_ID,
                PATIENT_ID,
                "documento.pdf",
                "arquivo-armazenado.pdf",
                "/storage/documento.pdf",
                "application/pdf",
                1024L,
                DocumentType.EXAME_LABORATORIAL,
                MedicalSpecialty.EXAMES_LABORATORIAIS,
                LocalDate.of(2026, 7, 10),
                "Resultado original.",
                new BigDecimal("0.91"),
                DocumentProcessingStatus.PROCESSED,
                LocalDateTime.of(
                        2026,
                        7,
                        15,
                        10,
                        0
                ),
                LocalDateTime.of(
                        2026,
                        7,
                        15,
                        10,
                        5
                ),
                List.of("EXAME_HEMOGRAMA")
        );
    }
}

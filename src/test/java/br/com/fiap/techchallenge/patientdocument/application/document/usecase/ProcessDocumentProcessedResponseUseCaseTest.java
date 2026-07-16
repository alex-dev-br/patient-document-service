package br.com.fiap.techchallenge.patientdocument.application.document.usecase;

import br.com.fiap.techchallenge.patientdocument.application.document.command.ProcessDocumentProcessedResponseCommand;
import br.com.fiap.techchallenge.patientdocument.application.document.gateway.DocumentProcessingEventGateway;
import br.com.fiap.techchallenge.patientdocument.application.document.gateway.HealthDocumentGateway;
import br.com.fiap.techchallenge.patientdocument.application.document.gateway.ProcessedDocumentResultGateway;
import br.com.fiap.techchallenge.patientdocument.application.document.result.ProcessedDocumentResult;
import br.com.fiap.techchallenge.patientdocument.domain.document.DocumentProcessingStatus;
import br.com.fiap.techchallenge.patientdocument.domain.document.DocumentType;
import br.com.fiap.techchallenge.patientdocument.domain.document.HealthDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessDocumentProcessedResponseUseCaseTest {

    private static final UUID EVENT_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID DOCUMENT_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID PATIENT_ID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final UUID OTHER_PATIENT_ID =
            UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    private DocumentProcessingEventGateway documentProcessingEventGateway;

    @Mock
    private ProcessedDocumentResultGateway processedDocumentResultGateway;

    @Mock
    private HealthDocumentGateway healthDocumentGateway;

    private ProcessDocumentProcessedResponseUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ProcessDocumentProcessedResponseUseCase(
                documentProcessingEventGateway,
                processedDocumentResultGateway,
                healthDocumentGateway
        );
    }

    @Test
    void shouldStoreSuccessfulResultAndUpdatePendingDocument() {
        HealthDocument pendingDocument = pendingDocument();

        Map<String, Object> payload = successfulPayload(
                "resultado-001",
                "EXAME_HEMOGRAMA",
                "Hemograma processado."
        );

        mockValidRequest(pendingDocument);
        when(processedDocumentResultGateway
                .existsByEventIdAndExternalResultId(
                        EVENT_ID,
                        "resultado-001"
                ))
                .thenReturn(false);

        useCase.execute(
                new ProcessDocumentProcessedResponseCommand(
                        EVENT_ID,
                        DOCUMENT_ID,
                        PATIENT_ID,
                        null,
                        payload,
                        null
                )
        );

        ArgumentCaptor<ProcessedDocumentResult> resultCaptor =
                ArgumentCaptor.forClass(ProcessedDocumentResult.class);

        verify(processedDocumentResultGateway)
                .save(resultCaptor.capture());

        ProcessedDocumentResult savedResult = resultCaptor.getValue();

        assertThat(savedResult.eventId()).isEqualTo(EVENT_ID);
        assertThat(savedResult.documentId()).isEqualTo(DOCUMENT_ID);
        assertThat(savedResult.patientId()).isEqualTo(PATIENT_ID);
        assertThat(savedResult.externalResultId())
                .isEqualTo("resultado-001");
        assertThat(savedResult.externalDocumentType())
                .isEqualTo("EXAME_HEMOGRAMA");
        assertThat(savedResult.status())
                .isEqualTo(DocumentProcessingStatus.PROCESSED);
        assertThat(savedResult.payload())
                .containsEntry("descricaoGeral", "Hemograma processado.");
        assertThat(savedResult.receivedAt()).isNotNull();

        ArgumentCaptor<HealthDocument> documentCaptor =
                ArgumentCaptor.forClass(HealthDocument.class);

        verify(healthDocumentGateway)
                .save(documentCaptor.capture());

        HealthDocument updatedDocument = documentCaptor.getValue();

        assertThat(updatedDocument.getId()).isEqualTo(DOCUMENT_ID);
        assertThat(updatedDocument.getPatientId()).isEqualTo(PATIENT_ID);
        assertThat(updatedDocument.getProcessingStatus())
                .isEqualTo(DocumentProcessingStatus.PROCESSED);
        assertThat(updatedDocument.getDocumentType())
                .isEqualTo(DocumentType.EXAME_LABORATORIAL);
        assertThat(updatedDocument.getDocumentDate())
                .isEqualTo(LocalDate.of(2026, 7, 15));
        assertThat(updatedDocument.getSummary())
                .isEqualTo("Hemograma processado.");
        assertThat(updatedDocument.getKeywords())
                .containsExactly("EXAME_HEMOGRAMA");
        assertThat(updatedDocument.getProcessedAt()).isNotNull();
    }

    @Test
    void shouldIgnoreDuplicatedResult() {
        HealthDocument pendingDocument = pendingDocument();

        Map<String, Object> payload = successfulPayload(
                "resultado-001",
                "EXAME_HEMOGRAMA",
                "Hemograma processado."
        );

        mockValidRequest(pendingDocument);
        when(processedDocumentResultGateway
                .existsByEventIdAndExternalResultId(
                        EVENT_ID,
                        "resultado-001"
                ))
                .thenReturn(true);

        useCase.execute(
                new ProcessDocumentProcessedResponseCommand(
                        EVENT_ID,
                        DOCUMENT_ID,
                        PATIENT_ID,
                        null,
                        payload,
                        null
                )
        );

        verify(processedDocumentResultGateway, never())
                .save(any());

        verify(healthDocumentGateway, never())
                .save(any());
    }

    @Test
    void shouldStoreAdditionalResultWithoutOverwritingProcessedDocument() {
        HealthDocument processedDocument = processedDocument();

        Map<String, Object> payload = successfulPayload(
                "resultado-002",
                "EXAME_TSH",
                "Exame de TSH processado."
        );

        mockValidRequest(processedDocument);
        when(processedDocumentResultGateway
                .existsByEventIdAndExternalResultId(
                        EVENT_ID,
                        "resultado-002"
                ))
                .thenReturn(false);

        useCase.execute(
                new ProcessDocumentProcessedResponseCommand(
                        EVENT_ID,
                        DOCUMENT_ID,
                        PATIENT_ID,
                        null,
                        payload,
                        null
                )
        );

        ArgumentCaptor<ProcessedDocumentResult> resultCaptor =
                ArgumentCaptor.forClass(ProcessedDocumentResult.class);

        verify(processedDocumentResultGateway)
                .save(resultCaptor.capture());

        assertThat(resultCaptor.getValue().externalResultId())
                .isEqualTo("resultado-002");

        assertThat(resultCaptor.getValue().externalDocumentType())
                .isEqualTo("EXAME_TSH");

        verify(healthDocumentGateway, never())
                .save(any());
    }

    @Test
    void shouldStoreFailureAndMarkPendingDocumentAsFailed() {
        HealthDocument pendingDocument = pendingDocument();

        mockValidRequest(pendingDocument);
        when(processedDocumentResultGateway
                .existsByEventIdAndExternalResultId(
                        EVENT_ID,
                        "FAILED"
                ))
                .thenReturn(false);

        useCase.execute(
                new ProcessDocumentProcessedResponseCommand(
                        EVENT_ID,
                        DOCUMENT_ID,
                        PATIENT_ID,
                        "FAILED",
                        null,
                        "Não foi possível processar o documento."
                )
        );

        ArgumentCaptor<ProcessedDocumentResult> resultCaptor =
                ArgumentCaptor.forClass(ProcessedDocumentResult.class);

        verify(processedDocumentResultGateway)
                .save(resultCaptor.capture());

        ProcessedDocumentResult savedResult = resultCaptor.getValue();

        assertThat(savedResult.externalResultId())
                .isEqualTo("FAILED");
        assertThat(savedResult.status())
                .isEqualTo(DocumentProcessingStatus.FAILED);
        assertThat(savedResult.payload()).isNull();
        assertThat(savedResult.errorDetail())
                .isEqualTo("Não foi possível processar o documento.");

        ArgumentCaptor<HealthDocument> documentCaptor =
                ArgumentCaptor.forClass(HealthDocument.class);

        verify(healthDocumentGateway)
                .save(documentCaptor.capture());

        HealthDocument failedDocument = documentCaptor.getValue();

        assertThat(failedDocument.getProcessingStatus())
                .isEqualTo(DocumentProcessingStatus.FAILED);
        assertThat(failedDocument.getProcessedAt()).isNotNull();
    }

    @Test
    void shouldNotDowngradeProcessedDocumentAfterLateFailure() {
        HealthDocument processedDocument = processedDocument();

        mockValidRequest(processedDocument);
        when(processedDocumentResultGateway
                .existsByEventIdAndExternalResultId(
                        EVENT_ID,
                        "FAILED"
                ))
                .thenReturn(false);

        useCase.execute(
                new ProcessDocumentProcessedResponseCommand(
                        EVENT_ID,
                        DOCUMENT_ID,
                        PATIENT_ID,
                        "FAILED",
                        null,
                        "Falha recebida após um resultado bem-sucedido."
                )
        );

        ArgumentCaptor<ProcessedDocumentResult> resultCaptor =
                ArgumentCaptor.forClass(ProcessedDocumentResult.class);

        verify(processedDocumentResultGateway)
                .save(resultCaptor.capture());

        assertThat(resultCaptor.getValue().status())
                .isEqualTo(DocumentProcessingStatus.FAILED);

        verify(healthDocumentGateway, never())
                .save(any());
    }

    @Test
    void shouldRejectDivergentPatientInsidePayload() {
        HealthDocument pendingDocument = pendingDocument();

        Map<String, Object> payload = Map.of(
                "id", "resultado-invalido",
                "patientId", OTHER_PATIENT_ID.toString(),
                "documentType", "EXAME_HEMOGRAMA"
        );

        mockValidRequest(pendingDocument);

        ProcessDocumentProcessedResponseCommand command =
                new ProcessDocumentProcessedResponseCommand(
                        EVENT_ID,
                        DOCUMENT_ID,
                        PATIENT_ID,
                        null,
                        payload,
                        null
                );

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "O patientId interno do resultado é divergente."
                );

        verifyNoInteractions(processedDocumentResultGateway);

        verify(healthDocumentGateway, never())
                .save(any());
    }

    private void mockValidRequest(HealthDocument healthDocument) {
        when(documentProcessingEventGateway
                .existsByEventIdAndDocumentIdAndPatientId(
                        EVENT_ID,
                        DOCUMENT_ID,
                        PATIENT_ID
                ))
                .thenReturn(true);

        when(healthDocumentGateway.findById(DOCUMENT_ID))
                .thenReturn(Optional.of(healthDocument));
    }

    private Map<String, Object> successfulPayload(
            String resultId,
            String documentType,
            String description
    ) {
        return Map.of(
                "id", resultId,
                "patientId", PATIENT_ID.toString(),
                "documentType", documentType,
                "documentDate", "2026-07-15T00:00:00",
                "descricaoGeral", description
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
                LocalDateTime.of(2026, 7, 15, 10, 0),
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
                null,
                LocalDate.of(2026, 7, 15),
                "Resultado original.",
                null,
                DocumentProcessingStatus.PROCESSED,
                LocalDateTime.of(2026, 7, 15, 10, 0),
                LocalDateTime.of(2026, 7, 15, 10, 5),
                List.of("EXAME_HEMOGRAMA")
        );
    }
}

package br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka;

import br.com.fiap.techchallenge.patientdocument.application.document.command.ProcessDocumentProcessingResultCommand;
import br.com.fiap.techchallenge.patientdocument.application.document.usecase.ProcessDocumentProcessingResultUseCase;
import br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka.validation.DocumentProcessingResultValidator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DocumentProcessingResultListenerTest {

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

    @Mock
    private ProcessDocumentProcessingResultUseCase useCase;

    private DocumentProcessingResultListener listener;

    @BeforeEach
    void setUp() {
        listener =
                new DocumentProcessingResultListener(
                        useCase,
                        new DocumentProcessingResultValidator()
                );
    }

    @Test
    void shouldDeserializeValidateAndMapCompletedMessage() {
        listener.consume(
                record(
                        DOCUMENT_ID.toString(),
                        completedPayload()
                )
        );

        ArgumentCaptor<ProcessDocumentProcessingResultCommand>
                captor =
                ArgumentCaptor.forClass(
                        ProcessDocumentProcessingResultCommand.class
                );

        verify(useCase).execute(captor.capture());

        ProcessDocumentProcessingResultCommand command =
                captor.getValue();

        assertThat(command.schemaVersion())
                .isEqualTo(1);

        assertThat(command.eventType())
                .isEqualTo(
                        "DOCUMENT_PROCESSING_COMPLETED"
                );

        assertThat(command.eventId())
                .isEqualTo(RESPONSE_EVENT_ID);

        assertThat(command.correlationId())
                .isEqualTo(CORRELATION_ID);

        assertThat(command.documentId())
                .isEqualTo(DOCUMENT_ID);

        assertThat(command.patientId())
                .isEqualTo(PATIENT_ID);

        assertThat(command.summary())
                .isEqualTo(
                        "Documento processado contendo: "
                                + "exame hemograma, receita."
                );

        assertThat(command.primaryDocumentType())
                .isEqualTo("EXAME_LABORATORIAL");

        assertThat(command.specialty())
                .isEqualTo("EXAMES_LABORATORIAIS");

        assertThat(command.documentDate())
                .isEqualTo(LocalDate.of(2026, 7, 10));

        assertThat(command.confidence())
                .isEqualByComparingTo(
                        new BigDecimal("0.92")
                );

        assertThat(command.results())
                .hasSize(2);

        assertThat(command.results())
                .extracting(
                        result -> result.resultId()
                )
                .containsExactly(
                        "resultado-001",
                        "resultado-002"
                );

        assertThat(command.errorCode()).isNull();
        assertThat(command.errorDetail()).isNull();
        assertThat(command.errorRetryable()).isNull();
    }

    @Test
    void shouldRejectKafkaKeyDifferentFromDocumentId() {
        assertThatThrownBy(
                () -> listener.consume(
                        record(
                                UUID.randomUUID().toString(),
                                completedPayload()
                        )
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "A chave Kafka não corresponde "
                                + "ao documentId da mensagem."
                );

        verifyNoInteractions(useCase);
    }

    @Test
    void shouldRejectUnknownJsonProperty() {
        String invalidPayload =
                completedPayload().replace(
                        """
                        "results": [
                        """,
                        """
                        "futureEnvelopeField": "não permitido",
                        "results": [
                        """
                );

        assertThatThrownBy(
                () -> listener.consume(
                        record(
                                DOCUMENT_ID.toString(),
                                invalidPayload
                        )
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "O payload Kafka não corresponde ao "
                                + "contrato de resultado agregado."
                );

        verifyNoInteractions(useCase);
    }

    private ConsumerRecord<String, String> record(
            String key,
            String payload
    ) {
        return new ConsumerRecord<>(
                "document-processing-result",
                0,
                0L,
                key,
                payload
        );
    }

    private String completedPayload() {
        return """
                {
                  "schemaVersion": 1,
                  "eventType": "DOCUMENT_PROCESSING_COMPLETED",
                  "eventId": "%s",
                  "correlationId": "%s",
                  "occurredAt": "2026-07-22T18:00:00Z",
                  "documentId": "%s",
                  "patientId": "%s",
                  "summary": "Documento processado contendo: exame hemograma, receita.",
                  "primaryDocumentType": "EXAME_LABORATORIAL",
                  "specialty": "EXAMES_LABORATORIAIS",
                  "documentDate": "2026-07-10",
                  "confidence": 0.92,
                  "results": [
                    {
                      "resultId": "resultado-001",
                      "documentType": "EXAME_HEMOGRAMA",
                      "documentDate": "2026-07-10",
                      "data": {
                        "exameTipo": "HEMOGRAMA"
                      }
                    },
                    {
                      "resultId": "resultado-002",
                      "documentType": "RECEITA",
                      "documentDate": "2026-07-11",
                      "data": {
                        "descricaoGeral": "Receita médica."
                      }
                    }
                  ],
                  "error": null
                }
                """.formatted(
                RESPONSE_EVENT_ID,
                CORRELATION_ID,
                DOCUMENT_ID,
                PATIENT_ID
        );
    }
}

package br.com.fiap.techchallenge.patientdocument.infrastructure.messaging.kafka;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentProcessingResultMessageTest {

    @Test
    void shouldDefensivelyCopyResultsAndData() {
        Map<String, Object> sourceData =
                new LinkedHashMap<>();

        sourceData.put("exam", "hemograma");

        DocumentProcessingResultItemMessage item =
                new DocumentProcessingResultItemMessage(
                        "resultado-001",
                        "EXAME_HEMOGRAMA",
                        LocalDate.of(2026, 7, 10),
                        sourceData
                );

        List<DocumentProcessingResultItemMessage> sourceResults =
                new ArrayList<>();

        sourceResults.add(item);

        DocumentProcessingResultMessage message =
                new DocumentProcessingResultMessage(
                        1,
                        "DOCUMENT_PROCESSING_COMPLETED",
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Instant.parse(
                                "2026-07-22T18:00:00Z"
                        ),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Hemograma processado.",
                        "EXAME_HEMOGRAMA",
                        "EXAMES_LABORATORIAIS",
                        LocalDate.of(2026, 7, 10),
                        new BigDecimal("0.92"),
                        sourceResults,
                        null
                );

        sourceData.put("altered", true);
        sourceResults.clear();

        assertThat(message.results())
                .hasSize(1);

        assertThat(message.results().getFirst().data())
                .containsExactly(
                        Map.entry(
                                "exam",
                                "hemograma"
                        )
                );

        assertThatThrownBy(
                () -> message.results().clear()
        )
                .isInstanceOf(
                        UnsupportedOperationException.class
                );

        assertThatThrownBy(
                () -> message
                        .results()
                        .getFirst()
                        .data()
                        .put("newField", "value")
        )
                .isInstanceOf(
                        UnsupportedOperationException.class
                );
    }
}

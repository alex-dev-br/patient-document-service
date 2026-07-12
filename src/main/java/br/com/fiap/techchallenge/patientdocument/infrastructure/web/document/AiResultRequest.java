package br.com.fiap.techchallenge.patientdocument.infrastructure.web.document;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AiResultRequest(
        String documentType,
        String specialty,
        LocalDate documentDate,

        @Size(max = 5000, message = "O resumo deve ter no máximo 5000 caracteres")
        String summary,

        List<String> keywords,

        @DecimalMin(value = "0.0", message = "A confiança deve ser maior ou igual a 0")
        @DecimalMax(value = "1.0", message = "A confiança deve ser menor ou igual a 1")
        BigDecimal confidence,

        String status
) {
}

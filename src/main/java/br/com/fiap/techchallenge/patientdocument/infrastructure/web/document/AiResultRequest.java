package br.com.fiap.techchallenge.patientdocument.infrastructure.web.document;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Resultado produzido pelo processamento de inteligência artificial")
public record AiResultRequest(

        @Schema(
                description = "Tipo identificado para o documento",
                example = "EXAME_LABORATORIAL",
                allowableValues = {
                        "EXAME_LABORATORIAL",
                        "RECEITA_MEDICA",
                        "LAUDO_MEDICO",
                        "ENCAMINHAMENTO",
                        "RELATORIO_MEDICO",
                        "VACINACAO",
                        "INTERNACAO",
                        "ORIENTACAO_POS_CONSULTA",
                        "OUTRO"
                }
        )
        String documentType,

        @Schema(
                description = "Especialidade médica associada ao documento",
                example = "ENDOCRINOLOGIA",
                allowableValues = {
                        "CLINICA_MEDICA",
                        "CARDIOLOGIA",
                        "ENDOCRINOLOGIA",
                        "PEDIATRIA",
                        "GINECOLOGIA",
                        "ORTOPEDIA",
                        "ODONTOLOGIA",
                        "VACINACAO",
                        "EXAMES_LABORATORIAIS",
                        "OUTRA"
                }
        )
        String specialty,

        @Schema(
                description = "Data médica identificada no documento",
                example = "2026-06-10"
        )
        LocalDate documentDate,

        @Schema(
                description = "Resumo do conteúdo clínico identificado",
                example = "Exame laboratorial contendo informações de glicemia e colesterol."
        )
        @Size(max = 5000, message = "O resumo deve ter no máximo 5000 caracteres")
        String summary,

        @Schema(
                description = "Palavras-chave identificadas no documento",
                example = "[\"glicemia\", \"colesterol\", \"exame laboratorial\"]"
        )
        List<String> keywords,

        @Schema(
                description = "Confiança do resultado, entre zero e um",
                example = "0.92",
                minimum = "0",
                maximum = "1"
        )
        @DecimalMin(value = "0.0", message = "A confiança deve ser maior ou igual a 0")
        @DecimalMax(value = "1.0", message = "A confiança deve ser menor ou igual a 1")
        BigDecimal confidence,

        @Schema(
                description = "Situação do processamento do documento",
                example = "PROCESSED",
                allowableValues = {
                        "PENDING_PROCESSING",
                        "PROCESSING",
                        "PROCESSED",
                        "FAILED"
                }
        )
        String status
) {
}

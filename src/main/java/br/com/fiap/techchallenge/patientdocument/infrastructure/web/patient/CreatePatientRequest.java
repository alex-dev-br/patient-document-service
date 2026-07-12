package br.com.fiap.techchallenge.patientdocument.infrastructure.web.patient;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Dados necessários para cadastrar um paciente")
public record CreatePatientRequest(

        @Schema(
                description = "Nome completo do paciente",
                example = "Alexandre Andrade"
        )
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
        String name,

        @Schema(
                description = "Data de nascimento do paciente",
                example = "1990-01-15"
        )
        @NotNull(message = "A data de nascimento é obrigatória")
        LocalDate birthDate,

        @Schema(
                description = "CPF do paciente, com ou sem formatação",
                example = "12345678901"
        )
        @Size(max = 14, message = "O CPF deve ter no máximo 14 caracteres")
        String cpf,

        @Schema(
                description = "Endereço de e-mail do paciente",
                example = "alexandre@email.com"
        )
        @Email(message = "E-mail inválido")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres")
        String email,

        @Schema(
                description = "Telefone do paciente",
                example = "21999999999"
        )
        @Size(max = 30, message = "O telefone deve ter no máximo 30 caracteres")
        String phone
) {
}

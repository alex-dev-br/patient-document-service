package br.com.fiap.techchallenge.patientdocument.infrastructure.web.patient;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreatePatientRequest(

        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
        String name,

        @NotNull(message = "A data de nascimento é obrigatória")
        LocalDate birthDate,

        @Size(max = 14, message = "O CPF deve ter no máximo 14 caracteres")
        String cpf,

        @Email(message = "E-mail inválido")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres")
        String email,

        @Size(max = 30, message = "O telefone deve ter no máximo 30 caracteres")
        String phone
) {
}

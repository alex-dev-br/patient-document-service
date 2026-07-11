package br.com.fiap.techchallenge.patientdocument.domain.patient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class Patient {

    private final UUID id;
    private final String name;
    private final LocalDate birthDate;
    private final String cpf;
    private final String email;
    private final String phone;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private Patient(
            UUID id,
            String name,
            LocalDate birthDate,
            String cpf,
            String email,
            String phone,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.birthDate = birthDate;
        this.cpf = cpf;
        this.email = email;
        this.phone = phone;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Patient create(
            String name,
            LocalDate birthDate,
            String cpf,
            String email,
            String phone
    ) {
        return new Patient(
                UUID.randomUUID(),
                name,
                birthDate,
                cpf,
                email,
                phone,
                LocalDateTime.now(),
                null
        );
    }

    public static Patient restore(
            UUID id,
            String name,
            LocalDate birthDate,
            String cpf,
            String email,
            String phone,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new Patient(
                id,
                name,
                birthDate,
                cpf,
                email,
                phone,
                createdAt,
                updatedAt
        );
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getCpf() {
        return cpf;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

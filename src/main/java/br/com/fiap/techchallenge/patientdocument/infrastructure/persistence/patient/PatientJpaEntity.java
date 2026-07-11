package br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.patient;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "patients")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(length = 14)
    private String cpf;

    @Column(length = 150)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

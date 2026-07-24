package br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.audit;

import br.com.fiap.techchallenge.patientdocument.application.audit.AccessAction;
import br.com.fiap.techchallenge.patientdocument.application.audit.AccessOutcome;
import br.com.fiap.techchallenge.patientdocument.application.audit.AccessResourceType;
import br.com.fiap.techchallenge.patientdocument.application.audit.ActorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "access_audit_log")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AccessAuditJpaEntity {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private AccessAction action;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "resource_type",
            nullable = false,
            length = 50
    )
    private AccessResourceType resourceType;

    @Column(
            name = "resource_id",
            length = 100
    )
    private String resourceId;

    @Column(name = "patient_id")
    private UUID patientId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "actor_type",
            nullable = false,
            length = 30
    )
    private ActorType actorType;

    @Column(
            name = "actor_subject",
            length = 512
    )
    private String actorSubject;

    @Column(
            name = "actor_client_id",
            length = 255
    )
    private String actorClientId;

    @Column(
            name = "actor_username",
            length = 255
    )
    private String actorUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccessOutcome outcome;

    @Column(length = 1000)
    private String detail;

    @Column(
            name = "occurred_at",
            nullable = false
    )
    private Instant occurredAt;
}
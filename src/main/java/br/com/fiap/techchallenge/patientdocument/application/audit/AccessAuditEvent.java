package br.com.fiap.techchallenge.patientdocument.application.audit;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AccessAuditEvent(
        UUID id,
        AccessAction action,
        AccessResourceType resourceType,
        String resourceId,
        UUID patientId,
        AuthenticatedActor actor,
        AccessOutcome outcome,
        String detail,
        Instant occurredAt
) {

    private static final int MAX_RESOURCE_ID_LENGTH = 100;
    private static final int MAX_DETAIL_LENGTH = 1000;

    public AccessAuditEvent {
        id = Objects.requireNonNull(
                id,
                "O identificador do evento de auditoria é obrigatório."
        );

        action = Objects.requireNonNull(
                action,
                "A ação auditada é obrigatória."
        );

        resourceType = Objects.requireNonNull(
                resourceType,
                "O tipo do recurso auditado é obrigatório."
        );

        actor = Objects.requireNonNull(
                actor,
                "O ator da auditoria é obrigatório."
        );

        outcome = Objects.requireNonNull(
                outcome,
                "O resultado da auditoria é obrigatório."
        );

        occurredAt = Objects.requireNonNull(
                occurredAt,
                "O instante da auditoria é obrigatório."
        );

        resourceId = normalize(resourceId);
        detail = normalize(detail);

        validateMaximumLength(
                resourceId,
                MAX_RESOURCE_ID_LENGTH,
                "O identificador do recurso"
        );

        validateMaximumLength(
                detail,
                MAX_DETAIL_LENGTH,
                "O detalhe da auditoria"
        );
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private static void validateMaximumLength(
            String value,
            int maximumLength,
            String fieldName
    ) {
        if (value != null && value.length() > maximumLength) {
            throw new IllegalArgumentException(
                    fieldName +
                            " não pode possuir mais de " +
                            maximumLength +
                            " caracteres."
            );
        }
    }
}
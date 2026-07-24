package br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.audit;

import br.com.fiap.techchallenge.patientdocument.application.audit.AccessAuditEvent;
import org.springframework.stereotype.Component;

@Component
public class AccessAuditPersistenceMapper {

    public AccessAuditJpaEntity toEntity(
            AccessAuditEvent event
    ) {
        return AccessAuditJpaEntity.builder()
                .id(event.id())
                .action(event.action())
                .resourceType(event.resourceType())
                .resourceId(event.resourceId())
                .patientId(event.patientId())
                .actorType(event.actor().type())
                .actorSubject(event.actor().subject())
                .actorClientId(event.actor().clientId())
                .actorUsername(event.actor().username())
                .outcome(event.outcome())
                .detail(event.detail())
                .occurredAt(event.occurredAt())
                .build();
    }
}
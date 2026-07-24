package br.com.fiap.techchallenge.patientdocument.application.audit.usecase;

import br.com.fiap.techchallenge.patientdocument.application.audit.AccessAuditEvent;
import br.com.fiap.techchallenge.patientdocument.application.audit.AuthenticatedActor;
import br.com.fiap.techchallenge.patientdocument.application.audit.AuthenticatedActorProvider;
import br.com.fiap.techchallenge.patientdocument.application.audit.command.RecordAccessAuditCommand;
import br.com.fiap.techchallenge.patientdocument.application.audit.gateway.AccessAuditGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecordAccessAuditUseCase {

    private final AuthenticatedActorProvider
            authenticatedActorProvider;

    private final AccessAuditGateway
            accessAuditGateway;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AccessAuditEvent execute(
            RecordAccessAuditCommand command
    ) {
        Objects.requireNonNull(
                command,
                "O comando de auditoria é obrigatório."
        );

        AuthenticatedActor actor =
                authenticatedActorProvider.getCurrentActor();

        AccessAuditEvent event =
                new AccessAuditEvent(
                        UUID.randomUUID(),
                        command.action(),
                        command.resourceType(),
                        command.resourceId(),
                        command.patientId(),
                        actor,
                        command.outcome(),
                        command.detail(),
                        Instant.now()
                );

        accessAuditGateway.append(event);

        return event;
    }
}
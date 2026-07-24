package br.com.fiap.techchallenge.patientdocument.application.audit.command;

import br.com.fiap.techchallenge.patientdocument.application.audit.AccessAction;
import br.com.fiap.techchallenge.patientdocument.application.audit.AccessOutcome;
import br.com.fiap.techchallenge.patientdocument.application.audit.AccessResourceType;

import java.util.UUID;

public record RecordAccessAuditCommand(
        AccessAction action,
        AccessResourceType resourceType,
        String resourceId,
        UUID patientId,
        AccessOutcome outcome,
        String detail
) {
}
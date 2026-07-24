package br.com.fiap.techchallenge.patientdocument.application.audit.gateway;

import br.com.fiap.techchallenge.patientdocument.application.audit.AccessAuditEvent;

public interface AccessAuditGateway {

    void append(AccessAuditEvent event);
}
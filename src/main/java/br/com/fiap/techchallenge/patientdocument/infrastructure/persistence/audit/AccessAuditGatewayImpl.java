package br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.audit;

import br.com.fiap.techchallenge.patientdocument.application.audit.AccessAuditEvent;
import br.com.fiap.techchallenge.patientdocument.application.audit.gateway.AccessAuditGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccessAuditGatewayImpl
        implements AccessAuditGateway {

    private final AccessAuditJpaRepository
            accessAuditJpaRepository;

    private final AccessAuditPersistenceMapper
            accessAuditPersistenceMapper;

    @Override
    public void append(AccessAuditEvent event) {
        AccessAuditJpaEntity entity =
                accessAuditPersistenceMapper.toEntity(event);

        accessAuditJpaRepository.save(entity);
    }
}
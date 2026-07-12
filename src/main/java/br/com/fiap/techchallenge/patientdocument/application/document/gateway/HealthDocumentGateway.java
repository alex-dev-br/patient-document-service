package br.com.fiap.techchallenge.patientdocument.application.document.gateway;

import br.com.fiap.techchallenge.patientdocument.application.document.query.HealthDocumentFilter;
import br.com.fiap.techchallenge.patientdocument.domain.document.HealthDocument;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HealthDocumentGateway {

    HealthDocument save(HealthDocument document);

    Optional<HealthDocument> findById(UUID id);

    List<HealthDocument> findByPatientId(UUID patientId);

    List<HealthDocument> findByPatientId(UUID patientId, HealthDocumentFilter filter);
}

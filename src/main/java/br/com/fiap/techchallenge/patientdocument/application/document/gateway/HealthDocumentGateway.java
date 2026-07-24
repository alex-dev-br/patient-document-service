package br.com.fiap.techchallenge.patientdocument.application.document.gateway;

import br.com.fiap.techchallenge.patientdocument.application.common.pagination.PageQuery;
import br.com.fiap.techchallenge.patientdocument.application.common.pagination.PagedResult;
import br.com.fiap.techchallenge.patientdocument.application.document.query.HealthDocumentFilter;
import br.com.fiap.techchallenge.patientdocument.application.document.query.HealthDocumentSort;
import br.com.fiap.techchallenge.patientdocument.domain.document.HealthDocument;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HealthDocumentGateway {

    HealthDocument save(HealthDocument document);

    Optional<HealthDocument> findById(UUID id);

    List<HealthDocument> findByPatientId(UUID patientId);

    List<HealthDocument> findByPatientId(
            UUID patientId,
            HealthDocumentFilter filter
    );

    PagedResult<HealthDocument> findByPatientId(
            UUID patientId,
            HealthDocumentFilter filter,
            PageQuery pageQuery
    );

    default PagedResult<HealthDocument> findByPatientId(
            UUID patientId,
            HealthDocumentFilter filter,
            PageQuery pageQuery,
            HealthDocumentSort sort
    ) {
        if (sort != HealthDocumentSort.CREATED_AT_DESC) {
            throw new UnsupportedOperationException(
                    "A ordenação solicitada não é suportada pelo gateway."
            );
        }

        return findByPatientId(
                patientId,
                filter,
                pageQuery
        );
    }
}

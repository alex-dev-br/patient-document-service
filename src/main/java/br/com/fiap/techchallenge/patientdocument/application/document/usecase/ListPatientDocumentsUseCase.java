package br.com.fiap.techchallenge.patientdocument.application.document.usecase;

import br.com.fiap.techchallenge.patientdocument.application.common.pagination.PageQuery;
import br.com.fiap.techchallenge.patientdocument.application.common.pagination.PagedResult;
import br.com.fiap.techchallenge.patientdocument.application.document.gateway.HealthDocumentGateway;
import br.com.fiap.techchallenge.patientdocument.application.document.query.HealthDocumentFilter;
import br.com.fiap.techchallenge.patientdocument.application.exception.ResourceNotFoundException;
import br.com.fiap.techchallenge.patientdocument.application.patient.gateway.PatientGateway;
import br.com.fiap.techchallenge.patientdocument.domain.document.HealthDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListPatientDocumentsUseCase {

    private final PatientGateway patientGateway;
    private final HealthDocumentGateway healthDocumentGateway;

    @Transactional(readOnly = true)
    public List<HealthDocument> execute(UUID patientId) {
        return execute(patientId, HealthDocumentFilter.empty());
    }

    @Transactional(readOnly = true)
    public List<HealthDocument> execute(
            UUID patientId,
            HealthDocumentFilter filter
    ) {
        HealthDocumentFilter validatedFilter =
                validateAndNormalize(patientId, filter);

        return healthDocumentGateway.findByPatientId(
                patientId,
                validatedFilter
        );
    }

    @Transactional(readOnly = true)
    public PagedResult<HealthDocument> execute(
            UUID patientId,
            HealthDocumentFilter filter,
            PageQuery pageQuery
    ) {
        HealthDocumentFilter validatedFilter =
                validateAndNormalize(patientId, filter);

        if (pageQuery == null) {
            throw new IllegalArgumentException(
                    "Os parâmetros de paginação são obrigatórios."
            );
        }

        return healthDocumentGateway.findByPatientId(
                patientId,
                validatedFilter,
                pageQuery
        );
    }

    private HealthDocumentFilter validateAndNormalize(
            UUID patientId,
            HealthDocumentFilter filter
    ) {
        patientGateway.findById(patientId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Paciente não encontrado: " + patientId
                        )
                );

        HealthDocumentFilter normalizedFilter = filter == null
                ? HealthDocumentFilter.empty()
                : filter;

        if (normalizedFilter.hasInvalidDateRange()) {
            throw new IllegalArgumentException(
                    "A data inicial não pode ser posterior à data final."
            );
        }

        return normalizedFilter;
    }
}

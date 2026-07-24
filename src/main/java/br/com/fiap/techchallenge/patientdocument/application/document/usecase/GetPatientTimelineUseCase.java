package br.com.fiap.techchallenge.patientdocument.application.document.usecase;

import br.com.fiap.techchallenge.patientdocument.application.audit.AccessAction;
import br.com.fiap.techchallenge.patientdocument.application.audit.AccessOutcome;
import br.com.fiap.techchallenge.patientdocument.application.audit.AccessResourceType;
import br.com.fiap.techchallenge.patientdocument.application.audit.command.RecordAccessAuditCommand;
import br.com.fiap.techchallenge.patientdocument.application.audit.usecase.RecordAccessAuditUseCase;
import br.com.fiap.techchallenge.patientdocument.application.common.pagination.PageQuery;
import br.com.fiap.techchallenge.patientdocument.application.common.pagination.PagedResult;
import br.com.fiap.techchallenge.patientdocument.application.document.query.HealthDocumentFilter;
import br.com.fiap.techchallenge.patientdocument.application.document.query.HealthDocumentSort;
import br.com.fiap.techchallenge.patientdocument.domain.document.HealthDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetPatientTimelineUseCase {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;

    private final ListPatientDocumentsUseCase
            listPatientDocumentsUseCase;

    private final RecordAccessAuditUseCase
            recordAccessAuditUseCase;

    @Transactional(readOnly = true)
    public PagedResult<HealthDocument> execute(
            UUID patientId
    ) {
        return execute(
                patientId,
                HealthDocumentFilter.empty(),
                new PageQuery(
                        DEFAULT_PAGE,
                        DEFAULT_SIZE
                )
        );
    }

    @Transactional(readOnly = true)
    public PagedResult<HealthDocument> execute(
            UUID patientId,
            HealthDocumentFilter filter,
            PageQuery pageQuery
    ) {
        PagedResult<HealthDocument> timeline =
                listPatientDocumentsUseCase.execute(
                        patientId,
                        filter,
                        pageQuery,
                        HealthDocumentSort.TIMELINE_DESC
                );

        recordAccessAuditUseCase.execute(
                new RecordAccessAuditCommand(
                        AccessAction.VIEW_PATIENT_TIMELINE,
                        AccessResourceType.TIMELINE,
                        patientId.toString(),
                        patientId,
                        AccessOutcome.ALLOWED,
                        "Timeline do paciente consultada."
                )
        );

        return timeline;
    }
}

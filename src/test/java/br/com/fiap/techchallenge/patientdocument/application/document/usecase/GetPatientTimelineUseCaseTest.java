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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetPatientTimelineUseCaseTest {

    private static final UUID PATIENT_ID =
            UUID.fromString(
                    "11111111-1111-1111-1111-111111111111"
            );

    @Mock
    private ListPatientDocumentsUseCase
            listPatientDocumentsUseCase;

    @Mock
    private RecordAccessAuditUseCase
            recordAccessAuditUseCase;

    private GetPatientTimelineUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetPatientTimelineUseCase(
                listPatientDocumentsUseCase,
                recordAccessAuditUseCase
        );
    }

    @Test
    void shouldReturnDefaultPagedTimelineAndRecordAllowedAccess() {
        HealthDocumentFilter filter =
                HealthDocumentFilter.empty();

        PageQuery pageQuery =
                new PageQuery(
                        0,
                        10
                );

        HealthDocument firstDocument =
                mock(HealthDocument.class);

        HealthDocument secondDocument =
                mock(HealthDocument.class);

        PagedResult<HealthDocument> expectedResult =
                new PagedResult<>(
                        List.of(
                                firstDocument,
                                secondDocument
                        ),
                        0,
                        10,
                        2,
                        1,
                        true,
                        true
                );

        when(
                listPatientDocumentsUseCase.execute(
                        PATIENT_ID,
                        filter,
                        pageQuery,
                        HealthDocumentSort.TIMELINE_DESC
                )
        )
                .thenReturn(expectedResult);

        PagedResult<HealthDocument> result =
                useCase.execute(PATIENT_ID);

        assertThat(result)
                .isSameAs(expectedResult);

        assertThat(result.content())
                .containsExactly(
                        firstDocument,
                        secondDocument
                );

        verify(listPatientDocumentsUseCase)
                .execute(
                        PATIENT_ID,
                        filter,
                        pageQuery,
                        HealthDocumentSort.TIMELINE_DESC
                );

        ArgumentCaptor<RecordAccessAuditCommand> captor =
                ArgumentCaptor.forClass(
                        RecordAccessAuditCommand.class
                );

        verify(recordAccessAuditUseCase)
                .execute(
                        captor.capture()
                );

        RecordAccessAuditCommand command =
                captor.getValue();

        assertThat(command.action())
                .isEqualTo(
                        AccessAction.VIEW_PATIENT_TIMELINE
                );

        assertThat(command.resourceType())
                .isEqualTo(
                        AccessResourceType.TIMELINE
                );

        assertThat(command.resourceId())
                .isEqualTo(
                        PATIENT_ID.toString()
                );

        assertThat(command.patientId())
                .isEqualTo(PATIENT_ID);

        assertThat(command.outcome())
                .isEqualTo(
                        AccessOutcome.ALLOWED
                );

        assertThat(command.detail())
                .isEqualTo(
                        "Timeline do paciente consultada."
                );
    }

    @Test
    void shouldDelegateFiltersPaginationAndTimelineOrdering() {
        HealthDocumentFilter filter =
                new HealthDocumentFilter(
                        null,
                        null,
                        null,
                        "glicemia",
                        LocalDate.of(
                                2026,
                                1,
                                1
                        ),
                        LocalDate.of(
                                2026,
                                12,
                                31
                        )
                );

        PageQuery pageQuery =
                new PageQuery(
                        2,
                        5
                );

        HealthDocument document =
                mock(HealthDocument.class);

        PagedResult<HealthDocument> expectedResult =
                new PagedResult<>(
                        List.of(document),
                        2,
                        5,
                        11,
                        3,
                        false,
                        true
                );

        when(
                listPatientDocumentsUseCase.execute(
                        PATIENT_ID,
                        filter,
                        pageQuery,
                        HealthDocumentSort.TIMELINE_DESC
                )
        )
                .thenReturn(expectedResult);

        PagedResult<HealthDocument> result =
                useCase.execute(
                        PATIENT_ID,
                        filter,
                        pageQuery
                );

        assertThat(result)
                .isSameAs(expectedResult);

        assertThat(result.page())
                .isEqualTo(2);

        assertThat(result.size())
                .isEqualTo(5);

        assertThat(result.totalElements())
                .isEqualTo(11);

        assertThat(result.totalPages())
                .isEqualTo(3);

        verify(listPatientDocumentsUseCase)
                .execute(
                        PATIENT_ID,
                        filter,
                        pageQuery,
                        HealthDocumentSort.TIMELINE_DESC
                );

        verify(recordAccessAuditUseCase)
                .execute(
                        any(RecordAccessAuditCommand.class)
                );
    }
}

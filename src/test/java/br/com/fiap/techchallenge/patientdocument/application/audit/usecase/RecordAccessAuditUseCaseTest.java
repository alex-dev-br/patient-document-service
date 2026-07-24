package br.com.fiap.techchallenge.patientdocument.application.audit.usecase;

import br.com.fiap.techchallenge.patientdocument.application.audit.AccessAction;
import br.com.fiap.techchallenge.patientdocument.application.audit.AccessAuditEvent;
import br.com.fiap.techchallenge.patientdocument.application.audit.AccessOutcome;
import br.com.fiap.techchallenge.patientdocument.application.audit.AccessResourceType;
import br.com.fiap.techchallenge.patientdocument.application.audit.ActorType;
import br.com.fiap.techchallenge.patientdocument.application.audit.AuthenticatedActor;
import br.com.fiap.techchallenge.patientdocument.application.audit.AuthenticatedActorProvider;
import br.com.fiap.techchallenge.patientdocument.application.audit.command.RecordAccessAuditCommand;
import br.com.fiap.techchallenge.patientdocument.application.audit.gateway.AccessAuditGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordAccessAuditUseCaseTest {

    private static final UUID PATIENT_ID =
            UUID.fromString(
                    "11111111-1111-1111-1111-111111111111"
            );

    private static final String ACTOR_SUBJECT =
            "service-account-subject";

    @Mock
    private AuthenticatedActorProvider
            authenticatedActorProvider;

    @Mock
    private AccessAuditGateway
            accessAuditGateway;

    private RecordAccessAuditUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RecordAccessAuditUseCase(
                authenticatedActorProvider,
                accessAuditGateway
        );
    }

    @Test
    void shouldAppendAccessAuditUsingCurrentActor() {
        AuthenticatedActor actor =
                AuthenticatedActor.authenticated(
                        ActorType.SERVICE_ACCOUNT,
                        ACTOR_SUBJECT,
                        "patient-document-dev-client",
                        "service-account-patient-document-dev-client"
                );

        when(authenticatedActorProvider.getCurrentActor())
                .thenReturn(actor);

        RecordAccessAuditCommand command =
                new RecordAccessAuditCommand(
                        AccessAction.VIEW_PATIENT_TIMELINE,
                        AccessResourceType.TIMELINE,
                        PATIENT_ID.toString(),
                        PATIENT_ID,
                        AccessOutcome.ALLOWED,
                        "Timeline consultada."
                );

        Instant startedAt = Instant.now();

        AccessAuditEvent event =
                useCase.execute(command);

        Instant finishedAt = Instant.now();

        assertThat(event.id())
                .isNotNull();

        assertThat(event.action())
                .isEqualTo(
                        AccessAction.VIEW_PATIENT_TIMELINE
                );

        assertThat(event.resourceType())
                .isEqualTo(
                        AccessResourceType.TIMELINE
                );

        assertThat(event.resourceId())
                .isEqualTo(PATIENT_ID.toString());

        assertThat(event.patientId())
                .isEqualTo(PATIENT_ID);

        assertThat(event.actor())
                .isEqualTo(actor);

        assertThat(event.outcome())
                .isEqualTo(AccessOutcome.ALLOWED);

        assertThat(event.detail())
                .isEqualTo("Timeline consultada.");

        assertThat(event.occurredAt())
                .isAfterOrEqualTo(startedAt)
                .isBeforeOrEqualTo(finishedAt);

        verify(accessAuditGateway)
                .append(event);
    }

    @Test
    void shouldRejectMissingCommand() {
        assertThatThrownBy(
                () -> useCase.execute(null)
        )
                .isInstanceOf(
                        NullPointerException.class
                )
                .hasMessage(
                        "O comando de auditoria é obrigatório."
                );

        verifyNoInteractions(
                authenticatedActorProvider,
                accessAuditGateway
        );
    }
}
package br.com.fiap.techchallenge.patientdocument.infrastructure.security;

import br.com.fiap.techchallenge.patientdocument.application.audit.ActorType;
import br.com.fiap.techchallenge.patientdocument.application.audit.AuthenticatedActor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpringSecurityAuthenticatedActorProviderTest {

    private static final Instant ISSUED_AT =
            Instant.parse("2026-07-23T16:00:00Z");

    private static final Instant EXPIRES_AT =
            Instant.parse("2026-07-23T17:00:00Z");

    private static final String DOCUMENTS_READ_AUTHORITY =
            "SCOPE_documents:read";

    private final SpringSecurityAuthenticatedActorProvider provider =
            new SpringSecurityAuthenticatedActorProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnAnonymousActorWhenAuthenticationIsMissing() {
        AuthenticatedActor actor =
                provider.getCurrentActor();

        assertThat(actor)
                .isEqualTo(
                        AuthenticatedActor.anonymous()
                );

        assertThat(actor.isAuthenticated())
                .isFalse();

        assertThat(actor.displayName())
                .isEqualTo("anonymous");
    }

    @Test
    void shouldExtractServiceAccountFromKeycloakJwt() {
        Jwt jwt = Jwt.withTokenValue("service-token")
                .header("alg", "RS256")
                .subject("service-account-subject")
                .claim(
                        "azp",
                        "patient-document-dev-client"
                )
                .claim(
                        "preferred_username",
                        "service-account-patient-document-dev-client"
                )
                .issuedAt(ISSUED_AT)
                .expiresAt(EXPIRES_AT)
                .build();

        authenticate(jwt);

        AuthenticatedActor actor =
                provider.getCurrentActor();

        assertThat(actor.type())
                .isEqualTo(
                        ActorType.SERVICE_ACCOUNT
                );

        assertThat(actor.subject())
                .isEqualTo(
                        "service-account-subject"
                );

        assertThat(actor.clientId())
                .isEqualTo(
                        "patient-document-dev-client"
                );

        assertThat(actor.username())
                .isEqualTo(
                        "service-account-patient-document-dev-client"
                );

        assertThat(actor.isAuthenticated())
                .isTrue();

        assertThat(actor.displayName())
                .isEqualTo(
                        "service-account-patient-document-dev-client"
                );
    }

    @Test
    void shouldExtractHumanUserAndPreferClientIdClaim() {
        Jwt jwt = Jwt.withTokenValue("user-token")
                .header("alg", "RS256")
                .subject("user-subject")
                .claim(
                        "client_id",
                        "patient-portal"
                )
                .claim(
                        "azp",
                        "fallback-client"
                )
                .claim(
                        "preferred_username",
                        "alexandre"
                )
                .issuedAt(ISSUED_AT)
                .expiresAt(EXPIRES_AT)
                .build();

        authenticate(jwt);

        AuthenticatedActor actor =
                provider.getCurrentActor();

        assertThat(actor.type())
                .isEqualTo(ActorType.USER);

        assertThat(actor.subject())
                .isEqualTo("user-subject");

        assertThat(actor.clientId())
                .isEqualTo("patient-portal");

        assertThat(actor.username())
                .isEqualTo("alexandre");

        assertThat(actor.isAuthenticated())
                .isTrue();

        assertThat(actor.displayName())
                .isEqualTo("alexandre");
    }

    private void authenticate(Jwt jwt) {
        JwtAuthenticationToken authentication =
                new JwtAuthenticationToken(
                        jwt,
                        List.of(
                                new SimpleGrantedAuthority(
                                        DOCUMENTS_READ_AUTHORITY
                                )
                        )
                );

        authentication.setAuthenticated(true);

        assertThat(authentication.isAuthenticated())
                .isTrue();

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);

        SecurityContextHolder.setContext(context);
    }
}
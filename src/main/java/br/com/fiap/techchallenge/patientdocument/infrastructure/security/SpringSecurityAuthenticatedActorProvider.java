package br.com.fiap.techchallenge.patientdocument.infrastructure.security;

import br.com.fiap.techchallenge.patientdocument.application.audit.ActorType;
import br.com.fiap.techchallenge.patientdocument.application.audit.AuthenticatedActor;
import br.com.fiap.techchallenge.patientdocument.application.audit.AuthenticatedActorProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class SpringSecurityAuthenticatedActorProvider
        implements AuthenticatedActorProvider {

    private static final String CLIENT_ID_CLAIM =
            "client_id";

    private static final String AUTHORIZED_PARTY_CLAIM =
            "azp";

    private static final String PREFERRED_USERNAME_CLAIM =
            "preferred_username";

    private static final String SERVICE_ACCOUNT_PREFIX =
            "service-account-";

    @Override
    public AuthenticatedActor getCurrentActor() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)
                || !authentication.isAuthenticated()) {
            return AuthenticatedActor.anonymous();
        }

        Jwt jwt = jwtAuthentication.getToken();

        String subject = firstNonBlank(
                jwt.getSubject(),
                authentication.getName()
        );

        String clientId = firstNonBlank(
                jwt.getClaimAsString(CLIENT_ID_CLAIM),
                jwt.getClaimAsString(AUTHORIZED_PARTY_CLAIM)
        );

        String username = firstNonBlank(
                jwt.getClaimAsString(PREFERRED_USERNAME_CLAIM),
                authentication.getName()
        );

        ActorType actorType =
                isServiceAccount(username)
                        ? ActorType.SERVICE_ACCOUNT
                        : ActorType.USER;

        return AuthenticatedActor.authenticated(
                actorType,
                subject,
                clientId,
                username
        );
    }

    private boolean isServiceAccount(String username) {
        return username != null
                && username.startsWith(
                        SERVICE_ACCOUNT_PREFIX
                );
    }

    private String firstNonBlank(
            String first,
            String second
    ) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }

        if (second != null && !second.isBlank()) {
            return second.trim();
        }

        return null;
    }
}
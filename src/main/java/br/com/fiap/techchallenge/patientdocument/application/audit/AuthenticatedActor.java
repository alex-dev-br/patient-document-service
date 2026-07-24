package br.com.fiap.techchallenge.patientdocument.application.audit;

import java.util.Objects;

public record AuthenticatedActor(
        ActorType type,
        String subject,
        String clientId,
        String username
) {

    private static final String ANONYMOUS_DISPLAY_NAME =
            "anonymous";

    public AuthenticatedActor {
        type = Objects.requireNonNull(
                type,
                "O tipo do ator é obrigatório."
        );

        subject = normalize(subject);
        clientId = normalize(clientId);
        username = normalize(username);

        if (type != ActorType.ANONYMOUS && subject == null) {
            throw new IllegalArgumentException(
                    "O subject é obrigatório para atores autenticados."
            );
        }
    }

    public static AuthenticatedActor anonymous() {
        return new AuthenticatedActor(
                ActorType.ANONYMOUS,
                null,
                null,
                null
        );
    }

    public static AuthenticatedActor authenticated(
            ActorType type,
            String subject,
            String clientId,
            String username
    ) {
        if (type == ActorType.ANONYMOUS) {
            throw new IllegalArgumentException(
                    "Um ator autenticado não pode ser ANONYMOUS."
            );
        }

        return new AuthenticatedActor(
                type,
                subject,
                clientId,
                username
        );
    }

    public boolean isAuthenticated() {
        return type != ActorType.ANONYMOUS;
    }

    public String displayName() {
        if (username != null) {
            return username;
        }

        if (clientId != null) {
            return clientId;
        }

        if (subject != null) {
            return subject;
        }

        return ANONYMOUS_DISPLAY_NAME;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
package br.com.fiap.techchallenge.patientdocument.application.audit;

public interface AuthenticatedActorProvider {

    AuthenticatedActor getCurrentActor();
}
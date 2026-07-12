package br.com.fiap.techchallenge.patientdocument.application.storage.result;

public record StoredFileContent(
        byte[] content,
        long size
) {
}
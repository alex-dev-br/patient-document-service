package br.com.fiap.techchallenge.patientdocument.application.document.result;

public record HealthDocumentFile(
        String originalFileName,
        String contentType,
        long size,
        byte[] content
) {
}

package br.com.fiap.techchallenge.patientdocument.application.storage.result;

public record StoredFile(
        String originalFileName,
        String storedFileName,
        String storagePath,
        String contentType,
        Long fileSize
) {
}

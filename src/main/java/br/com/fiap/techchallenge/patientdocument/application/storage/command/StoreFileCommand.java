package br.com.fiap.techchallenge.patientdocument.application.storage.command;

import java.io.InputStream;

public record StoreFileCommand(
        String originalFileName,
        String contentType,
        Long fileSize,
        InputStream inputStream
) {
}

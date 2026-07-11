package br.com.fiap.techchallenge.patientdocument.application.document.command;

import java.io.InputStream;
import java.util.UUID;

public record UploadHealthDocumentCommand(
        UUID patientId,
        String originalFileName,
        String contentType,
        Long fileSize,
        InputStream inputStream
) {
}

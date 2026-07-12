package br.com.fiap.techchallenge.patientdocument.infrastructure.web.document;

import br.com.fiap.techchallenge.patientdocument.application.document.result.HealthDocumentFile;
import br.com.fiap.techchallenge.patientdocument.application.document.usecase.DownloadHealthDocumentFileUseCase;
import br.com.fiap.techchallenge.patientdocument.application.document.usecase.FindHealthDocumentByIdUseCase;
import br.com.fiap.techchallenge.patientdocument.application.document.usecase.UpdateDocumentAiResultUseCase;
import br.com.fiap.techchallenge.patientdocument.domain.document.HealthDocument;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DocumentController {

    private final FindHealthDocumentByIdUseCase findHealthDocumentByIdUseCase;
    private final UpdateDocumentAiResultUseCase updateDocumentAiResultUseCase;
    private final DownloadHealthDocumentFileUseCase downloadHealthDocumentFileUseCase;
    private final HealthDocumentWebMapper healthDocumentWebMapper;

    @GetMapping("/documents/{documentId}")
    public ResponseEntity<HealthDocumentResponse> findById(@PathVariable UUID documentId) {
        HealthDocument document = findHealthDocumentByIdUseCase.execute(documentId);
        return ResponseEntity.ok(healthDocumentWebMapper.toResponse(document));
    }

    @GetMapping("/documents/{documentId}/file")
    public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable UUID documentId) {
        HealthDocumentFile file = downloadHealthDocumentFileUseCase.execute(documentId);

        ByteArrayResource resource = new ByteArrayResource(file.content());

        MediaType mediaType = file.contentType() == null || file.contentType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(file.contentType());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(file.size())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(file.originalFileName(), StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .body(resource);
    }

    @PatchMapping("/documents/{documentId}/ai-result")
    public ResponseEntity<HealthDocumentResponse> updateAiResult(
            @PathVariable UUID documentId,
            @RequestBody @Valid AiResultRequest request
    ) {
        HealthDocument document = updateDocumentAiResultUseCase.execute(
                healthDocumentWebMapper.toCommand(documentId, request)
        );

        return ResponseEntity.ok(healthDocumentWebMapper.toResponse(document));
    }
}

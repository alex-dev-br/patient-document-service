package br.com.fiap.techchallenge.patientdocument.infrastructure.web.document;

import br.com.fiap.techchallenge.patientdocument.application.document.usecase.FindHealthDocumentByIdUseCase;
import br.com.fiap.techchallenge.patientdocument.application.document.usecase.UpdateDocumentAiResultUseCase;
import br.com.fiap.techchallenge.patientdocument.domain.document.HealthDocument;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DocumentController {

    private final FindHealthDocumentByIdUseCase findHealthDocumentByIdUseCase;
    private final UpdateDocumentAiResultUseCase updateDocumentAiResultUseCase;
    private final HealthDocumentWebMapper healthDocumentWebMapper;

    @GetMapping("/documents/{documentId}")
    public ResponseEntity<HealthDocumentResponse> findById(@PathVariable UUID documentId) {
        HealthDocument document = findHealthDocumentByIdUseCase.execute(documentId);
        return ResponseEntity.ok(healthDocumentWebMapper.toResponse(document));
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

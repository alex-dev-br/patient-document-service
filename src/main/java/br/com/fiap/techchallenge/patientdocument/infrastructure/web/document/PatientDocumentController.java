package br.com.fiap.techchallenge.patientdocument.infrastructure.web.document;

import br.com.fiap.techchallenge.patientdocument.application.document.command.UploadHealthDocumentCommand;
import br.com.fiap.techchallenge.patientdocument.application.document.usecase.GetPatientTimelineUseCase;
import br.com.fiap.techchallenge.patientdocument.application.document.usecase.ListPatientDocumentsUseCase;
import br.com.fiap.techchallenge.patientdocument.application.document.usecase.UploadHealthDocumentUseCase;
import br.com.fiap.techchallenge.patientdocument.application.exception.StorageException;
import br.com.fiap.techchallenge.patientdocument.domain.document.HealthDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PatientDocumentController {

    private final UploadHealthDocumentUseCase uploadHealthDocumentUseCase;
    private final ListPatientDocumentsUseCase listPatientDocumentsUseCase;
    private final GetPatientTimelineUseCase getPatientTimelineUseCase;
    private final HealthDocumentWebMapper healthDocumentWebMapper;

    @PostMapping(
            value = "/patients/{patientId}/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<HealthDocumentResponse> upload(
            @PathVariable UUID patientId,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            HealthDocument document = uploadHealthDocumentUseCase.execute(new UploadHealthDocumentCommand(
                    patientId,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    file.getInputStream()
            ));

            HealthDocumentResponse response = healthDocumentWebMapper.toResponse(document);

            URI location = URI.create("/documents/" + response.id());

            return ResponseEntity
                    .created(location)
                    .body(response);
        } catch (IOException exception) {
            throw new StorageException("Não foi possível ler o arquivo enviado.", exception);
        }
    }

    @GetMapping("/patients/{patientId}/documents")
    public ResponseEntity<List<HealthDocumentResponse>> listByPatient(@PathVariable UUID patientId) {
        List<HealthDocumentResponse> response = listPatientDocumentsUseCase.execute(patientId)
                .stream()
                .map(healthDocumentWebMapper::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/patients/{patientId}/timeline")
    public ResponseEntity<List<PatientTimelineItemResponse>> timeline(@PathVariable UUID patientId) {
        List<PatientTimelineItemResponse> response = getPatientTimelineUseCase.execute(patientId)
                .stream()
                .map(healthDocumentWebMapper::toTimelineItemResponse)
                .toList();

        return ResponseEntity.ok(response);
    }
}

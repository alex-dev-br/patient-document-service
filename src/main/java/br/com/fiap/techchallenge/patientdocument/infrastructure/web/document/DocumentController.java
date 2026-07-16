package br.com.fiap.techchallenge.patientdocument.infrastructure.web.document;

import br.com.fiap.techchallenge.patientdocument.application.document.result.HealthDocumentFile;
import br.com.fiap.techchallenge.patientdocument.application.document.usecase.DownloadHealthDocumentFileUseCase;
import br.com.fiap.techchallenge.patientdocument.application.document.usecase.FindHealthDocumentByIdUseCase;
import br.com.fiap.techchallenge.patientdocument.application.document.usecase.ListProcessedDocumentResultsUseCase;
import br.com.fiap.techchallenge.patientdocument.application.document.usecase.UpdateDocumentAiResultUseCase;
import br.com.fiap.techchallenge.patientdocument.domain.document.HealthDocument;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(
        name = "Documentos",
        description = "Consulta, download e atualização de documentos individuais"
)
public class DocumentController {

    private final FindHealthDocumentByIdUseCase findHealthDocumentByIdUseCase;
    private final UpdateDocumentAiResultUseCase updateDocumentAiResultUseCase;
    private final DownloadHealthDocumentFileUseCase downloadHealthDocumentFileUseCase;
    private final HealthDocumentWebMapper healthDocumentWebMapper;
    private final ListProcessedDocumentResultsUseCase listProcessedDocumentResultsUseCase;
    private final ProcessedDocumentResultWebMapper processedDocumentResultWebMapper;

    @GetMapping(
            value = "/documents/{documentId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            operationId = "findDocumentById",
            summary = "Consultar documento por ID",
            description = "Retorna os metadados e o resultado de processamento de um documento."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Documento encontrado"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Documento não encontrado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    public ResponseEntity<HealthDocumentResponse> findById(@PathVariable UUID documentId) {
        HealthDocument document = findHealthDocumentByIdUseCase.execute(documentId);
        return ResponseEntity.ok(healthDocumentWebMapper.toResponse(document));
    }

    @GetMapping("/documents/{documentId}/file")
    @Operation(
            operationId = "downloadDocumentFile",
            summary = "Baixar arquivo original",
            description = "Baixa o arquivo original associado ao documento."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Arquivo retornado com sucesso",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                            schema = @Schema(type = "string", format = "binary")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Documento não encontrado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro ao ler o arquivo armazenado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
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

    @GetMapping(
            value = "/documents/{documentId}/processed-results",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            operationId = "listDocumentProcessedResults",
            summary = "Listar resultados processados pela IA",
            description = """
                Retorna todos os resultados extraídos pela inteligência artificial
                para o documento original. Um mesmo arquivo pode gerar mais de um
                resultado clínico.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Resultados retornados com sucesso"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Documento não encontrado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    public ResponseEntity<List<ProcessedDocumentResultResponse>>
    listProcessedResults(@PathVariable UUID documentId) {

        List<ProcessedDocumentResultResponse> response =
                listProcessedDocumentResultsUseCase
                        .execute(documentId)
                        .stream()
                        .map(processedDocumentResultWebMapper::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    @PatchMapping(
            value = "/documents/{documentId}/ai-result",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            operationId = "updateDocumentAiResult",
            summary = "Atualizar resultado da IA",
            description = """
                Atualiza o tipo, especialidade, data, resumo, palavras-chave,
                confiança e status de processamento do documento.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Resultado atualizado com sucesso"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Resultado de processamento inválido",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Documento não encontrado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
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

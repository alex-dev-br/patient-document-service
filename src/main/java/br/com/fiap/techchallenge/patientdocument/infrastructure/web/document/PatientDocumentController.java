package br.com.fiap.techchallenge.patientdocument.infrastructure.web.document;

import br.com.fiap.techchallenge.patientdocument.application.common.pagination.PageQuery;
import br.com.fiap.techchallenge.patientdocument.application.common.pagination.PagedResult;
import br.com.fiap.techchallenge.patientdocument.application.document.command.UploadHealthDocumentCommand;
import br.com.fiap.techchallenge.patientdocument.application.document.query.HealthDocumentFilter;
import br.com.fiap.techchallenge.patientdocument.application.document.usecase.GetPatientTimelineUseCase;
import br.com.fiap.techchallenge.patientdocument.application.document.usecase.ListPatientDocumentsUseCase;
import br.com.fiap.techchallenge.patientdocument.application.document.usecase.UploadHealthDocumentUseCase;
import br.com.fiap.techchallenge.patientdocument.application.exception.StorageException;
import br.com.fiap.techchallenge.patientdocument.domain.document.HealthDocument;
import br.com.fiap.techchallenge.patientdocument.infrastructure.web.common.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(
        name = "Documentos do paciente",
        description = "Upload, filtros e timeline dos documentos de um paciente"
)
public class PatientDocumentController {

    private final UploadHealthDocumentUseCase uploadHealthDocumentUseCase;
    private final ListPatientDocumentsUseCase listPatientDocumentsUseCase;
    private final GetPatientTimelineUseCase getPatientTimelineUseCase;
    private final HealthDocumentWebMapper healthDocumentWebMapper;

    @PostMapping(
            value = "/patients/{patientId}/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            operationId = "uploadPatientDocument",
            summary = "Enviar documento de saúde",
            description = """
                Envia um documento para o paciente, salva o arquivo no armazenamento
                local e registra os metadados com status PENDING_PROCESSING.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Documento enviado com sucesso"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Arquivo inválido ou vazio",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Paciente não encontrado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro no armazenamento do arquivo",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    public ResponseEntity<HealthDocumentResponse> upload(
            @PathVariable UUID patientId,
            @Parameter(
                    description = "Arquivo de saúde que será associado ao paciente",
                    required = true
            )
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

    @GetMapping(
            value = "/patients/{patientId}/documents",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            operationId = "listPatientDocuments",
            summary = "Listar documentos do paciente",
            description = """
                Retorna os documentos do paciente de forma paginada, permitindo
                filtros por tipo, especialidade, status, palavra-chave e período.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Documentos retornados com sucesso"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Filtros ou parâmetros de paginação inválidos",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Paciente não encontrado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    public ResponseEntity<PagedResponse<HealthDocumentResponse>> listByPatient(
            @PathVariable UUID patientId,

            @Parameter(
                    description = "Tipo do documento",
                    example = "EXAME_LABORATORIAL"
            )
            @RequestParam(required = false) String documentType,

            @Parameter(
                    description = "Especialidade médica",
                    example = "ENDOCRINOLOGIA"
            )
            @RequestParam(required = false) String specialty,

            @Parameter(
                    description = "Status de processamento",
                    example = "PROCESSED"
            )
            @RequestParam(required = false) String status,

            @Parameter(
                    description = "Palavra-chave associada ao documento",
                    example = "glicemia"
            )
            @RequestParam(required = false) String keyword,

            @Parameter(
                    description = "Data inicial do documento",
                    example = "2026-01-01"
            )
            @RequestParam(required = false) LocalDate startDate,

            @Parameter(
                    description = "Data final do documento",
                    example = "2026-12-31"
            )
            @RequestParam(required = false) LocalDate endDate,

            @Parameter(
                    description = "Número da página, começando em zero",
                    example = "0",
                    schema = @Schema(
                            type = "integer",
                            format = "int32",
                            minimum = "0",
                            defaultValue = "0"
                    )
            )
            @RequestParam(defaultValue = "0") int page,

            @Parameter(
                    description = "Quantidade de documentos por página",
                    example = "10",
                    schema = @Schema(
                            type = "integer",
                            format = "int32",
                            minimum = "1",
                            maximum = "100",
                            defaultValue = "10"
                    )
            )
            @RequestParam(defaultValue = "10") int size
    ) {
        HealthDocumentFilter filter = healthDocumentWebMapper.toFilter(
                documentType,
                specialty,
                status,
                keyword,
                startDate,
                endDate
        );

        PagedResult<HealthDocument> result =
                listPatientDocumentsUseCase.execute(
                        patientId,
                        filter,
                        new PageQuery(page, size)
                );

        List<HealthDocumentResponse> content = result.content()
                .stream()
                .map(healthDocumentWebMapper::toResponse)
                .toList();

        PagedResponse<HealthDocumentResponse> response =
                new PagedResponse<>(
                        content,
                        result.page(),
                        result.size(),
                        result.totalElements(),
                        result.totalPages(),
                        result.first(),
                        result.last()
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping(
            value = "/patients/{patientId}/timeline",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            operationId = "getPatientTimeline",
            summary = "Consultar timeline do paciente",
            description = """
                    Retorna a timeline paginada dos documentos do paciente.
                    A consulta aceita os mesmos filtros da listagem de documentos.
                    Os registros são ordenados pela data do documento em ordem
                    decrescente. Documentos sem essa data são organizados pela
                    data de criação.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Timeline retornada com sucesso"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Filtro ou paginação inválidos",
                    content = @Content(
                            mediaType =
                                    MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(
                                    implementation = ProblemDetail.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Paciente não encontrado",
                    content = @Content(
                            mediaType =
                                    MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(
                                    implementation = ProblemDetail.class
                            )
                    )
            )
    })
    public ResponseEntity<
            PagedResponse<PatientTimelineItemResponse>
            > timeline(
            @PathVariable UUID patientId,

            @Parameter(
                    description = "Tipo do documento",
                    example = "EXAME_LABORATORIAL"
            )
            @RequestParam(required = false)
            String documentType,

            @Parameter(
                    description = "Especialidade médica",
                    example = "ENDOCRINOLOGIA"
            )
            @RequestParam(required = false)
            String specialty,

            @Parameter(
                    description = "Status de processamento",
                    example = "PROCESSED"
            )
            @RequestParam(required = false)
            String status,

            @Parameter(
                    description =
                            "Palavra-chave associada ao documento",
                    example = "glicemia"
            )
            @RequestParam(required = false)
            String keyword,

            @Parameter(
                    description = "Data inicial do documento",
                    example = "2026-01-01"
            )
            @RequestParam(required = false)
            LocalDate startDate,

            @Parameter(
                    description = "Data final do documento",
                    example = "2026-12-31"
            )
            @RequestParam(required = false)
            LocalDate endDate,

            @Parameter(
                    description =
                            "Número da página, começando em zero",
                    example = "0",
                    schema = @Schema(
                            type = "integer",
                            format = "int32",
                            minimum = "0",
                            defaultValue = "0"
                    )
            )
            @RequestParam(defaultValue = "0")
            int page,

            @Parameter(
                    description =
                            "Quantidade de documentos por página",
                    example = "10",
                    schema = @Schema(
                            type = "integer",
                            format = "int32",
                            minimum = "1",
                            maximum = "100",
                            defaultValue = "10"
                    )
            )
            @RequestParam(defaultValue = "10")
            int size
    ) {
        HealthDocumentFilter filter =
                healthDocumentWebMapper.toFilter(
                        documentType,
                        specialty,
                        status,
                        keyword,
                        startDate,
                        endDate
                );

        PagedResult<HealthDocument> result =
                getPatientTimelineUseCase.execute(
                        patientId,
                        filter,
                        new PageQuery(
                                page,
                                size
                        )
                );

        List<PatientTimelineItemResponse> content =
                result.content()
                        .stream()
                        .map(
                                healthDocumentWebMapper
                                        ::toTimelineItemResponse
                        )
                        .toList();

        PagedResponse<PatientTimelineItemResponse> response =
                new PagedResponse<>(
                        content,
                        result.page(),
                        result.size(),
                        result.totalElements(),
                        result.totalPages(),
                        result.first(),
                        result.last()
                );

        return ResponseEntity.ok(response);
    }
}

package br.com.fiap.techchallenge.patientdocument.infrastructure.web.patient;

import br.com.fiap.techchallenge.patientdocument.application.patient.usecase.CreatePatientUseCase;
import br.com.fiap.techchallenge.patientdocument.application.patient.usecase.FindPatientByIdUseCase;
import br.com.fiap.techchallenge.patientdocument.application.patient.usecase.ListPatientsUseCase;
import br.com.fiap.techchallenge.patientdocument.domain.patient.Patient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(
        value = "/patients",
        produces = MediaType.APPLICATION_JSON_VALUE
)
@RequiredArgsConstructor
@Tag(
        name = "Pacientes",
        description = "Operações de cadastro e consulta de pacientes"
)
public class PatientController {

    private final CreatePatientUseCase createPatientUseCase;
    private final FindPatientByIdUseCase findPatientByIdUseCase;
    private final ListPatientsUseCase listPatientsUseCase;
    private final PatientWebMapper patientWebMapper;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "createPatient",
            summary = "Cadastrar paciente",
            description = "Cadastra um novo paciente no sistema."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Paciente cadastrado com sucesso"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados do paciente inválidos",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    public ResponseEntity<PatientResponse> create(@RequestBody @Valid CreatePatientRequest request) {
        Patient patient = createPatientUseCase.execute(patientWebMapper.toCommand(request));
        PatientResponse response = patientWebMapper.toResponse(patient);

        URI location = URI.create("/patients/" + response.id());

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @GetMapping("/{id}")
    @Operation(
            operationId = "findPatientById",
            summary = "Consultar paciente por ID",
            description = "Retorna os dados do paciente correspondente ao identificador informado."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Paciente encontrado"
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
    public ResponseEntity<PatientResponse> findById(@PathVariable UUID id) {
        Patient patient = findPatientByIdUseCase.execute(id);
        return ResponseEntity.ok(patientWebMapper.toResponse(patient));
    }

    @GetMapping
    @Operation(
            operationId = "listPatients",
            summary = "Listar pacientes",
            description = "Retorna todos os pacientes cadastrados."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Pacientes retornados com sucesso"
    )
    public ResponseEntity<List<PatientResponse>> findAll() {
        List<PatientResponse> response = listPatientsUseCase.execute()
                .stream()
                .map(patientWebMapper::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }
}

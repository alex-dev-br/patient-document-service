package br.com.fiap.techchallenge.patientdocument.infrastructure.web.patient;

import br.com.fiap.techchallenge.patientdocument.application.patient.usecase.CreatePatientUseCase;
import br.com.fiap.techchallenge.patientdocument.application.patient.usecase.FindPatientByIdUseCase;
import br.com.fiap.techchallenge.patientdocument.application.patient.usecase.ListPatientsUseCase;
import br.com.fiap.techchallenge.patientdocument.domain.patient.Patient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
public class PatientController {

    private final CreatePatientUseCase createPatientUseCase;
    private final FindPatientByIdUseCase findPatientByIdUseCase;
    private final ListPatientsUseCase listPatientsUseCase;
    private final PatientWebMapper patientWebMapper;

    @PostMapping
    public ResponseEntity<PatientResponse> create(@RequestBody @Valid CreatePatientRequest request) {
        Patient patient = createPatientUseCase.execute(patientWebMapper.toCommand(request));
        PatientResponse response = patientWebMapper.toResponse(patient);

        URI location = URI.create("/patients/" + response.id());

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatientResponse> findById(@PathVariable UUID id) {
        Patient patient = findPatientByIdUseCase.execute(id);
        return ResponseEntity.ok(patientWebMapper.toResponse(patient));
    }

    @GetMapping
    public ResponseEntity<List<PatientResponse>> findAll() {
        List<PatientResponse> response = listPatientsUseCase.execute()
                .stream()
                .map(patientWebMapper::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }
}

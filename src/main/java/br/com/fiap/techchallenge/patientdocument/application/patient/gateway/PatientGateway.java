package br.com.fiap.techchallenge.patientdocument.application.patient.gateway;

import br.com.fiap.techchallenge.patientdocument.domain.patient.Patient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientGateway {

    Patient save(Patient patient);

    Optional<Patient> findById(UUID id);

    List<Patient> findAll();
}

package br.com.fiap.techchallenge.patientdocument.application.document.usecase;

import br.com.fiap.techchallenge.patientdocument.domain.document.HealthDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetPatientTimelineUseCase {

    private final ListPatientDocumentsUseCase listPatientDocumentsUseCase;

    @Transactional(readOnly = true)
    public List<HealthDocument> execute(UUID patientId) {
        return listPatientDocumentsUseCase.execute(patientId)
                .stream()
                .sorted(Comparator.comparing(HealthDocument::getCreatedAt).reversed())
                .toList();
    }
}

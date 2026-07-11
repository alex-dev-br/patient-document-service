package br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.document;

import br.com.fiap.techchallenge.patientdocument.application.document.gateway.HealthDocumentGateway;
import br.com.fiap.techchallenge.patientdocument.domain.document.HealthDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class HealthDocumentGatewayImpl implements HealthDocumentGateway {

    private final HealthDocumentJpaRepository healthDocumentJpaRepository;
    private final DocumentKeywordJpaRepository documentKeywordJpaRepository;
    private final HealthDocumentPersistenceMapper healthDocumentPersistenceMapper;

    @Override
    public HealthDocument save(HealthDocument document) {
        HealthDocumentJpaEntity entity = healthDocumentPersistenceMapper.toEntity(document);
        HealthDocumentJpaEntity savedEntity = healthDocumentJpaRepository.save(entity);

        documentKeywordJpaRepository.deleteByDocumentId(savedEntity.getId());

        List<DocumentKeywordJpaEntity> keywordEntities = document.getKeywords()
                .stream()
                .filter(keyword -> keyword != null && !keyword.isBlank())
                .distinct()
                .map(keyword -> DocumentKeywordJpaEntity.builder()
                        .id(UUID.randomUUID())
                        .documentId(savedEntity.getId())
                        .keyword(keyword)
                        .build())
                .toList();

        if (!keywordEntities.isEmpty()) {
            documentKeywordJpaRepository.saveAll(keywordEntities);
        }

        return healthDocumentPersistenceMapper.toDomain(
                savedEntity,
                keywordEntities.stream()
                        .map(DocumentKeywordJpaEntity::getKeyword)
                        .toList()
        );
    }

    @Override
    public Optional<HealthDocument> findById(UUID id) {
        return healthDocumentJpaRepository.findById(id)
                .map(entity -> {
                    List<String> keywords = documentKeywordJpaRepository.findByDocumentId(entity.getId())
                            .stream()
                            .map(DocumentKeywordJpaEntity::getKeyword)
                            .toList();

                    return healthDocumentPersistenceMapper.toDomain(entity, keywords);
                });
    }

    @Override
    public List<HealthDocument> findByPatientId(UUID patientId) {
        List<HealthDocumentJpaEntity> documents = healthDocumentJpaRepository.findByPatientIdOrderByCreatedAtDesc(patientId);

        List<UUID> documentIds = documents.stream()
                .map(HealthDocumentJpaEntity::getId)
                .toList();

        Map<UUID, List<String>> keywordsByDocumentId = documentKeywordJpaRepository.findByDocumentIdIn(documentIds)
                .stream()
                .collect(Collectors.groupingBy(
                        DocumentKeywordJpaEntity::getDocumentId,
                        Collectors.mapping(DocumentKeywordJpaEntity::getKeyword, Collectors.toList())
                ));

        return documents.stream()
                .map(entity -> healthDocumentPersistenceMapper.toDomain(
                        entity,
                        keywordsByDocumentId.getOrDefault(entity.getId(), List.of())
                ))
                .toList();
    }
}

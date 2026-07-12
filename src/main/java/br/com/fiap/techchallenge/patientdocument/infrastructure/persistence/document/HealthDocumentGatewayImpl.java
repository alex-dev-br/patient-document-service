package br.com.fiap.techchallenge.patientdocument.infrastructure.persistence.document;

import br.com.fiap.techchallenge.patientdocument.application.common.pagination.PageQuery;
import br.com.fiap.techchallenge.patientdocument.application.common.pagination.PagedResult;
import br.com.fiap.techchallenge.patientdocument.application.document.gateway.HealthDocumentGateway;
import br.com.fiap.techchallenge.patientdocument.application.document.query.HealthDocumentFilter;
import br.com.fiap.techchallenge.patientdocument.domain.document.HealthDocument;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    public PagedResult<HealthDocument> findByPatientId(
            UUID patientId,
            HealthDocumentFilter filter,
            PageQuery pageQuery
    ) {
        Specification<HealthDocumentJpaEntity> specification =
                buildSpecification(patientId, filter);

        PageRequest pageable = PageRequest.of(
                pageQuery.page(),
                pageQuery.size(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<HealthDocumentJpaEntity> entityPage =
                healthDocumentJpaRepository.findAll(
                        specification,
                        pageable
                );

        List<HealthDocument> content =
                toDomainList(entityPage.getContent());

        return new PagedResult<>(
                content,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages(),
                entityPage.isFirst(),
                entityPage.isLast()
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
        return findByPatientId(patientId, HealthDocumentFilter.empty());
    }

    @Override
    public List<HealthDocument> findByPatientId(UUID patientId, HealthDocumentFilter filter) {
        Specification<HealthDocumentJpaEntity> specification = buildSpecification(patientId, filter);

        List<HealthDocumentJpaEntity> entities = healthDocumentJpaRepository.findAll(
                specification,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return toDomainList(entities);
    }

    private Specification<HealthDocumentJpaEntity> buildSpecification(
            UUID patientId,
            HealthDocumentFilter filter
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("patientId"), patientId));

            if (filter.documentType() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("documentType"),
                        filter.documentType().name()
                ));
            }

            if (filter.specialty() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("specialty"),
                        filter.specialty().name()
                ));
            }

            if (filter.status() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("processingStatus"),
                        filter.status().name()
                ));
            }

            if (filter.startDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("documentDate"),
                        filter.startDate()
                ));
            }

            if (filter.endDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("documentDate"),
                        filter.endDate()
                ));
            }

            if (filter.keyword() != null) {
                Subquery<UUID> subquery = query.subquery(UUID.class);
                Root<DocumentKeywordJpaEntity> keywordRoot = subquery.from(DocumentKeywordJpaEntity.class);

                subquery.select(keywordRoot.get("documentId"))
                        .where(
                                criteriaBuilder.equal(keywordRoot.get("documentId"), root.get("id")),
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(keywordRoot.get("keyword")),
                                        "%" + filter.keyword().toLowerCase() + "%"
                                )
                        );

                predicates.add(criteriaBuilder.exists(subquery));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private List<HealthDocument> toDomainList(List<HealthDocumentJpaEntity> entities) {
        List<UUID> documentIds = entities.stream()
                .map(HealthDocumentJpaEntity::getId)
                .toList();

        if (documentIds.isEmpty()) {
            return List.of();
        }

        List<DocumentKeywordJpaEntity> keywords = documentKeywordJpaRepository.findByDocumentIdIn(documentIds);

        var keywordsByDocumentId = keywords.stream()
                .collect(java.util.stream.Collectors.groupingBy(DocumentKeywordJpaEntity::getDocumentId));

        return entities.stream()
                .map(entity -> {
                    List<String> documentKeywords = keywordsByDocumentId
                            .getOrDefault(entity.getId(), List.of())
                            .stream()
                            .map(DocumentKeywordJpaEntity::getKeyword)
                            .toList();

                    return healthDocumentPersistenceMapper.toDomain(entity, documentKeywords);
                })
                .toList();
    }
}

package br.com.fiap.techchallenge.patientdocument.application.common.pagination;

import java.util.List;

public record PagedResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public PagedResult {
        content = content == null
                ? List.of()
                : List.copyOf(content);
    }
}

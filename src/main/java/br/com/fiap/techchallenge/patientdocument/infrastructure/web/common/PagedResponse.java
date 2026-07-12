package br.com.fiap.techchallenge.patientdocument.infrastructure.web.common;

import java.util.List;

public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public PagedResponse {
        content = content == null
                ? List.of()
                : List.copyOf(content);
    }
}

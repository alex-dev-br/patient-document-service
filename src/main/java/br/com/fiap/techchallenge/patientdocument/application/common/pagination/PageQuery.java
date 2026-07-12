package br.com.fiap.techchallenge.patientdocument.application.common.pagination;

public record PageQuery(
        int page,
        int size
) {

    public static final int MAX_SIZE = 100;

    public PageQuery {
        if (page < 0) {
            throw new IllegalArgumentException(
                    "O número da página não pode ser negativo."
            );
        }

        if (size < 1) {
            throw new IllegalArgumentException(
                    "O tamanho da página deve ser maior que zero."
            );
        }

        if (size > MAX_SIZE) {
            throw new IllegalArgumentException(
                    "O tamanho máximo permitido para a página é " + MAX_SIZE + "."
            );
        }
    }
}

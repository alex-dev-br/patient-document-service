package br.com.fiap.techchallenge.patientdocument.infrastructure.web.exception;

import br.com.fiap.techchallenge.patientdocument.application.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Recurso não encontrado");
        problemDetail.setDetail(exception.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Erro de validação");
        problemDetail.setDetail("Um ou mais campos estão inválidos.");
        return problemDetail;
    }
}

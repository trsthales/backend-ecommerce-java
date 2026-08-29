package com.trsthales.ecommerce.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler Tests (RFC 7807)")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Should handle ResourceNotFoundException with 404 ProblemDetail")
    void shouldHandleResourceNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Produto 123 não encontrado");

        ResponseEntity<ProblemDetail> response = handler.handleResourceNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getTitle()).isEqualTo("Resource Not Found");
        assertThat(response.getBody().getDetail()).isEqualTo("Produto 123 não encontrado");
        assertThat(response.getBody().getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("Should handle BusinessRuleViolationException with 422 ProblemDetail")
    void shouldHandleBusinessRuleViolation() {
        BusinessRuleViolationException ex = new BusinessRuleViolationException("Saldo insuficiente de estoque");

        ResponseEntity<ProblemDetail> response = handler.handleBusinessRuleViolation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(422);
        assertThat(response.getBody().getTitle()).isEqualTo("Business Rule Violation");
        assertThat(response.getBody().getDetail()).isEqualTo("Saldo insuficiente de estoque");
        assertThat(response.getBody().getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("Should handle IdempotencyConflictException with 409 ProblemDetail")
    void shouldHandleIdempotencyConflict() {
        IdempotencyConflictException ex = new IdempotencyConflictException("Requisição concorrente em processamento");

        ResponseEntity<ProblemDetail> response = handler.handleIdempotencyConflict(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getTitle()).isEqualTo("Idempotency Conflict");
        assertThat(response.getBody().getDetail()).isEqualTo("Requisição concorrente em processamento");
        assertThat(response.getBody().getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException with 400 ProblemDetail")
    void shouldHandleIllegalArgument() {
        IllegalArgumentException ex = new IllegalArgumentException("Parâmetro inválido");

        ResponseEntity<ProblemDetail> response = handler.handleIllegalArgument(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getTitle()).isEqualTo("Invalid Request");
        assertThat(response.getBody().getDetail()).isEqualTo("Parâmetro inválido");
        assertThat(response.getBody().getProperties()).containsKey("timestamp");
    }
}

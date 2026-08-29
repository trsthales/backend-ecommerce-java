package com.trsthales.ecommerce.common.exception;

public class IdempotencyConflictException extends DomainException {

    public IdempotencyConflictException(String message) {
        super(message);
    }
}

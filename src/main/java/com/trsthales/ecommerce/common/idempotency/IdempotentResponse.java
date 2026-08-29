package com.trsthales.ecommerce.common.idempotency;

public record IdempotentResponse(
        int statusCode,
        String body
) {
}

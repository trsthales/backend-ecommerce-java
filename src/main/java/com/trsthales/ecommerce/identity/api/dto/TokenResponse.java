package com.trsthales.ecommerce.identity.api.dto;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {
}

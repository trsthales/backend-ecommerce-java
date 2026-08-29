package com.trsthales.ecommerce.identity.api.dto;

import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        Set<String> roles
) {
}

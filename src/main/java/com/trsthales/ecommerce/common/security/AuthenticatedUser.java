package com.trsthales.ecommerce.common.security;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record AuthenticatedUser(
        UUID userId,
        String email,
        Set<String> roles
) implements Serializable {

    public AuthenticatedUser {
        Objects.requireNonNull(userId, "userId é obrigatório");
        Objects.requireNonNull(email, "email é obrigatório");
        roles = (roles != null) ? Collections.unmodifiableSet(new HashSet<>(roles)) : Collections.emptySet();
    }

    public boolean hasRole(String role) {
        if (role == null) {
            return false;
        }
        return roles.contains(role);
    }
}

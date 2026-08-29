package com.trsthales.ecommerce.common.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class SecurityContextCurrentUserProvider implements CurrentUserProvider {

    @Override
    public Optional<AuthenticatedUser> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof AuthenticatedUser user) {
            return Optional.of(user);
        }

        if (principal instanceof Jwt jwt) {
            UUID userId = extractUserId(jwt);
            String email = extractEmail(jwt);
            Set<String> roles = extractRoles(authentication, jwt);
            return Optional.of(new AuthenticatedUser(userId, email, roles));
        }

        return Optional.empty();
    }

    private UUID extractUserId(Jwt jwt) {
        Object userIdClaim = jwt.getClaim("user_id");
        if (userIdClaim != null) {
            return UUID.fromString(userIdClaim.toString());
        }
        Object sub = jwt.getSubject();
        if (sub != null) {
            try {
                return UUID.fromString(sub.toString());
            } catch (IllegalArgumentException ignored) {
            }
        }
        throw new IllegalStateException("JWT não contém claim 'user_id' ou 'sub' no formato UUID válido");
    }

    private String extractEmail(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email != null && !email.isBlank()) {
            return email;
        }
        return jwt.getSubject();
    }

    private Set<String> extractRoles(Authentication authentication, Jwt jwt) {
        if (authentication.getAuthorities() != null && !authentication.getAuthorities().isEmpty()) {
            return authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());
        }
        Object rolesClaim = jwt.getClaim("roles");
        if (rolesClaim instanceof Iterable<?> iterable) {
            Set<String> roles = new java.util.HashSet<>();
            for (Object item : iterable) {
                if (item != null) {
                    roles.add(item.toString());
                }
            }
            return roles;
        }
        return Collections.emptySet();
    }
}

package com.trsthales.ecommerce.identity.infrastructure;

import com.trsthales.ecommerce.common.security.AuthenticatedUser;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UUID userId = extractUserId(jwt);
        String email = extractEmail(jwt);
        Set<String> roles = extractRoles(jwt);

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(userId, email, roles);

        Collection<GrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());

        return new UsernamePasswordAuthenticationToken(authenticatedUser, jwt, authorities);
    }

    private UUID extractUserId(Jwt jwt) {
        Object userIdClaim = jwt.getClaim("user_id");
        if (userIdClaim != null) {
            return UUID.fromString(userIdClaim.toString());
        }
        return UUID.fromString(jwt.getSubject());
    }

    private String extractEmail(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email != null && !email.isBlank()) {
            return email;
        }
        return jwt.getSubject();
    }

    private Set<String> extractRoles(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null) {
            return new HashSet<>(roles);
        }
        return Collections.emptySet();
    }
}

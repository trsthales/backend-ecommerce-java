package com.trsthales.ecommerce.common.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SecurityContextCurrentUserProvider Tests (INV-006)")
class SecurityContextCurrentUserProviderTest {

    private final SecurityContextCurrentUserProvider provider = new SecurityContextCurrentUserProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should return empty when no authentication present")
    void shouldReturnEmptyWhenNoAuth() {
        SecurityContextHolder.clearContext();

        Optional<AuthenticatedUser> user = provider.getCurrentUser();

        assertThat(user).isEmpty();
        assertThatThrownBy(provider::getRequiredCurrentUser)
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    @DisplayName("Should return empty for AnonymousAuthenticationToken")
    void shouldReturnEmptyForAnonymous() {
        AnonymousAuthenticationToken anonymous = new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        SecurityContextHolder.getContext().setAuthentication(anonymous);

        assertThat(provider.getCurrentUser()).isEmpty();
    }

    @Test
    @DisplayName("Should resolve when principal is already AuthenticatedUser")
    void shouldResolveAuthenticatedUserPrincipal() {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser expected = new AuthenticatedUser(userId, "user@example.com", Set.of("ROLE_CUSTOMER"));
        TestingAuthenticationToken auth = new TestingAuthenticationToken(expected, null);
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<AuthenticatedUser> result = provider.getCurrentUser();

        assertThat(result).contains(expected);
        assertThat(result.get().hasRole("ROLE_CUSTOMER")).isTrue();
        assertThat(result.get().hasRole("ROLE_ADMIN")).isFalse();
    }

    @Test
    @DisplayName("Should extract AuthenticatedUser from Jwt principal")
    void shouldExtractFromJwt() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of(
                        "user_id", userId.toString(),
                        "email", "customer@ecommerce.com",
                        "roles", List.of("ROLE_CUSTOMER", "ROLE_BUYER")
                )
        );

        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                jwt, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"), new SimpleGrantedAuthority("ROLE_BUYER"))
        );
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<AuthenticatedUser> result = provider.getCurrentUser();

        assertThat(result).isPresent();
        AuthenticatedUser user = result.get();
        assertThat(user.userId()).isEqualTo(userId);
        assertThat(user.email()).isEqualTo("customer@ecommerce.com");
        assertThat(user.roles()).containsExactlyInAnyOrder("ROLE_CUSTOMER", "ROLE_BUYER");
    }
}

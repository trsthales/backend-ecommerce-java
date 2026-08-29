package com.trsthales.ecommerce.common.security;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import java.util.Optional;

public interface CurrentUserProvider {

    Optional<AuthenticatedUser> getCurrentUser();

    default AuthenticatedUser getRequiredCurrentUser() {
        return getCurrentUser()
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Nenhum usuário autenticado no contexto de segurança"));
    }
}

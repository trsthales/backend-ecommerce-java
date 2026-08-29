package com.trsthales.ecommerce.identity.api;

import com.trsthales.ecommerce.common.security.AuthenticatedUser;
import com.trsthales.ecommerce.common.security.CurrentUserProvider;
import com.trsthales.ecommerce.identity.api.dto.LoginRequest;
import com.trsthales.ecommerce.identity.api.dto.RegisterUserRequest;
import com.trsthales.ecommerce.identity.api.dto.TokenResponse;
import com.trsthales.ecommerce.identity.api.dto.UserResponse;
import com.trsthales.ecommerce.identity.application.IdentityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final IdentityService identityService;
    private final CurrentUserProvider currentUserProvider;

    public AuthController(IdentityService identityService, CurrentUserProvider currentUserProvider) {
        this.identityService = identityService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        UserResponse response = identityService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/token")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = identityService.authenticate(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<AuthenticatedUser> getMe() {
        AuthenticatedUser user = currentUserProvider.getRequiredCurrentUser();
        return ResponseEntity.ok(user);
    }
}

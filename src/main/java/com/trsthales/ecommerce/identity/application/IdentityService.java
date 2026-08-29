package com.trsthales.ecommerce.identity.application;

import com.trsthales.ecommerce.common.exception.BusinessRuleViolationException;
import com.trsthales.ecommerce.identity.api.dto.LoginRequest;
import com.trsthales.ecommerce.identity.api.dto.RegisterUserRequest;
import com.trsthales.ecommerce.identity.api.dto.TokenResponse;
import com.trsthales.ecommerce.identity.api.dto.UserResponse;
import com.trsthales.ecommerce.identity.domain.Role;
import com.trsthales.ecommerce.identity.domain.User;
import com.trsthales.ecommerce.identity.infrastructure.RoleRepository;
import com.trsthales.ecommerce.identity.infrastructure.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class IdentityService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public IdentityService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        String normalizedEmail = request.email().toLowerCase().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessRuleViolationException("Email já cadastrado na plataforma: " + normalizedEmail);
        }

        Role roleCustomer = roleRepository.findById(Role.ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(new Role(Role.ROLE_CUSTOMER, "Cliente da plataforma")));

        Instant now = Instant.now();
        User user = User.builder()
                .id(UUID.randomUUID())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .enabled(true)
                .roles(Set.of(roleCustomer))
                .createdAt(now)
                .updatedAt(now)
                .build();

        User savedUser = userRepository.save(user);

        Set<String> roles = savedUser.getRoles().stream()
                .map(Role::getId)
                .collect(Collectors.toSet());

        return new UserResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getFullName(), roles);
    }

    @Transactional(readOnly = true)
    public TokenResponse authenticate(LoginRequest request) {
        String normalizedEmail = request.email().toLowerCase().trim();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Credenciais inválidas");
        }

        if (!user.isEnabled()) {
            throw new BusinessRuleViolationException("Usuário desativado na plataforma");
        }

        return tokenService.generateToken(user);
    }
}

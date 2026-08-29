package com.trsthales.ecommerce.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trsthales.ecommerce.AbstractIntegrationTest;
import com.trsthales.ecommerce.identity.api.dto.LoginRequest;
import com.trsthales.ecommerce.identity.api.dto.RegisterUserRequest;
import com.trsthales.ecommerce.identity.api.dto.TokenResponse;
import com.trsthales.ecommerce.identity.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("Identity & RSA Security Integration Tests (Fase 2)")
class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should successfully register a new user with ROLE_CUSTOMER")
    void shouldRegisterUser() throws Exception {
        RegisterUserRequest request = new RegisterUserRequest(
                "customer@ecommerce.com",
                "StrongPassword123!",
                "John Doe"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.email", is("customer@ecommerce.com")))
                .andExpect(jsonPath("$.fullName", is("John Doe")))
                .andExpect(jsonPath("$.roles", hasItem("ROLE_CUSTOMER")));
    }

    @Test
    @DisplayName("Should reject duplicate email with 422 Unprocessable Entity")
    void shouldRejectDuplicateEmail() throws Exception {
        RegisterUserRequest request = new RegisterUserRequest(
                "duplicate@ecommerce.com",
                "Password123!",
                "First User"
        );

        // First registration
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate attempt
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title", is("Business Rule Violation")))
                .andExpect(jsonPath("$.status", is(422)))
                .andExpect(jsonPath("$.detail", is("Email já cadastrado na plataforma: duplicate@ecommerce.com")));
    }

    @Test
    @DisplayName("Should reject invalid registration parameters with 400 Bad Request and violations")
    void shouldRejectInvalidRegistrationParams() throws Exception {
        RegisterUserRequest invalidRequest = new RegisterUserRequest(
                "invalid-email-format",
                "short",
                ""
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Validation Error")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.violations", notNullValue()));
    }

    @Test
    @DisplayName("Should authenticate valid user and issue signed RSA JWT")
    void shouldAuthenticateAndIssueJwt() throws Exception {
        RegisterUserRequest registerRequest = new RegisterUserRequest(
                "auth.user@ecommerce.com",
                "ValidSecretPassword123!",
                "Auth User"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest(
                "auth.user@ecommerce.com",
                "ValidSecretPassword123!"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.expiresInSeconds", is(7200)))
                .andReturn();

        TokenResponse tokenResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                TokenResponse.class
        );
        assertThat(tokenResponse.accessToken()).isNotBlank();
    }

    @Test
    @DisplayName("Should reject authentication with invalid password with 401 Unauthorized")
    void shouldRejectInvalidCredentials() throws Exception {
        RegisterUserRequest registerRequest = new RegisterUserRequest(
                "wrong.pass@ecommerce.com",
                "CorrectPassword123!",
                "Test User"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest invalidLogin = new LoginRequest(
                "wrong.pass@ecommerce.com",
                "WrongPassword999!"
        );

        mockMvc.perform(post("/api/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title", is("Authentication Required")))
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("Should access protected /api/v1/auth/me using Bearer JWT and resolve AuthenticatedUser")
    void shouldAccessProtectedEndpointWithJwt() throws Exception {
        RegisterUserRequest registerRequest = new RegisterUserRequest(
                "secured.user@ecommerce.com",
                "SecurePassword123!",
                "Secured User"
        );

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        LoginRequest loginRequest = new LoginRequest(
                "secured.user@ecommerce.com",
                "SecurePassword123!"
        );

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        TokenResponse tokenResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                TokenResponse.class
        );

        // Access protected endpoint with Bearer token
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + tokenResponse.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("secured.user@ecommerce.com")))
                .andExpect(jsonPath("$.roles", containsInAnyOrder("ROLE_CUSTOMER")));
    }

    @Test
    @DisplayName("Should reject access to protected endpoint without token with 401 Unauthorized")
    void shouldRejectProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}

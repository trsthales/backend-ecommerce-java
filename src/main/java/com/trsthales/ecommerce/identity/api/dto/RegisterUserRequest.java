package com.trsthales.ecommerce.identity.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Formato de email inválido")
        String email,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, message = "A senha deve conter no mínimo 8 caracteres")
        String password,

        @NotBlank(message = "Nome completo é obrigatório")
        String fullName
) {
}

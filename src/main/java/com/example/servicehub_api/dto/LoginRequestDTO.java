package com.example.servicehub_api.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @com.fasterxml.jackson.annotation.JsonAlias("email") @NotBlank(message = "O e-mail é obrigatório.") String username,
        @NotBlank(message = "A senha é obrigatória.") String password) {
}

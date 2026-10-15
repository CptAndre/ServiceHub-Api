package com.example.servicehub_api.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @NotBlank(message = "O usuário é obrigatório.") String username,
        @NotBlank(message = "A senha é obrigatória.") String password) {
}

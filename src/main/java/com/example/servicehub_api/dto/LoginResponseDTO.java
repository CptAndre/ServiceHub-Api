package com.example.servicehub_api.dto;

import java.util.List;

public record LoginResponseDTO(
        String accessToken,
        String tokenType,
        long expiresIn,
        List<String> roles) {
}

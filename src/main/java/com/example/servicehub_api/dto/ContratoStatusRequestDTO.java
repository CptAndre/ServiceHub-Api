package com.example.servicehub_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ContratoStatusRequestDTO {
    @NotBlank
    @Pattern(regexp = "AGENDADO|EM_ANDAMENTO|CONCLUIDO|CANCELADO")
    private String status;
}

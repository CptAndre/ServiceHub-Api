package com.example.servicehub_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ContratoRequestDTO {
    @NotBlank @Size(max = 2000)
    private String descricaoServico;
    @NotNull @Positive
    private BigDecimal valorTotal;
    @NotNull @Positive
    private Long clienteId;
    @NotNull @Positive
    private Long prestadorId;
}

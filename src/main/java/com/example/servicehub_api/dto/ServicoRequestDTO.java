package com.example.servicehub_api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ServicoRequestDTO {
    @NotBlank
    @Size(max = 150)
    private String titulo;

    @Size(max = 2000)
    private String descricao;

    @NotNull
    @Positive
    private BigDecimal preco;

    @NotNull
    @Positive
    private Long prestadorId;
}

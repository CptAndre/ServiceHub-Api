package com.example.servicehub_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PagamentoRequestDTO {
    @NotNull @Positive
    private BigDecimal valorPago;
    @NotBlank @Pattern(regexp = "PIX|CARTAO|DINHEIRO")
    private String metodoPagamento;
    @NotNull @Positive
    private Long contratoId;
}

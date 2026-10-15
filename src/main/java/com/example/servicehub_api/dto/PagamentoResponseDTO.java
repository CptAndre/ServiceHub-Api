package com.example.servicehub_api.dto;

import com.example.servicehub_api.model.Pagamento;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class PagamentoResponseDTO {
    private Long id;
    private BigDecimal valorPago;
    private String metodoPagamento;
    private LocalDateTime data;
    private Long contratoId;

    public static PagamentoResponseDTO from(Pagamento pagamento) {
        return new PagamentoResponseDTO(pagamento.getId(), pagamento.getValorPago(), pagamento.getMetodoPagamento(),
                pagamento.getData(), pagamento.getContrato().getId());
    }
}

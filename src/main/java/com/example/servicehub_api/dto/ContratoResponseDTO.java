package com.example.servicehub_api.dto;

import com.example.servicehub_api.model.Contrato;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ContratoResponseDTO {
    private Long id;
    private String descricaoServico;
    private BigDecimal valorTotal;
    private String status;
    private Long clienteId;
    private Long prestadorId;

    public static ContratoResponseDTO from(Contrato contrato) {
        return new ContratoResponseDTO(contrato.getId(), contrato.getDescricaoServico(), contrato.getValorTotal(),
                contrato.getStatus(), contrato.getCliente().getId(), contrato.getPrestador().getId());
    }
}

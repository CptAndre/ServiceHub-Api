package com.example.servicehub_api.dto;

import com.example.servicehub_api.model.Servico;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ServicoResponseDTO {
    private Long id;
    private String titulo;
    private String descricao;
    private BigDecimal preco;
    private Long prestadorId;

    public static ServicoResponseDTO from(Servico servico) {
        return new ServicoResponseDTO(servico.getId(), servico.getTitulo(), servico.getDescricao(),
                servico.getPreco(), servico.getPrestador().getId());
    }
}

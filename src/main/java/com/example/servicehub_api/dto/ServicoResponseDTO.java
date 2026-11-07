package com.example.servicehub_api.dto;
import com.example.servicehub_api.model.Servico;
import com.example.servicehub_api.model.ServiceCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;
@Data @AllArgsConstructor
public class ServicoResponseDTO {
    private Long id;
    private String titulo;
    private String descricao;
    private BigDecimal preco;
    private Long prestadorId;
    private ServiceCategory categoria;
    private boolean ativo;
    public static ServicoResponseDTO from(Servico s) {
        return new ServicoResponseDTO(s.getId(), s.getTitulo(), s.getDescricao(), s.getPreco(), s.getPrestador().getId(), s.getCategoria(), s.isAtivo());
    }
}

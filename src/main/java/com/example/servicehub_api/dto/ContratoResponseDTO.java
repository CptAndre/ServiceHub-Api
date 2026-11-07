package com.example.servicehub_api.dto;
import com.example.servicehub_api.model.Contrato;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @AllArgsConstructor
public class ContratoResponseDTO {
    private Long id;
    private String descricaoServico;
    private BigDecimal valorTotal;
    private String status;
    private Long clienteId;
    private Long prestadorId;
    private Long servicoId;
    private LocalDateTime scheduledAt;
    public static ContratoResponseDTO from(Contrato c) {
        return new ContratoResponseDTO(c.getId(), c.getDescricaoServico(), c.getValorTotal(), c.getStatus().name(),
                c.getCliente().getId(), c.getPrestador().getId(), c.getServico().getId(), c.getScheduledAt());
    }
}

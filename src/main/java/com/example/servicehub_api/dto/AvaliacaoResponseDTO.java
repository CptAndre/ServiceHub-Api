package com.example.servicehub_api.dto;

import com.example.servicehub_api.model.Avaliacao;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AvaliacaoResponseDTO {
    private Long id;
    private Integer nota;
    private String comentario;
    private Long contratoId;

    public static AvaliacaoResponseDTO from(Avaliacao avaliacao) {
        return new AvaliacaoResponseDTO(avaliacao.getId(), avaliacao.getNota(), avaliacao.getComentario(),
                avaliacao.getContrato().getId());
    }
}

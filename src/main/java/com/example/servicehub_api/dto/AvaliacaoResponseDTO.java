package com.example.servicehub_api.dto;
import com.example.servicehub_api.model.Avaliacao;
import lombok.AllArgsConstructor;
import lombok.Data;
@Data @AllArgsConstructor
public class AvaliacaoResponseDTO {
    private Long id;
    private Integer nota;
    private String comentario;
    private Long contratoId;
    private Long reviewerId;
    public static AvaliacaoResponseDTO from(Avaliacao a) {
        return new AvaliacaoResponseDTO(a.getId(), a.getNota().intValue(), a.getComentario(), a.getContrato().getId(), a.getReviewer().getId());
    }
}

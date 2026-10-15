package com.example.servicehub_api.dto;

import com.example.servicehub_api.model.Prestador;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PrestadorResponseDTO {
    private Long id;
    private String nome;
    private String cpf;
    private String especialidade;
    private String telefone;

    public static PrestadorResponseDTO from(Prestador prestador) {
        return new PrestadorResponseDTO(prestador.getId(), prestador.getNome(), prestador.getCpf(),
                prestador.getEspecialidade(), prestador.getTelefone());
    }
}

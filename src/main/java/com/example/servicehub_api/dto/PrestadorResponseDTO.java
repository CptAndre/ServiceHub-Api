package com.example.servicehub_api.dto;
import com.example.servicehub_api.model.Usuario;
import lombok.AllArgsConstructor;
import lombok.Data;
@Data @AllArgsConstructor
public class PrestadorResponseDTO {
    private Long id;
    private String nome;
    private String email;
    private String especialidade;
    private String telefone;
    private boolean ativo;
    public static PrestadorResponseDTO from(Usuario user) {
        return new PrestadorResponseDTO(user.getId(), user.getNome(), user.getEmail(), user.getBio(), user.getTelefone(), user.isAtivo());
    }
}

package com.example.servicehub_api.dto;
import com.example.servicehub_api.model.Usuario;
import lombok.AllArgsConstructor;
import lombok.Data;
@Data @AllArgsConstructor
public class ClienteResponseDTO {
    private Long id;
    private String nome;
    private String email;
    private String telefone;
    private boolean ativo;
    public static ClienteResponseDTO from(Usuario user) {
        return new ClienteResponseDTO(user.getId(), user.getNome(), user.getEmail(), user.getTelefone(), user.isAtivo());
    }
}

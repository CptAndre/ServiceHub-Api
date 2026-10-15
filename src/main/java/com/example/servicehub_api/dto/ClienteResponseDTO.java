package com.example.servicehub_api.dto;

import com.example.servicehub_api.model.Cliente;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClienteResponseDTO {
    private Long id;
    private String nome;
    private String documento;
    private String email;
    private String telefone;

    public static ClienteResponseDTO from(Cliente cliente) {
        return new ClienteResponseDTO(cliente.getId(), cliente.getNome(), cliente.getDocumento(),
                cliente.getEmail(), cliente.getTelefone());
    }
}

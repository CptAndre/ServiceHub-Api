package com.example.servicehub_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClienteRequestDTO {

    @NotBlank(message = "O nome é obrigatório e não pode ser vazio.")
    @Size(min = 2, max = 100, message = "O nome deve ter entre 2 e 100 caracteres.")
    private String nome;

    @NotBlank(message = "O email é obrigatório.")
    @Email(message = "Forneça um formato de e-mail válido.")
    private String email;

    @NotBlank(message = "O telefone é obrigatório.")
    @Pattern(regexp = "^\\d{10,11}$", message = "Telefone deve conter apenas números (10 ou 11 dígitos).")
    private String telefone;

    @Size(max = 20, message = "O documento deve ter no máximo 20 caracteres.")
    @Pattern(regexp = "^[0-9.\\-\\s]*$", message = "O documento contém caracteres inválidos.")
    private String documento;
}

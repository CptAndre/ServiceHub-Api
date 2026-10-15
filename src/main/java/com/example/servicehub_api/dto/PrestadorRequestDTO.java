package com.example.servicehub_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import com.example.servicehub_api.validation.CPF;

@Data
public class PrestadorRequestDTO {
    @NotBlank @Size(min = 2, max = 150)
    private String nome;
    @NotBlank @Size(max = 14) @CPF
    private String cpf;
    @Size(max = 100)
    private String especialidade;
    @NotBlank @Pattern(regexp = "^\\d{10,11}$")
    private String telefone;
}

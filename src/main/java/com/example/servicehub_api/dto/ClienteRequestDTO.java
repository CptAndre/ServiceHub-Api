package com.example.servicehub_api.dto;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class ClienteRequestDTO {
    @JsonAlias("fullName") @NotBlank @Size(min = 2, max = 100)
    private String nome;
    @NotBlank @Email @Size(max = 255)
    private String email;
    @JsonAlias("phone") @Pattern(regexp = "^\\d{10,11}$")
    private String telefone;
    @Size(min = 12, max = 72)
    private String password;
    private String bio;
    @JsonAlias("active") private Boolean ativo;
    @Size(max = 500)
    private String avatarUrl;
}

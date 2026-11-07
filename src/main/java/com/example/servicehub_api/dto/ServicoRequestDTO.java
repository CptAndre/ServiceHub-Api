package com.example.servicehub_api.dto;
import com.example.servicehub_api.model.ServiceCategory;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
@Data
public class ServicoRequestDTO {
    @JsonAlias("title") @NotBlank @Size(max = 150) private String titulo;
    @JsonAlias("description") private String descricao;
    @JsonAlias("price") @NotNull @Positive @Digits(integer = 8, fraction = 2) private BigDecimal preco;
    @JsonAlias("category") @NotNull private ServiceCategory categoria;
    @JsonAlias("providerId") @NotNull @Positive private Long prestadorId;
}

package com.example.servicehub_api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AvaliacaoRequestDTO {
    @NotNull @Min(1) @Max(5)
    private Integer nota;
    @Size(max = 2000)
    private String comentario;
    @NotNull @Positive
    private Long contratoId;
}

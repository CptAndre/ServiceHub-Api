package com.example.servicehub_api.dto;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class ContratoRequestDTO {
    @JsonAlias("clientId") @NotNull @Positive private Long clienteId;
    @JsonAlias("serviceId") @NotNull @Positive private Long servicoId;
    @Future private LocalDateTime scheduledAt;
    @JsonAlias("notes") @Size(max = 2000) private String descricaoServico;
}

package com.example.servicehub_api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class BookServiceRestRequestDTO {
    @NotNull
    @Positive
    private Long clientId;

    @NotNull
    @Positive
    private Long serviceId;
}

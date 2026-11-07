package com.example.servicehub_api.dto;
import com.example.servicehub_api.model.RequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data
public class ContratoStatusRequestDTO {
    @NotNull private RequestStatus status;
}

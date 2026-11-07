package com.example.servicehub_api.dto;
import com.example.servicehub_api.model.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper = true)
public class RegisterRequestDTO extends ClienteRequestDTO {
    @NotNull private UserRole role = UserRole.CLIENT;
}

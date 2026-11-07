package com.example.servicehub_api.dto;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper = true)
public class PrestadorRequestDTO extends ClienteRequestDTO {
    private String especialidade;
}

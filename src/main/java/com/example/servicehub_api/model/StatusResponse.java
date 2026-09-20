package com.example.servicehub_api.model;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StatusResponse {
    private String status;
    private String versao;
    private String mensagem;
}

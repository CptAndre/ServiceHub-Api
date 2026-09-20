package com.example.servicehub_api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "pagamentos")
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "valor_pago")
    private BigDecimal valorPago;

    @Column(name = "metodo_pagamento")
    private String metodoPagamento; // Ex: PIX, CARTAO, DINHEIRO

    private LocalDateTime data = LocalDateTime.now();

    // Relacionamento com Contrato
    @ManyToOne
    @JoinColumn(name = "contrato_id")
    private Contrato contrato;
}
package com.example.servicehub_api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "contratos")
public class Contrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String descricaoMoveis;
    private BigDecimal valorTotal;
    private String status = "AGENDADO"; // Valor padrão

    // Relacionamento com Cliente
    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    // Relacionamento com Montador
    @ManyToOne
    @JoinColumn(name = "montador_id")
    private Montador montador;
}
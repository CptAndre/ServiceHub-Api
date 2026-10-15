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

    @Column(name = "descricao_servico")
    private String descricaoServico;

    @Column(name = "valor_total")
    private BigDecimal valorTotal;

    private String status = "AGENDADO"; // Valor padrão

    // Relacionamento com Cliente
    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    // Relacionamento com prestador
    @ManyToOne
    @JoinColumn(name = "prestador_id")
    private Prestador prestador;
}
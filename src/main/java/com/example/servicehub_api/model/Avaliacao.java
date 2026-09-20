package com.example.servicehub_api.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "avaliacoes")
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer nota; // Esperado de 1 a 5
    private String comentario;

    // Relacionamento com Contrato
    @ManyToOne
    @JoinColumn(name = "contrato_id")
    private Contrato contrato;
}
package com.example.servicehub_api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @Entity @Table(name = "services")
public class Servico {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "title", nullable = false, length = 150)
    private String titulo;
    @Column(name = "description", columnDefinition = "TEXT")
    private String descricao;
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;
    @Enumerated(EnumType.STRING) @Column(name = "category", nullable = false, length = 50)
    private ServiceCategory categoria;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private Usuario prestador;
    @Column(name = "is_active", nullable = false)
    private boolean ativo = true;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
}

package com.example.servicehub_api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @Entity @Table(name = "service_requests")
public class Contrato {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private Servico servico;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Usuario cliente;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private RequestStatus status = RequestStatus.PENDING;
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;
    @Column(name = "notes", columnDefinition = "TEXT")
    private String descricaoServico;
    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    public Usuario getPrestador() { return servico.getPrestador(); }
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
}

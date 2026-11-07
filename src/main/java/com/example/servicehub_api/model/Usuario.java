package com.example.servicehub_api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.Locale;

@Getter @Setter @Entity @Table(name = "users")
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "full_name", nullable = false, length = 100)
    private String nome;
    @Column(nullable = false, unique = true, length = 255)
    private String email;
    @JsonIgnore @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(name = "role", nullable = false, length = 20)
    private UserRole papel = UserRole.CLIENT;
    @Column(name = "phone", length = 20)
    private String telefone;
    @Column(columnDefinition = "TEXT")
    private String bio;
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;
    @Column(name = "is_active", nullable = false)
    private boolean ativo = true;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        onUpdate();
    }
    @PreUpdate
    void onUpdate() {
        email = email.trim().toLowerCase(Locale.ROOT);
        updatedAt = LocalDateTime.now();
    }
}

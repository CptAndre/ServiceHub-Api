package com.example.servicehub_api.repository;

import com.example.servicehub_api.model.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {
    Page<Avaliacao> findByContratoPrestadorId(Long prestadorId, Pageable pageable);
}
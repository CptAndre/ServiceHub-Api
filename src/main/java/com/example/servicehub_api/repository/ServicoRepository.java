package com.example.servicehub_api.repository;

import com.example.servicehub_api.model.Servico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicoRepository extends JpaRepository<Servico, Long> {
    Page<Servico> findByPrestadorId(Long prestadorId, Pageable pageable);
}

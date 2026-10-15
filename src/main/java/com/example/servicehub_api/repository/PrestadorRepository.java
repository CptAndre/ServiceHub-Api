package com.example.servicehub_api.repository;

import com.example.servicehub_api.model.Prestador;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrestadorRepository extends JpaRepository<Prestador, Long> {
    Page<Prestador> findByEspecialidadeContainingIgnoreCase(String especialidade, Pageable pageable);
}
